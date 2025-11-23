package fr.uga.im2ag.m1info.chatservice.client.encryption;

import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.crypto.SessionKeyManager;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.KeyExchangeException;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.KeyExchangeListener;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.KeyExchangeManager;
import fr.uga.im2ag.m1info.chatservice.crypto.strategy.AESEncryptionStrategy;
import fr.uga.im2ag.m1info.chatservice.crypto.strategy.EncryptionStrategy;
import fr.uga.im2ag.m1info.chatservice.crypto.strategy.NoOpEncryptionStrategy;
import fr.uga.im2ag.m1info.chatservice.storage.KeyStore;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-level facade for client-side encryption operations.
 * <p>
 * This service encapsulates:
 * <ul>
 *   <li>{@link SessionKeyManager} - Session key storage and sequence management</li>
 *   <li>{@link KeyExchangeManager} - ECDH key exchange protocol</li>
 *   <li>{@link EncryptionStrategy} - Message encryption/decryption</li>
 * </ul>
 * <p>
 * The facade provides simple, high-level methods for:
 * <ul>
 *   <li>Initializing secure conversations with peers</li>
 *   <li>Encrypting outgoing messages</li>
 *   <li>Checking encryption readiness</li>
 *   <li>Managing encryption lifecycle</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe. All underlying components use
 * concurrent data structures.
 * <p>
 * Initialization Flow:
 * <pre>
 * 1. Create service with initial client ID (can be 0 for new users)
 * 2. Call setMessageSender() to configure outgoing message transport
 * 3. Call start() to begin accepting key exchanges
 * 4. After connection established, call updateClientId() if needed
 * 5. Use initiateSecureConversation() before sending to new peers
 * 6. Use prepareForSending() to encrypt messages
 * 7. Call shutdown() on disconnect
 * </pre>
 * <p>
 * Usage:
 * <pre>{@code
 * // Initialize
 * ClientEncryptionService encryption = new ClientEncryptionService(clientId);
 * encryption.setMessageSender(packet -> client.sendPacket(packet));
 * encryption.start();
 * 
 * // Before first message to a peer
 * encryption.initiateSecureConversation(peerId)
 *     .thenAccept(success -> {
 *         if (success) {
 *             // Now we can send encrypted messages
 *         }
 *     });
 * 
 * // Send encrypted message
 * ProtocolMessage encrypted = encryption.prepareForSending(message);
 * client.sendPacket(encrypted.toPacket());
 * 
 * // Cleanup
 * encryption.shutdown();
 * }</pre>
 *
 * @see SessionKeyManager
 * @see KeyExchangeManager
 * @see EncryptionStrategy
 */
public class ClientEncryptionService {

    private static final Logger LOG = Logger.getLogger(ClientEncryptionService.class.getName());

    // ========================= Configuration =========================

    /** Timeout for key exchange operations in seconds */
    private static final long KEY_EXCHANGE_TIMEOUT_SECONDS = 30;

    // ========================= Core Components =========================

    private volatile int localClientId;
    private final KeyStore keyStore;
    private final SessionKeyManager sessionManager;
    private KeyExchangeManager keyExchangeManager;
    private EncryptionStrategy encryptionStrategy;

    // ========================= State =========================

    /** Pending key exchange futures, keyed by peer ID */
    private final ConcurrentHashMap<Integer, CompletableFuture<Boolean>> pendingExchanges;

    /** Whether the service has been started */
    private volatile boolean started;

    /** Whether encryption is enabled */
    private volatile boolean encryptionEnabled;

    // ========================= Constructors =========================

    /**
     * Creates a new ClientEncryptionService with in-memory key storage.
     *
     * @param localClientId the local client's ID (can be 0 for new users)
     */
    public ClientEncryptionService(int localClientId) {
        this(localClientId, null);
    }

    /**
     * Creates a new ClientEncryptionService with optional persistent key storage.
     *
     * @param localClientId the local client's ID (can be 0 for new users)
     * @param keyStore      the persistent key store (can be null for in-memory only)
     */
    public ClientEncryptionService(int localClientId, KeyStore keyStore) {
        this.localClientId = localClientId;
        this.keyStore = keyStore;
        this.sessionManager = new SessionKeyManager();
        this.pendingExchanges = new ConcurrentHashMap<>();
        this.started = false;
        this.encryptionEnabled = true;

        initializeComponents();
    }

    private void initializeComponents() {
        if (localClientId == 0) {
            return; // Cannot initialize components with ID 0
        }
        this.keyExchangeManager = new KeyExchangeManager(localClientId, sessionManager, keyStore);
        this.encryptionStrategy = new AESEncryptionStrategy(localClientId, sessionManager, keyExchangeManager);
        setupKeyExchangeListener();
    }

    // ========================= Lifecycle =========================

    /**
     * Starts the encryption service.
     * <p>
     * Must be called after setting the message sender.
     */
    public void start() {
        if (started) {
            LOG.warning("ClientEncryptionService already started");
            return;
        }

        if (localClientId == 0) {
            LOG.warning("Cannot start ClientEncryptionService with client ID 0");
            return;
        }

        keyExchangeManager.start();
        started = true;
        LOG.info("ClientEncryptionService started for client " + localClientId);
    }

    /**
     * Shuts down the encryption service.
     * <p>
     * Cancels pending key exchanges and releases resources.
     */
    public void shutdown() {
        if (!started) {
            return;
        }

        started = false;

        // Cancel all pending exchanges
        pendingExchanges.forEach((peerId, future) -> {
            future.completeExceptionally(new IllegalStateException("Service shutdown"));
        });
        pendingExchanges.clear();

        if (keyExchangeManager != null) {
            keyExchangeManager.shutdown();
        }

        if (encryptionStrategy != null) {
            encryptionStrategy.clearAllContexts();
        }

        LOG.info("ClientEncryptionService shutdown");
    }

    /**
     * Checks if the service is running.
     *
     * @return true if started and not shutdown
     */
    public boolean isRunning() {
        return started && (keyExchangeManager == null || keyExchangeManager.isRunning());
    }

    // ========================= Client ID Management =========================

    /**
     * Updates the local client ID after server assignment.
     * <p>
     * This must be called when a new user is created and the server assigns an ID.
     * Re-initializes internal components with the new ID.
     *
     * @param newClientId the assigned client ID (must be positive)
     * @throws IllegalArgumentException if newClientId is not positive
     */
    public void updateClientId(int newClientId) {
        if (newClientId <= 0) {
            throw new IllegalArgumentException("Client ID must be positive: " + newClientId);
        }

        if (newClientId == this.localClientId) {
            return; // No change needed
        }

        LOG.info("Updating client ID from " + localClientId + " to " + newClientId);

        int oldId = this.localClientId;
        this.localClientId = newClientId;

        // Re-initialize components with new ID
        if (started) { shutdown(); }
        initializeComponents();
        start();
    }

    /**
     * Gets the current local client ID.
     *
     * @return the local client ID
     */
    public int getLocalClientId() {
        return localClientId;
    }

    // ========================= Configuration =========================

    /**
     * Sets the message sender for key exchange messages.
     * <p>
     * Must be called before {@link #start()}.
     *
     * @param sender the consumer that sends key exchange message data
     */
    public void setMessageSender(Consumer<KeyExchangeManager.KeyExchangeMessageData> sender) {
        if (keyExchangeManager != null) {
            keyExchangeManager.setMessageSender(sender);
        } else {
            LOG.warning("Message sender set but KeyExchangeManager not initialized (ID=0)");
        }
    }

    /**
     * Enables or disables encryption.
     * <p>
     * When disabled, messages are sent in plaintext.
     *
     * @param enabled true to enable encryption
     */
    public void setEncryptionEnabled(boolean enabled) {
        this.encryptionEnabled = enabled;
        LOG.info("Encryption " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Checks if encryption is enabled.
     *
     * @return true if encryption is enabled
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    // ========================= High-Level Operations =========================

    /**
     * Initiates a secure conversation with a peer.
     * <p>
     * Performs ECDH key exchange if no session exists.
     * Returns a future that completes when the session is established.
     *
     * @param peerId the peer ID
     * @return a future that completes with true if successful, false otherwise
     */
    public CompletableFuture<Boolean> initiateSecureConversation(int peerId) {
        if (!encryptionEnabled) {
            return CompletableFuture.completedFuture(true);
        }

        if (hasSecureSession(peerId)) {
            return CompletableFuture.completedFuture(true);
        }

        if (keyExchangeManager == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Encryption not initialized (client ID = 0)")
            );
        }

        // Check for existing pending exchange
        CompletableFuture<Boolean> existing = pendingExchanges.get(peerId);
        if (existing != null && !existing.isDone()) {
            return existing;
        }

        // Create new pending exchange
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingExchanges.put(peerId, future);

        try {
            keyExchangeManager.initiateKeyExchange(peerId);

            // Set timeout
            CompletableFuture.delayedExecutor(KEY_EXCHANGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .execute(() -> {
                        if (!future.isDone()) {
                            future.complete(false);
                            pendingExchanges.remove(peerId);
                            LOG.warning("Key exchange with peer " + peerId + " timed out");
                        }
                    });

        } catch (KeyExchangeException e) {
            future.completeExceptionally(e);
            pendingExchanges.remove(peerId);
            LOG.log(Level.SEVERE, "Failed to initiate key exchange with peer " + peerId, e);
        }

        return future;
    }

    /**
     * Prepares a message for sending by encrypting if necessary.
     * <p>
     * If encryption is enabled and a session exists with the recipient,
     * the message is encrypted. Otherwise, it's returned unchanged.
     *
     * @param message the message to prepare
     * @return the message ready for sending (possibly encrypted)
     * @throws GeneralSecurityException if encryption fails
     */
    public ProtocolMessage prepareForSending(ProtocolMessage message) throws GeneralSecurityException {
        if (!shouldEncrypt(message)) {
            return message;
        }

        if (encryptionStrategy == null) {
            LOG.warning("Encryption strategy not initialized, sending unencrypted");
            return message;
        }

        return encryptionStrategy.encrypt(message);
    }

    /**
     * Checks if a message should be encrypted.
     *
     * @param message the message to check
     * @return true if the message should be encrypted
     */
    public boolean shouldEncrypt(ProtocolMessage message) {
        System.out.println("Checking if should encrypt message to " + message.getTo() +
                " of type " + message.getMessageType());
        if (!encryptionEnabled) {
            System.out.println("Encryption is disabled");
            return false;
        }

        if (encryptionStrategy == null) {
            System.out.println("Encryption strategy not initialized");
            return false;
        }

        System.out.println("Delegating to encryption strategy");
        return encryptionStrategy.shouldEncrypt(message.getMessageType(), message.getTo());
    }

    /**
     * Checks if a secure session exists with a peer.
     *
     * @param peerId the peer ID
     * @return true if encrypted communication is possible
     */
    public boolean hasSecureSession(int peerId) {
        if (!encryptionEnabled || encryptionStrategy == null) {
            return false;
        }
        return encryptionStrategy.hasSessionKey(peerId);
    }

    /**
     * Checks if a key exchange is in progress with a peer.
     *
     * @param peerId the peer ID
     * @return true if key exchange is pending
     */
    public boolean isKeyExchangePending(int peerId) {
        CompletableFuture<Boolean> pending = pendingExchanges.get(peerId);
        return pending != null && !pending.isDone();
    }

    /**
     * Gets all peer IDs with active secure sessions.
     *
     * @return set of peer IDs with established sessions
     */
    public Set<String> getActiveConversationIds() {
        return sessionManager.getActiveConversations();
    }

    // ========================= Key Exchange Message Handling =========================

    /**
     * Handles an incoming KEY_EXCHANGE message.
     * <p>
     * Should be called by the message handler when a KEY_EXCHANGE is received.
     *
     * @param peerId         the peer ID
     * @param publicKeyBytes the peer's public key
     */
    public void handleKeyExchangeRequest(int peerId, byte[] publicKeyBytes) {
        if (keyExchangeManager == null) {
            LOG.warning("Cannot handle key exchange: manager not initialized");
            return;
        }

        try {
            keyExchangeManager.handleKeyExchangeRequest(peerId, publicKeyBytes);
        } catch (KeyExchangeException e) {
            LOG.log(Level.SEVERE, "Failed to handle key exchange from peer " + peerId, e);
        }
    }

    /**
     * Handles an incoming KEY_EXCHANGE_RESPONSE message.
     * <p>
     * Should be called by the message handler when a KEY_EXCHANGE_RESPONSE is received.
     *
     * @param peerId         the peer ID
     * @param publicKeyBytes the peer's public key
     */
    public void handleKeyExchangeResponse(int peerId, byte[] publicKeyBytes) {
        if (keyExchangeManager == null) {
            LOG.warning("Cannot handle key exchange response: manager not initialized");
            return;
        }

        try {
            keyExchangeManager.handleKeyExchangeResponse(peerId, publicKeyBytes);
        } catch (KeyExchangeException e) {
            LOG.log(Level.SEVERE, "Failed to handle key exchange response from peer " + peerId, e);
        }
    }

    // ========================= Accessors for Advanced Use =========================

    /**
     * Gets the encryption strategy.
     * <p>
     * For advanced use cases like custom decryption handling.
     *
     * @return the encryption strategy, or a NoOpEncryptionStrategy if not initialized
     */
    public EncryptionStrategy getEncryptionStrategy() {
        if (encryptionStrategy != null) {
            return encryptionStrategy;
        }
        return new NoOpEncryptionStrategy(false);
    }

    /**
     * Gets the key exchange manager.
     * <p>
     * For advanced use cases like adding custom listeners.
     *
     * @return the key exchange manager (can be null if ID = 0)
     */
    public KeyExchangeManager getKeyExchangeManager() {
        return keyExchangeManager;
    }

    /**
     * Gets the session key manager.
     * <p>
     * For advanced use cases like session statistics.
     *
     * @return the session key manager
     */
    public SessionKeyManager getSessionManager() {
        return sessionManager;
    }

    // ========================= Private Methods =========================

    /**
     * Sets up the key exchange listener to complete pending futures.
     */
    private void setupKeyExchangeListener() {
        if (keyExchangeManager == null) {
            return;
        }

        keyExchangeManager.addListener(new KeyExchangeListener() {
            @Override
            public void onKeyExchangeCompleted(int peerId, SecretKey sessionKey) {
                LOG.info("Key exchange completed with peer " + peerId);

                CompletableFuture<Boolean> future = pendingExchanges.remove(peerId);
                if (future != null) {
                    future.complete(true);
                }

                // Invalidate context cache to use new key
                if (encryptionStrategy != null) {
                    encryptionStrategy.invalidateContext(peerId);
                }
            }

            @Override
            public void onKeyExchangeFailed(int peerId, KeyExchangeException error) {
                LOG.log(Level.WARNING, "Key exchange failed with peer " + peerId, error);

                CompletableFuture<Boolean> future = pendingExchanges.remove(peerId);
                if (future != null) {
                    future.complete(false);
                }
            }

            @Override
            public void onKeyExchangeExpired(int peerId) {
                LOG.warning("Key exchange expired with peer " + peerId);

                CompletableFuture<Boolean> future = pendingExchanges.remove(peerId);
                if (future != null) {
                    future.complete(false);
                }
            }

            @Override
            public void onSessionInvalidated(int peerId, String reason) {
                LOG.info("Session invalidated with peer " + peerId + ": " + reason);

                // Invalidate context cache
                if (encryptionStrategy != null) {
                    encryptionStrategy.invalidateContext(peerId);
                }
            }
        });
    }

    @Override
    public String toString() {
        return "ClientEncryptionService{" +
                "clientId=" + localClientId +
                ", started=" + started +
                ", encryptionEnabled=" + encryptionEnabled +
                ", activeSessions=" + (sessionManager != null ? sessionManager.getActiveConversations().size() : 0) +
                '}';
    }
}
