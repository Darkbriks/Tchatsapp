package fr.uga.im2ag.m1info.chatservice.client.encryption;

import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.repository.GroupRepository;
import fr.uga.im2ag.m1info.chatservice.crypto.SessionKeyManager;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.*;
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
 * This service handles both private and group conversations through
 * a unified interface, managing:
 * <ul>
 *   <li>Private peer-to-peer ECDH key exchanges</li>
 *   <li>Group symmetric key distribution and rotation</li>
 *   <li>Automatic key rotation on member changes</li>
 *   <li>Message encryption/decryption for all conversation types</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe.
 *
 * @see CompositeKeyExchangeManager
 * @see PrivateKeyExchangeManager
 * @see GroupKeyExchangeManager
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
    private final GroupRepository groupRepository;
    private IKeyExchangeManager keyExchangeManager;
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
     * Creates a new ClientEncryptionService with group support.
     *
     * @param localClientId   the local client's ID (can be 0 for new users)
     * @param groupRepository the group repository for group management
     */
    public ClientEncryptionService(int localClientId, GroupRepository groupRepository) {
        this(localClientId, groupRepository, null);
    }

    /**
     * Creates a new ClientEncryptionService with group support and persistent storage.
     *
     * @param localClientId   the local client's ID (can be 0 for new users)
     * @param groupRepository the group repository
     * @param keyStore        the persistent key store (can be null)
     */
    public ClientEncryptionService(int localClientId, GroupRepository groupRepository, KeyStore keyStore) {
        this.localClientId = localClientId;
        this.groupRepository = groupRepository;
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

        PrivateKeyExchangeManager privateManager = new PrivateKeyExchangeManager(localClientId, sessionManager, keyStore);
        GroupKeyExchangeManager groupManager = new GroupKeyExchangeManager(localClientId, sessionManager, groupRepository, keyStore);
        this.keyExchangeManager = new CompositeKeyExchangeManager(privateManager, groupManager, groupRepository);
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
    public void setMessageSender(Consumer<KeyExchangeMessageData> sender) {
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

    // ========================= Key Exchange Operations =========================

    /**
     * Initiates a secure conversation with a target (peer or group).
     *
     * @param targetId the target ID (peer ID for private, group ID for groups)
     * @return a future that completes when the exchange is done
     */
    public CompletableFuture<Boolean> initiateSecureConversation(int targetId) {
        if (!encryptionEnabled) {
            return CompletableFuture.completedFuture(false);
        }

        if (keyExchangeManager == null) {
            LOG.warning("Key exchange manager not initialized");
            return CompletableFuture.completedFuture(false);
        }

        // Check if we already have a session
        if (keyExchangeManager.hasSessionWith(targetId)) {
            LOG.fine("Session already exists with " + targetId);
            return CompletableFuture.completedFuture(true);
        }

        // Check for pending exchange
        CompletableFuture<Boolean> existing = pendingExchanges.get(targetId);
        if (existing != null && !existing.isDone()) {
            LOG.fine("Reusing pending exchange with " + targetId);
            return existing;
        }

        // Create new exchange future
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingExchanges.put(targetId, future);

        // Set timeout
        future.orTimeout(KEY_EXCHANGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Initiate exchange
        try {
            keyExchangeManager.initiateKeyExchange(targetId);
        } catch (KeyExchangeException e) {
            future.completeExceptionally(e);
            pendingExchanges.remove(targetId);
            LOG.log(Level.SEVERE, "Failed to initiate key exchange with " + targetId, e);
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
        if (!encryptionEnabled) {
            return false;
        }

        if (encryptionStrategy == null) {
            return false;
        }

        return encryptionStrategy.shouldEncrypt(message.getMessageType(), message.getTo());
    }

    /**
     * Checks if a secure session exists with a peer.
     *
     * @param peerId the peer ID
     * @return true if encrypted communication is possible
     */
    public boolean hasSecureSession(int peerId) {
        if (!encryptionEnabled || keyExchangeManager == null) {
            return false;
        }
        return keyExchangeManager.hasSessionWith(peerId);
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

    // ========================= Group Management =========================

    /**
     * Handles a group member addition.
     * Triggers key rotation if we're the admin.
     *
     * @param groupId  the group ID
     * @param memberId the new member ID
     */
    public void handleGroupMemberAdded(int groupId, int memberId) {
        if (!encryptionEnabled || keyExchangeManager == null) {
            return;
        }

        LOG.info(String.format("Handling member %d added to group %d", memberId, groupId));
        keyExchangeManager.handleGroupMemberChange(groupId, memberId, true);

        // Invalidate encryption context to force refresh
        if (encryptionStrategy != null) {
            encryptionStrategy.invalidateContext(groupId);
        }
    }

    /**
     * Handles a group member removal.
     * Triggers key rotation if we're the admin.
     *
     * @param groupId  the group ID
     * @param memberId the removed member ID
     */
    public void handleGroupMemberRemoved(int groupId, int memberId) {
        if (!encryptionEnabled || keyExchangeManager == null) {
            return;
        }

        LOG.info(String.format("Handling member %d removed from group %d", memberId, groupId));
        keyExchangeManager.handleGroupMemberChange(groupId, memberId, false);

        // If we're the removed member, invalidate the session
        if (memberId == localClientId) {
            keyExchangeManager.invalidateSession(groupId, "Removed from group");
        }

        // Invalidate encryption context
        if (encryptionStrategy != null) {
            encryptionStrategy.invalidateContext(groupId);
        }
    }

    /**
     * Handles group creation.
     * If we're the admin, initiates key distribution.
     *
     * @param groupId the new group ID
     */
    public void handleGroupCreated(int groupId) {
        if (!encryptionEnabled || keyExchangeManager == null) {
            return;
        }

        LOG.info("Handling group creation: " + groupId);

        // If we're admin, initiate key distribution
        if (groupRepository != null) {
            var group = groupRepository.findById(groupId);
            if (group != null && group.getAdminId() == localClientId) {
                try {
                    keyExchangeManager.initiateKeyExchange(groupId);
                } catch (KeyExchangeException e) {
                    LOG.log(Level.WARNING, "Failed to initiate group key distribution", e);
                }
            }
        }
    }

    /**
     * Manually rotates a group key (admin only).
     *
     * @param groupId the group ID
     * @throws KeyExchangeException if rotation fails
     */
    public void rotateGroupKey(int groupId) throws KeyExchangeException {
        if (!encryptionEnabled || keyExchangeManager == null) {
            throw new KeyExchangeException("Encryption not enabled", KeyExchangeException.ErrorCode.INTERNAL_ERROR);
        }

        keyExchangeManager.rotateGroupKey(groupId);

        // Invalidate encryption context
        if (encryptionStrategy != null) {
            encryptionStrategy.invalidateContext(groupId);
        }
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

    // ========================= Accessors =========================

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
    public IKeyExchangeManager getKeyExchangeManager() {
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
            public void onKeyExchangeCompleted(int targetId, SecretKey sessionKey) {
                LOG.info("Key exchange completed with " + targetId);

                CompletableFuture<Boolean> future = pendingExchanges.remove(targetId);
                if (future != null) {
                    future.complete(true);
                }

                // Invalidate context cache to use new key
                if (encryptionStrategy != null) {
                    encryptionStrategy.invalidateContext(targetId);
                }
            }

            @Override
            public void onKeyExchangeFailed(int targetId, KeyExchangeException error) {
                LOG.log(Level.WARNING, "Key exchange failed with " + targetId, error);

                CompletableFuture<Boolean> future = pendingExchanges.remove(targetId);
                if (future != null) {
                    future.complete(false);
                }
            }

            @Override
            public void onKeyExchangeExpired(int targetId) {
                LOG.warning("Key exchange expired with " + targetId);

                CompletableFuture<Boolean> future = pendingExchanges.remove(targetId);
                if (future != null) {
                    future.complete(false);
                }
            }

            @Override
            public void onSessionInvalidated(int targetId, String reason) {
                LOG.info("Session invalidated with " + targetId + ": " + reason);

                if (encryptionStrategy != null) {
                    encryptionStrategy.invalidateContext(targetId);
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
