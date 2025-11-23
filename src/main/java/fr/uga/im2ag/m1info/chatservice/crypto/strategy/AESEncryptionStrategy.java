package fr.uga.im2ag.m1info.chatservice.crypto.strategy;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.EncryptedWrapper;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.crypto.SessionKeyManager;
import fr.uga.im2ag.m1info.chatservice.crypto.SymmetricCipher;
import fr.uga.im2ag.m1info.chatservice.crypto.context.ConversationEncryptionContext;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.KeyExchangeManager;

import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AES-256-GCM encryption strategy for end-to-end encrypted messaging.
 * <p>
 * This strategy implements full E2EE using:
 * <ul>
 *   <li>AES-256-GCM for symmetric encryption with authentication</li>
 *   <li>Per-conversation session keys managed by {@link SessionKeyManager}</li>
 *   <li>ECDH key exchange via {@link KeyExchangeManager}</li>
 *   <li>Sequence numbers for replay protection</li>
 *   <li>AAD for metadata authentication (from, to, sequence)</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe. Context caching uses ConcurrentHashMap.
 * <p>
 * Usage:
 * <pre>{@code
 * AESEncryptionStrategy strategy = new AESEncryptionStrategy(
 *     localClientId, sessionManager, keyExchangeManager
 * );
 * 
 * // Encrypt before sending
 * if (strategy.shouldEncrypt(message.getMessageType(), message.getTo())) {
 *     message = strategy.encrypt(message);
 * }
 * 
 * // Decrypt after receiving
 * if (message instanceof EncryptedWrapper wrapper) {
 *     message = strategy.decrypt(wrapper);
 * }
 * }</pre>
 *
 * @see EncryptionStrategy
 * @see ConversationEncryptionContext
 * @see SessionKeyManager
 */
public class AESEncryptionStrategy implements EncryptionStrategy {

    private static final Logger LOG = Logger.getLogger(AESEncryptionStrategy.class.getName());

    // ========================= Message Types Excluded from Encryption =========================

    /**
     * Message types that should never be encrypted.
     * These are protocol-level messages required for establishing encryption.
     */
    private static final Set<MessageType> EXCLUDED_TYPES = Set.of(
            MessageType.KEY_EXCHANGE,
            MessageType.KEY_EXCHANGE_RESPONSE,
            MessageType.ENCRYPTED
    );

    // ========================= Dependencies =========================

    private volatile int localClientId;
    private final SessionKeyManager sessionManager;
    private final KeyExchangeManager keyExchangeManager;
    private final SymmetricCipher cipher;

    /** Cache of encryption contexts per peer ID */
    private final Map<Integer, ConversationEncryptionContext> contextCache;

    // ========================= Constructor =========================

    /**
     * Creates a new AESEncryptionStrategy.
     *
     * @param localClientId      the local client's ID (can be 0 for new users, updated later)
     * @param sessionManager     the session key manager
     * @param keyExchangeManager the key exchange manager
     * @throws NullPointerException if sessionManager or keyExchangeManager is null
     */
    public AESEncryptionStrategy(
            int localClientId,
            SessionKeyManager sessionManager,
            KeyExchangeManager keyExchangeManager) {

        Objects.requireNonNull(sessionManager, "Session manager cannot be null");
        Objects.requireNonNull(keyExchangeManager, "Key exchange manager cannot be null");

        this.localClientId = localClientId;
        this.sessionManager = sessionManager;
        this.keyExchangeManager = keyExchangeManager;
        this.cipher = new SymmetricCipher();
        this.contextCache = new ConcurrentHashMap<>();
    }

    // ========================= Client ID Management =========================

    /**
     * Updates the local client ID.
     * <p>
     * This is necessary when a new user is created and the server assigns an ID.
     * Clears the context cache since contexts depend on the local client ID.
     *
     * @param newClientId the new client ID (must be positive)
     * @throws IllegalArgumentException if newClientId is not positive
     */
    public void updateLocalClientId(int newClientId) {
        if (newClientId <= 0) {
            throw new IllegalArgumentException("Client ID must be positive: " + newClientId);
        }

        LOG.info("Updating local client ID from " + localClientId + " to " + newClientId);
        this.localClientId = newClientId;

        // Clear cached contexts as they contain the old client ID
        contextCache.clear();
    }

    /**
     * Gets the current local client ID.
     *
     * @return the local client ID
     */
    public int getLocalClientId() {
        return localClientId;
    }

    // ========================= EncryptionStrategy Implementation =========================

    /**
     * {@inheritDoc}
     * <p>
     * Wraps the message in an {@link EncryptedWrapper} using AES-256-GCM.
     *
     * @throws GeneralSecurityException if encryption fails
     * @throws IllegalStateException    if no session key exists for the recipient
     */
    @Override
    public ProtocolMessage encrypt(ProtocolMessage message) throws GeneralSecurityException {
        Objects.requireNonNull(message, "Message cannot be null");

        int recipientId = message.getTo();

        if (!hasSessionKey(recipientId)) {
            throw new IllegalStateException(
                    "No session key for recipient " + recipientId +
                            ". Initiate key exchange first."
            );
        }

        ConversationEncryptionContext context = getOrCreateContext(recipientId);

        LOG.fine(() -> "Encrypting " + message.getMessageType() + " to " + recipientId);

        EncryptedWrapper wrapper = EncryptedWrapper.wrap(message, context);

        // Check if key rotation is needed
        if (context.shouldRotateKey()) {
            LOG.info("Key rotation recommended for peer " + recipientId);
            // Could trigger async key rotation here
        }

        return wrapper;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Extracts and decrypts the original message from the wrapper.
     * Validates the sequence number to prevent replay attacks.
     *
     * @throws GeneralSecurityException if decryption fails or authentication fails
     * @throws SecurityException        if replay attack detected
     */
    @Override
    public ProtocolMessage decrypt(EncryptedWrapper wrapper) throws GeneralSecurityException {
        Objects.requireNonNull(wrapper, "Wrapper cannot be null");

        int senderId = wrapper.getFrom();

        if (!hasSessionKey(senderId)) {
            throw new IllegalStateException(
                    "No session key for sender " + senderId +
                            ". Key exchange may not have completed."
            );
        }

        ConversationEncryptionContext context = getOrCreateContext(senderId);

        LOG.fine(() -> "Decrypting " + wrapper.getOriginalType() + " from " + senderId);

        try {
            return wrapper.unwrap(context);
        } catch (SecurityException e) {
            LOG.log(Level.SEVERE, "Potential replay attack from " + senderId, e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns true if:
     * <ul>
     *   <li>The message type is not in the excluded set</li>
     *   <li>The recipient is not the server (ID 0)</li>
     *   <li>A session key exists for the recipient</li>
     *   <li>The local client ID is valid (> 0)</li>
     * </ul>
     */
    @Override
    public boolean shouldEncrypt(MessageType type, int recipientId) {
        // Never encrypt excluded types
        if (isExcludedFromEncryption(type)) {
            return false;
        }

        // Don't encrypt messages to server
        if (recipientId == 0) {
            return false;
        }

        // Can't encrypt if we don't have a valid client ID yet
        if (localClientId <= 0) {
            return false;
        }

        // Only encrypt if we have a session key
        return hasSessionKey(recipientId);
    }

    /**
     * {@inheritDoc}
     *
     * @return always true for this strategy
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasSessionKey(int peerId) {
        if (peerId <= 0 || localClientId <= 0) {
            return false;
        }
        String conversationId = createConversationId(peerId);
        return sessionManager.hasSession(conversationId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Initiates ECDH key exchange if no session exists.
     */
    @Override
    public void ensureSessionExists(int peerId) throws GeneralSecurityException {
        if (hasSessionKey(peerId)) {
            return;
        }

        if (localClientId <= 0) {
            throw new IllegalStateException(
                    "Cannot initiate key exchange: local client ID not set"
            );
        }

        try {
            keyExchangeManager.initiateKeyExchange(peerId);
            LOG.info("Initiated key exchange with peer " + peerId);
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to initiate key exchange", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<MessageType> getExcludedMessageTypes() {
        return EXCLUDED_TYPES;
    }

    @Override
    public String getName() {
        return "AES-256-GCM E2EE";
    }

    // ========================= Context Management =========================

    /**
     * Gets or creates an encryption context for a peer.
     *
     * @param peerId the peer ID
     * @return the encryption context
     */
    private ConversationEncryptionContext getOrCreateContext(int peerId) {
        return contextCache.computeIfAbsent(peerId, this::createContext);
    }

    /**
     * Creates a new encryption context for a peer.
     *
     * @param peerId the peer ID
     * @return a new encryption context
     */
    private ConversationEncryptionContext createContext(int peerId) {
        String conversationId = createConversationId(peerId);
        return new ConversationEncryptionContext(
                conversationId,
                sessionManager,
                cipher,
                localClientId,
                peerId
        );
    }

    /**
     * Creates a deterministic conversation ID for a peer.
     * <p>
     * The ID is symmetric: conversationId(A, B) == conversationId(B, A)
     *
     * @param peerId the peer ID
     * @return the conversation ID
     */
    private String createConversationId(int peerId) {
        int min = Math.min(localClientId, peerId);
        int max = Math.max(localClientId, peerId);
        return min + "_" + max;
    }

    /**
     * Invalidates the cached context for a peer.
     * <p>
     * Should be called after key rotation.
     *
     * @param peerId the peer ID
     */
    public void invalidateContext(int peerId) {
        contextCache.remove(peerId);
        LOG.fine("Invalidated context cache for peer " + peerId);
    }

    /**
     * Clears all cached contexts.
     * <p>
     * Useful when local client ID changes or on logout.
     */
    public void clearAllContexts() {
        contextCache.clear();
        LOG.fine("Cleared all context caches");
    }

    @Override
    public String toString() {
        return "AESEncryptionStrategy{" +
                "localClientId=" + localClientId +
                ", cachedContexts=" + contextCache.size() +
                ", enabled=" + isEnabled() +
                '}';
    }
}
