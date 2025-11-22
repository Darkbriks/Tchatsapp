package fr.uga.im2ag.m1info.chatservice.crypto.keyexchange;

import fr.uga.im2ag.m1info.chatservice.crypto.KeyExchange;
import fr.uga.im2ag.m1info.chatservice.crypto.SessionKeyManager;
import fr.uga.im2ag.m1info.chatservice.storage.KeyStore;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages ECDH key exchanges and session keys for secure peer-to-peer communication.
 * <p>
 * This class orchestrates the complete key exchange lifecycle:
 * <ol>
 *   <li>Initiating key exchanges with peers</li>
 *   <li>Processing incoming key exchange requests</li>
 *   <li>Deriving and storing session keys</li>
 *   <li>Managing key rotation</li>
 *   <li>Persisting keys to storage</li>
 * </ol>
 * <p>
 * Thread Safety: This class is thread-safe. All public methods can be called
 * from any thread. Internal state is protected by concurrent data structures
 * and synchronization where necessary.
 * <p>
 * Usage example:
 * <pre>{@code
 * // Initialize
 * KeyExchangeManager manager = new KeyExchangeManager(myClientId, sessionManager, keyStore);
 * manager.setMessageSender(msg -> clientController.sendPacket(msg.toPacket()));
 * manager.addListener(myListener);
 * manager.start();
 * 
 * // Initiate exchange
 * manager.initiateKeyExchange(peerId);
 * 
 * // Handle incoming (called from message handler)
 * manager.handleKeyExchangeRequest(peerId, publicKeyBytes);
 * manager.handleKeyExchangeResponse(peerId, publicKeyBytes);
 * 
 * // Check status
 * if (manager.hasSessionWith(peerId)) {
 *     // Can send encrypted messages
 * }
 * 
 * // Cleanup
 * manager.shutdown();
 * }</pre>
 *
 * @see KeyExchange
 * @see SessionKeyManager
 * @see PendingKeyExchange
 */
public class KeyExchangeManager {
    
    private static final Logger LOG = Logger.getLogger(KeyExchangeManager.class.getName());
    
    // ========================= Constants =========================
    
    /** Algorithm for public key encoding/decoding. */
    private static final String KEY_ALGORITHM = "X25519";
    
    /** Provider for cryptographic operations. */
    private static final String CRYPTO_PROVIDER = "BC";
    
    /** Interval for checking expired key exchanges. */
    private static final Duration CLEANUP_INTERVAL = Duration.ofSeconds(5);
    
    /** Maximum number of retry attempts for key exchange. */
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // ========================= Dependencies =========================
    
    private final int localClientId;
    private final KeyExchange keyExchange;
    private final SessionKeyManager sessionManager;
    private final KeyStore keyStore;
    
    // ========================= State =========================
    
    /** Pending key exchanges indexed by peer ID. */
    private final ConcurrentMap<Integer, PendingKeyExchange> pendingExchanges;
    
    /** Retry counts for failed exchanges. */
    private final ConcurrentMap<Integer, Integer> retryCounters;
    
    /** Event listeners. */
    private final List<KeyExchangeListener> listeners;
    
    /** Lock for listener modifications. */
    private final Object listenerLock = new Object();
    
    /** Callback for sending messages. */
    private volatile Consumer<KeyExchangeMessageData> messageSender;
    
    /** Scheduler for timeout checks. */
    private final ScheduledExecutorService scheduler;
    
    /** Flag indicating if manager is running. */
    private final AtomicBoolean running;
    
    /** Scheduled cleanup task handle. */
    private ScheduledFuture<?> cleanupTask;
    
    // ========================= Constructor =========================
    
    /**
     * Creates a new KeyExchangeManager.
     *
     * @param localClientId  the ID of the local client
     * @param sessionManager the session key manager for storing derived keys
     * @param keyStore       the key store for persistent storage (may be null for in-memory only)
     * @throws IllegalArgumentException if localClientId is not positive
     * @throws NullPointerException     if sessionManager is null
     */
    public KeyExchangeManager(int localClientId, SessionKeyManager sessionManager, KeyStore keyStore) {
        if (localClientId <= 0) {
            throw new IllegalArgumentException("Local client ID must be positive: " + localClientId);
        }
        Objects.requireNonNull(sessionManager, "Session manager cannot be null");
        
        this.localClientId = localClientId;
        this.keyExchange = new KeyExchange();
        this.sessionManager = sessionManager;
        this.keyStore = keyStore;
        
        this.pendingExchanges = new ConcurrentHashMap<>();
        this.retryCounters = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "KeyExchange-Cleanup");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
        
        // Load persisted session keys
        loadPersistedSessions();
    }
    
    /**
     * Creates a KeyExchangeManager without persistent storage.
     *
     * @param localClientId  the ID of the local client
     * @param sessionManager the session key manager
     */
    public KeyExchangeManager(int localClientId, SessionKeyManager sessionManager) {
        this(localClientId, sessionManager, null);
    }
    
    // ========================= Lifecycle =========================
    
    /**
     * Starts the key exchange manager.
     * <p>
     * Begins periodic cleanup of expired exchanges.
     * Must be called before initiating any exchanges.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            cleanupTask = scheduler.scheduleAtFixedRate(
                this::cleanupExpiredExchanges,
                CLEANUP_INTERVAL.toMillis(),
                CLEANUP_INTERVAL.toMillis(),
                TimeUnit.MILLISECONDS
            );
            LOG.info("KeyExchangeManager started for client " + localClientId);
        }
    }
    
    /**
     * Shuts down the key exchange manager.
     * <p>
     * Cancels all pending exchanges and stops the cleanup scheduler.
     * After shutdown, no new exchanges can be initiated.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            if (cleanupTask != null) {
                cleanupTask.cancel(false);
            }
            scheduler.shutdown();
            
            // Fail all pending exchanges
            pendingExchanges.forEach((peerId, pending) -> {
                notifyFailed(peerId, new KeyExchangeException(
                    "Manager shutdown",
                    KeyExchangeException.ErrorCode.INTERNAL_ERROR,
                    peerId
                ));
            });
            pendingExchanges.clear();
            
            LOG.info("KeyExchangeManager shutdown for client " + localClientId);
        }
    }
    
    /**
     * Checks if the manager is running.
     *
     * @return true if started and not shutdown
     */
    public boolean isRunning() {
        return running.get();
    }
    
    // ========================= Configuration =========================
    
    /**
     * Sets the message sender callback.
     * <p>
     * The callback is invoked when the manager needs to send KEY_EXCHANGE
     * or KEY_EXCHANGE_RESPONSE messages. The callback should serialize
     * the message data and send it via the network.
     *
     * @param sender the message sender callback
     */
    public void setMessageSender(Consumer<KeyExchangeMessageData> sender) {
        this.messageSender = sender;
    }
    
    /**
     * Adds a key exchange event listener.
     *
     * @param listener the listener to add
     * @throws NullPointerException if listener is null
     */
    public void addListener(KeyExchangeListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        synchronized (listenerLock) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a key exchange event listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(KeyExchangeListener listener) {
        synchronized (listenerLock) {
            listeners.remove(listener);
        }
    }
    
    // ========================= Key Exchange Operations =========================
    
    /**
     * Initiates a key exchange with a peer.
     * <p>
     * Generates an ephemeral keypair and sends a KEY_EXCHANGE message to the peer.
     * The exchange completes when a KEY_EXCHANGE_RESPONSE is received.
     *
     * @param peerId the ID of the peer to exchange keys with
     * @throws KeyExchangeException if the exchange cannot be initiated
     */
    public void initiateKeyExchange(int peerId) throws KeyExchangeException {
        validatePeerId(peerId);
        ensureRunning();
        
        // Check for existing session (use rotation instead)
        if (sessionManager.hasSession(createConversationId(peerId))) {
            throw KeyExchangeException.sessionAlreadyExists(peerId);
        }
        
        // Check for pending exchange
        PendingKeyExchange existing = pendingExchanges.get(peerId);
        if (existing != null && existing.isValid()) {
            throw KeyExchangeException.alreadyInProgress(peerId);
        }
        
        try {
            // Generate ephemeral keypair
            KeyPair ephemeralPair = keyExchange.generateKeyPair();
            
            // Create pending exchange
            PendingKeyExchange pending = new PendingKeyExchange(peerId, ephemeralPair, true);
            pendingExchanges.put(peerId, pending);
            
            // Send KEY_EXCHANGE message
            byte[] publicKeyBytes = encodePublicKey(ephemeralPair.getPublic());
            sendKeyExchangeMessage(peerId, publicKeyBytes, false);
            
            // Notify listeners
            notifyInitiated(peerId, pending);
            
            LOG.info("Initiated key exchange with peer " + peerId);
            
        } catch (GeneralSecurityException e) {
            pendingExchanges.remove(peerId);
            throw KeyExchangeException.cryptoFailure("Key generation", e);
        }
    }
    
    /**
     * Initiates a key exchange if no session exists, otherwise does nothing.
     * <p>
     * Convenience method that silently succeeds if a session already exists.
     *
     * @param peerId the ID of the peer
     * @throws KeyExchangeException if exchange cannot be initiated (other than existing session)
     */
    public void ensureSessionExists(int peerId) throws KeyExchangeException {
        if (!hasSessionWith(peerId)) {
            try {
                initiateKeyExchange(peerId);
            } catch (KeyExchangeException e) {
                if (e.getErrorCode() != KeyExchangeException.ErrorCode.SESSION_ALREADY_EXISTS &&
                    e.getErrorCode() != KeyExchangeException.ErrorCode.EXCHANGE_ALREADY_IN_PROGRESS) {
                    throw e;
                }
            }
        }
    }
    
    /**
     * Handles a KEY_EXCHANGE request received from a peer.
     * <p>
     * Generates an ephemeral keypair, computes the shared secret,
     * derives the session key, and sends a KEY_EXCHANGE_RESPONSE.
     *
     * @param peerId           the ID of the peer who sent the request
     * @param peerPublicKeyBytes the peer's public key (X509 encoded)
     * @throws KeyExchangeException if the request cannot be processed
     */
    public void handleKeyExchangeRequest(int peerId, byte[] peerPublicKeyBytes) 
            throws KeyExchangeException {
        validatePeerId(peerId);
        ensureRunning();
        Objects.requireNonNull(peerPublicKeyBytes, "Public key bytes cannot be null");
        
        LOG.info("Received KEY_EXCHANGE from peer " + peerId);
        notifyReceived(peerId);
        
        try {
            // Decode peer's public key
            PublicKey peerPublicKey = decodePublicKey(peerPublicKeyBytes);
            
            // Generate our ephemeral keypair
            KeyPair ephemeralPair = keyExchange.generateKeyPair();
            
            // Create pending exchange (for tracking)
            PendingKeyExchange pending = new PendingKeyExchange(peerId, ephemeralPair, false);
            pendingExchanges.put(peerId, pending);
            
            // Compute shared secret and derive session key
            String conversationId = createConversationId(peerId);
            SecretKey sessionKey = deriveAndStoreSessionKey(
                ephemeralPair.getPrivate(), 
                peerPublicKey, 
                conversationId
            );
            
            // Mark as completed
            pending = pending.complete();
            pendingExchanges.put(peerId, pending);
            
            // Send response
            byte[] publicKeyBytes = encodePublicKey(ephemeralPair.getPublic());
            sendKeyExchangeMessage(peerId, publicKeyBytes, true);
            
            // Clear retry counter
            retryCounters.remove(peerId);
            
            // Notify listeners
            notifyCompleted(peerId, sessionKey);
            
            LOG.info("Key exchange with peer " + peerId + " completed (responder)");
            
        } catch (GeneralSecurityException e) {
            handleExchangeFailure(peerId, e);
        }
    }
    
    /**
     * Handles a KEY_EXCHANGE_RESPONSE received from a peer.
     * <p>
     * Computes the shared secret using the stored ephemeral private key
     * and the received public key, then derives the session key.
     *
     * @param peerId           the ID of the peer who sent the response
     * @param peerPublicKeyBytes the peer's public key (X509 encoded)
     * @throws KeyExchangeException if the response cannot be processed
     */
    public void handleKeyExchangeResponse(int peerId, byte[] peerPublicKeyBytes) 
            throws KeyExchangeException {
        validatePeerId(peerId);
        ensureRunning();
        Objects.requireNonNull(peerPublicKeyBytes, "Public key bytes cannot be null");
        
        LOG.info("Received KEY_EXCHANGE_RESPONSE from peer " + peerId);
        
        // Retrieve pending exchange
        PendingKeyExchange pending = pendingExchanges.get(peerId);
        if (pending == null || !pending.isValid()) {
            throw KeyExchangeException.noPendingExchange(peerId);
        }
        
        if (!pending.isInitiator()) {
            throw new KeyExchangeException(
                "Received response but we did not initiate exchange with peer " + peerId,
                KeyExchangeException.ErrorCode.PROTOCOL_VIOLATION,
                peerId
            );
        }
        
        try {
            // Decode peer's public key
            PublicKey peerPublicKey = decodePublicKey(peerPublicKeyBytes);
            
            // Compute shared secret and derive session key
            String conversationId = createConversationId(peerId);
            SecretKey sessionKey = deriveAndStoreSessionKey(
                pending.getEphemeralKeyPair().getPrivate(),
                peerPublicKey,
                conversationId
            );
            
            // Mark as completed
            pending = pending.complete();
            pendingExchanges.put(peerId, pending);
            
            // Clear retry counter
            retryCounters.remove(peerId);
            
            // Notify listeners
            notifyCompleted(peerId, sessionKey);
            
            LOG.info("Key exchange with peer " + peerId + " completed (initiator)");
            
        } catch (GeneralSecurityException e) {
            handleExchangeFailure(peerId, e);
        }
    }
    
    // ========================= Key Rotation =========================
    
    /**
     * Rotates the session key with a peer.
     * <p>
     * Initiates a new key exchange that will replace the existing session key.
     * The old key remains valid until the new exchange completes.
     *
     * @param peerId the ID of the peer
     * @throws KeyExchangeException if rotation cannot be initiated
     */
    public void rotateKey(int peerId) throws KeyExchangeException {
        validatePeerId(peerId);
        ensureRunning();
        
        String conversationId = createConversationId(peerId);
        if (!sessionManager.hasSession(conversationId)) {
            throw KeyExchangeException.noSession(peerId);
        }
        
        // Remove old pending exchange if any
        pendingExchanges.remove(peerId);
        
        // Remove old session
        sessionManager.removeSession(conversationId);
        
        // Initiate new exchange
        initiateKeyExchange(peerId);
        
        LOG.info("Initiated key rotation with peer " + peerId);
    }
    
    // ========================= Session Queries =========================
    
    /**
     * Checks if a session exists with a peer.
     *
     * @param peerId the peer ID
     * @return true if a session key exists
     */
    public boolean hasSessionWith(int peerId) {
        return sessionManager.hasSession(createConversationId(peerId));
    }
    
    /**
     * Gets the session key for a peer.
     *
     * @param peerId the peer ID
     * @return the session key, or null if no session exists
     */
    public SecretKey getSessionKey(int peerId) {
        return sessionManager.getSessionKey(createConversationId(peerId));
    }
    
    /**
     * Gets the conversation ID for a peer.
     * <p>
     * Conversation IDs are deterministic and symmetric:
     * createConversationId(A, B) == createConversationId(B, A)
     *
     * @param peerId the peer ID
     * @return the conversation ID
     */
    public String createConversationId(int peerId) {
        int min = Math.min(localClientId, peerId);
        int max = Math.max(localClientId, peerId);
        return min + "_" + max;
    }
    
    /**
     * Checks if a key exchange is pending with a peer.
     *
     * @param peerId the peer ID
     * @return true if an exchange is in progress
     */
    public boolean isExchangePending(int peerId) {
        PendingKeyExchange pending = pendingExchanges.get(peerId);
        return pending != null && pending.isValid();
    }
    
    /**
     * Gets the pending exchange for a peer, if any.
     *
     * @param peerId the peer ID
     * @return the pending exchange, or null if none
     */
    public PendingKeyExchange getPendingExchange(int peerId) {
        return pendingExchanges.get(peerId);
    }
    
    /**
     * Gets the local client ID.
     *
     * @return the local client ID
     */
    public int getLocalClientId() {
        return localClientId;
    }
    
    /**
     * Gets all peer IDs with active sessions.
     *
     * @return unmodifiable set of peer IDs
     */
    public Set<Integer> getActivePeers() {
        Set<Integer> peers = new HashSet<>();
        for (String convId : sessionManager.getActiveConversations()) {
            // Parse peer ID from conversation ID (format: "min_max")
            String[] parts = convId.split("_");
            if (parts.length == 2) {
                try {
                    int id1 = Integer.parseInt(parts[0]);
                    int id2 = Integer.parseInt(parts[1]);
                    peers.add(id1 == localClientId ? id2 : id1);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return Collections.unmodifiableSet(peers);
    }
    
    /**
     * Invalidates a session with a peer.
     * <p>
     * Removes the session key from memory and storage.
     * A new key exchange will be required to communicate.
     *
     * @param peerId the peer ID
     * @param reason the reason for invalidation
     */
    public void invalidateSession(int peerId, String reason) {
        String conversationId = createConversationId(peerId);
        
        sessionManager.removeSession(conversationId);
        pendingExchanges.remove(peerId);
        
        // Remove from persistent storage
        if (keyStore != null) {
            try {
                keyStore.deleteSessionKey(conversationId);
            } catch (IOException e) {
                LOG.warning("Failed to delete session key from storage: " + e.getMessage());
            }
        }
        
        notifyInvalidated(peerId, reason);
        LOG.info("Invalidated session with peer " + peerId + ": " + reason);
    }
    
    // ========================= Private: Key Derivation =========================
    
    /**
     * Derives and stores the session key from the shared secret.
     */
    private SecretKey deriveAndStoreSessionKey(
            PrivateKey myPrivateKey, 
            PublicKey peerPublicKey,
            String conversationId) throws GeneralSecurityException {
        
        // Compute ECDH shared secret
        byte[] sharedSecret = keyExchange.deriveSharedSecret(myPrivateKey, peerPublicKey);
        
        // Derive session key using HKDF
        byte[] sessionKeyBytes = keyExchange.deriveSessionKey(sharedSecret, conversationId);
        SecretKey sessionKey = new SecretKeySpec(sessionKeyBytes, "AES");
        
        // Store in session manager
        sessionManager.storeSessionKey(conversationId, sessionKey);
        
        // Persist to storage
        persistSessionKey(conversationId, sessionKey);
        
        // Clear sensitive data
        Arrays.fill(sharedSecret, (byte) 0);
        Arrays.fill(sessionKeyBytes, (byte) 0);
        
        return sessionKey;
    }
    
    /**
     * Persists a session key to storage.
     */
    private void persistSessionKey(String conversationId, SecretKey key) {
        if (keyStore == null) {
            return;
        }
        
        try {
            keyStore.saveSessionKey(conversationId, key);
            LOG.fine("Persisted session key for conversation " + conversationId);
        } catch (IOException e) {
            LOG.warning("Failed to persist session key: " + e.getMessage());
        }
    }
    
    /**
     * Loads persisted session keys on startup.
     */
    private void loadPersistedSessions() {
        // Note: The current KeyStore interface doesn't provide a list method.
        // This would need to be added, or we track conversation IDs separately.
        // For now, sessions will be established on-demand.
        LOG.fine("Session loading skipped (KeyStore doesn't support listing)");
    }
    
    // ========================= Private: Public Key Encoding =========================
    
    /**
     * Encodes a public key to bytes.
     */
    private byte[] encodePublicKey(PublicKey publicKey) {
        return publicKey.getEncoded();
    }
    
    /**
     * Decodes a public key from bytes.
     */
    private PublicKey decodePublicKey(byte[] encodedKey) throws KeyExchangeException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, CRYPTO_PROVIDER);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw KeyExchangeException.cryptoFailure("Key algorithm not available", e);
        } catch (InvalidKeySpecException e) {
            throw new KeyExchangeException(
                "Invalid public key format",
                KeyExchangeException.ErrorCode.INVALID_PUBLIC_KEY,
                e
            );
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new KeyExchangeException(
                "Public key data is corrupted or incomplete",
                KeyExchangeException.ErrorCode.INVALID_PUBLIC_KEY,
                e
            );
        }
    }
    
    // ========================= Private: Message Sending =========================
    
    /**
     * Sends a key exchange message to a peer.
     */
    private void sendKeyExchangeMessage(int peerId, byte[] publicKeyBytes, boolean isResponse) {
        Consumer<KeyExchangeMessageData> sender = this.messageSender;
        if (sender == null) {
            LOG.warning("No message sender configured, cannot send key exchange message");
            return;
        }
        
        KeyExchangeMessageData data = new KeyExchangeMessageData(
            localClientId,
            peerId,
            publicKeyBytes,
            isResponse
        );
        
        sender.accept(data);
    }
    
    // ========================= Private: Error Handling =========================
    
    /**
     * Handles a key exchange failure.
     */
    private void handleExchangeFailure(int peerId, Exception cause) throws KeyExchangeException {
        PendingKeyExchange pending = pendingExchanges.get(peerId);
        if (pending != null) {
            pendingExchanges.put(peerId, pending.fail());
        }
        
        KeyExchangeException exception;
        if (cause instanceof KeyExchangeException kee) {
            exception = kee;
        } else {
            exception = new KeyExchangeException(
                "Key exchange failed with peer " + peerId + ": " + cause.getMessage(),
                KeyExchangeException.ErrorCode.CRYPTO_FAILURE,
                peerId,
                cause
            );
        }
        
        // Check for retry
        int retries = retryCounters.getOrDefault(peerId, 0);
        if (exception.isRecoverable() && retries < MAX_RETRY_ATTEMPTS) {
            retryCounters.put(peerId, retries + 1);
            LOG.info("Will retry key exchange with peer " + peerId + " (attempt " + (retries + 1) + ")");
            // Schedule retry? Or let the caller handle it.
        }
        
        notifyFailed(peerId, exception);
        throw exception;
    }
    
    // ========================= Private: Cleanup =========================
    
    /**
     * Cleans up expired key exchanges.
     */
    private void cleanupExpiredExchanges() {
        pendingExchanges.forEach((peerId, pending) -> {
            if (pending.isExpiredOrMarkedExpired() && !pending.isCompleted()) {
                PendingKeyExchange expired = pending.expire();
                pendingExchanges.put(peerId, expired);
                notifyExpired(peerId);
                LOG.info("Key exchange with peer " + peerId + " expired");
            }
        });
        
        // Remove completed/failed exchanges older than cleanup interval
        pendingExchanges.entrySet().removeIf(entry -> {
            PendingKeyExchange p = entry.getValue();
            return p.getState().isTerminal() && 
                   p.getElapsedTime().compareTo(CLEANUP_INTERVAL.multipliedBy(2)) > 0;
        });
    }
    
    // ========================= Private: Validation =========================
    
    /**
     * Validates a peer ID.
     */
    private void validatePeerId(int peerId) throws KeyExchangeException {
        if (peerId <= 0) {
            throw KeyExchangeException.invalidPeerId(peerId);
        }
        if (peerId == localClientId) {
            throw new KeyExchangeException(
                "Cannot exchange keys with self",
                KeyExchangeException.ErrorCode.INVALID_PEER_ID,
                peerId
            );
        }
    }
    
    /**
     * Ensures the manager is running.
     */
    private void ensureRunning() throws KeyExchangeException {
        if (!running.get()) {
            throw new KeyExchangeException(
                "KeyExchangeManager is not running",
                KeyExchangeException.ErrorCode.INTERNAL_ERROR
            );
        }
    }
    
    // ========================= Private: Listener Notifications =========================
    
    private void notifyInitiated(int peerId, PendingKeyExchange pending) {
        for (KeyExchangeListener listener : listeners) {
            try {
                listener.onKeyExchangeInitiated(peerId, pending);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Listener threw exception", e);
            }
        }
    }
    
    private void notifyReceived(int peerId) {
        for (KeyExchangeListener listener : listeners) {
            try {
                listener.onKeyExchangeReceived(peerId);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Listener threw exception", e);
            }
        }
    }
    
    private void notifyCompleted(int peerId, SecretKey sessionKey) {
        for (KeyExchangeListener listener : listeners) {
            try {
                listener.onKeyExchangeCompleted(peerId, sessionKey);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Listener threw exception", e);
            }
        }
    }
    
    private void notifyFailed(int peerId, KeyExchangeException cause) {
        for (KeyExchangeListener listener : listeners) {
            try {
                listener.onKeyExchangeFailed(peerId, cause);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Listener threw exception", e);
            }
        }
    }
    
    private void notifyExpired(int peerId) {
        for (KeyExchangeListener listener : listeners) {
            try {
                listener.onKeyExchangeExpired(peerId);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Listener threw exception", e);
            }
        }
    }
    
    private void notifyInvalidated(int peerId, String reason) {
        for (KeyExchangeListener listener : listeners) {
            try {
                listener.onSessionInvalidated(peerId, reason);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Listener threw exception", e);
            }
        }
    }
    
    // ========================= Inner Class: Message Data =========================
    
    /**
     * Data class for key exchange messages to be sent.
     * <p>
     * This class is used as a transfer object to the message sender callback,
     * allowing the caller to construct the appropriate ProtocolMessage.
     */
    public static final class KeyExchangeMessageData {
        private final int fromId;
        private final int toId;
        private final byte[] publicKey;
        private final boolean isResponse;
        
        public KeyExchangeMessageData(int fromId, int toId, byte[] publicKey, boolean isResponse) {
            this.fromId = fromId;
            this.toId = toId;
            this.publicKey = publicKey.clone();
            this.isResponse = isResponse;
        }
        
        public int getFromId() { return fromId; }
        public int getToId() { return toId; }
        public byte[] getPublicKey() { return publicKey.clone(); }
        public boolean isResponse() { return isResponse; }
        
        @Override
        public String toString() {
            return String.format("KeyExchangeMessageData{from=%d, to=%d, isResponse=%s, keyLen=%d}",
                fromId, toId, isResponse, publicKey.length);
        }
    }
}
