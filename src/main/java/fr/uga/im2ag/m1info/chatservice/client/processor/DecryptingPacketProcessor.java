package fr.uga.im2ag.m1info.chatservice.client.processor;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.PacketProcessor;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.EncryptedWrapper;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.crypto.strategy.EncryptionStrategy;

import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorator for {@link PacketProcessor} that transparently decrypts incoming encrypted messages.
 * <p>
 * This processor intercepts messages of type {@link MessageType#ENCRYPTED}, decrypts them
 * using the provided {@link EncryptionStrategy}, and passes the decrypted message to the
 * wrapped processor. Non-encrypted messages pass through unchanged.
 * <p>
 * Design Pattern: Decorator
 * <p>
 * Processing flow:
 * <pre>
 * Incoming Message
 *       │
 *       ▼
 * ┌─────────────────────────────┐
 * │  DecryptingPacketProcessor  │
 * │  ┌───────────────────────┐  │
 * │  │ Is ENCRYPTED type?    │  │
 * │  └───────────────────────┘  │
 * │        Yes       No         │
 * │        ▼         │          │
 * │   ┌─────────┐    │          │
 * │   │ Decrypt │    │          │
 * │   └────┬────┘    │          │
 * │        │         │          │
 * │        ▼         ▼          │
 * │  ┌───────────────────────┐  │
 * │  │   Wrapped Processor   │  │
 * │  └───────────────────────┘  │
 * └─────────────────────────────┘
 * </pre>
 * <p>
 * Error Handling:
 * <ul>
 *   <li>If decryption fails, the error is logged and the message is dropped</li>
 *   <li>If replay attack is detected, a {@link SecurityException} is logged</li>
 *   <li>The error callback (if set) is invoked for UI notification</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe if the wrapped processor and strategy are thread-safe.
 * <p>
 * Usage:
 * <pre>{@code
 * PacketProcessor baseProcessor = new ClientPaquetRouter(handlers, controller);
 * EncryptionStrategy strategy = new AESEncryptionStrategy(...);
 * 
 * PacketProcessor decryptingProcessor = new DecryptingPacketProcessor(baseProcessor, strategy);
 * client.setPacketProcessor(decryptingProcessor);
 * }</pre>
 *
 * @see PacketProcessor
 * @see EncryptionStrategy
 * @see EncryptedWrapper
 */
public class DecryptingPacketProcessor implements PacketProcessor {

    private static final Logger LOG = Logger.getLogger(DecryptingPacketProcessor.class.getName());

    // ========================= Dependencies =========================

    private final PacketProcessor delegate;
    private final EncryptionStrategy encryptionStrategy;

    // ========================= Optional Callbacks =========================

    /** Callback invoked when decryption fails */
    private volatile DecryptionErrorCallback errorCallback;

    // ========================= Constructor =========================

    /**
     * Creates a new DecryptingPacketProcessor.
     *
     * @param delegate           the wrapped packet processor
     * @param encryptionStrategy the encryption strategy for decryption
     * @throws NullPointerException if any parameter is null
     */
    public DecryptingPacketProcessor(PacketProcessor delegate, EncryptionStrategy encryptionStrategy) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate processor cannot be null");
        this.encryptionStrategy = Objects.requireNonNull(encryptionStrategy, "Encryption strategy cannot be null");
    }

    // ========================= Configuration =========================

    /**
     * Sets the error callback invoked when decryption fails.
     *
     * @param callback the error callback (can be null to disable)
     */
    public void setErrorCallback(DecryptionErrorCallback callback) {
        this.errorCallback = callback;
    }

    // ========================= PacketProcessor Implementation =========================

    /**
     * Processes an incoming message, decrypting it if necessary.
     * <p>
     * If the message is of type {@link MessageType#ENCRYPTED}:
     * <ol>
     *   <li>Cast to {@link EncryptedWrapper}</li>
     *   <li>Decrypt using the encryption strategy</li>
     *   <li>Pass the decrypted message to the delegate</li>
     * </ol>
     * <p>
     * Non-encrypted messages pass through directly to the delegate.
     *
     * @param message the incoming message
     */
    @Override
    public void process(ProtocolMessage message) {
        if (message == null) {
            LOG.warning("Received null message, ignoring");
            return;
        }

        ProtocolMessage processedMessage = message;

        // Check if message needs decryption
        if (message.getMessageType() == MessageType.ENCRYPTED) {
            if (!(message instanceof EncryptedWrapper wrapper)) {
                LOG.severe("Message type is ENCRYPTED but not an EncryptedWrapper instance: " +
                        message.getClass().getName());
                notifyError(message.getFrom(), "Invalid encrypted message format", null);
                return;
            }

            try {
                processedMessage = decryptMessage(wrapper);
                ProtocolMessage finalProcessedMessage = processedMessage;
                LOG.fine(() -> "Decrypted message from " + wrapper.getFrom() + ": " + finalProcessedMessage.getMessageType());
            } catch (SecurityException e) {
                // Replay attack or security violation
                LOG.log(Level.SEVERE, "Security violation from " + wrapper.getFrom(), e);
                notifyError(wrapper.getFrom(), "Security violation: " + e.getMessage(), e);
                return; // Drop the message
            } catch (GeneralSecurityException e) {
                // Decryption failed (wrong key, corrupted, etc.)
                LOG.log(Level.SEVERE, "Decryption failed for message from " + wrapper.getFrom(), e);
                notifyError(wrapper.getFrom(), "Decryption failed: " + e.getMessage(), e);
                return; // Drop the message
            } catch (IllegalStateException e) {
                // No session key
                LOG.log(Level.WARNING, "No session key for message from " + wrapper.getFrom() + "(" + e.getMessage() + ")", e);
                notifyError(wrapper.getFrom(), "No session key available", e);
                return; // Drop the message
            }
        }

        // Pass to delegate (either original or decrypted message)
        delegate.process(processedMessage);
    }

    // ========================= Private Methods =========================

    /**
     * Decrypts an encrypted wrapper.
     *
     * @param wrapper the encrypted wrapper
     * @return the decrypted message
     * @throws GeneralSecurityException if decryption fails
     */
    private ProtocolMessage decryptMessage(EncryptedWrapper wrapper) throws GeneralSecurityException {
        if (!encryptionStrategy.isEnabled()) {
            throw new IllegalStateException("Encryption strategy is disabled but received encrypted message");
        }

        return encryptionStrategy.decrypt(wrapper);
    }

    /**
     * Notifies the error callback if set.
     *
     * @param senderId the sender of the failed message
     * @param message  the error message
     * @param cause    the exception that caused the error (can be null)
     */
    private void notifyError(int senderId, String message, Exception cause) {
        DecryptionErrorCallback callback = this.errorCallback;
        if (callback != null) {
            try {
                callback.onDecryptionError(senderId, message, cause);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error callback threw exception", e);
            }
        }
    }

    // ========================= Callback Interface =========================

    /**
     * Callback interface for decryption errors.
     * <p>
     * Implementations can use this to notify the UI of decryption failures.
     */
    @FunctionalInterface
    public interface DecryptionErrorCallback {

        /**
         * Called when decryption fails.
         *
         * @param senderId the ID of the message sender
         * @param message  a human-readable error message
         * @param cause    the exception that caused the error (can be null)
         */
        void onDecryptionError(int senderId, String message, Exception cause);
    }

    @Override
    public String toString() {
        return "DecryptingPacketProcessor{" +
                "delegate=" + delegate.getClass().getSimpleName() +
                ", strategy=" + encryptionStrategy.getName() +
                '}';
    }
}
