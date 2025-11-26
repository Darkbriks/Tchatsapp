package fr.uga.im2ag.m1info.chatservice.server.processor;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.PacketProcessor;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.encryption.ServerEncryptionService;

import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerDecryptingPacketProcessor implements PacketProcessor {

    private static final Logger LOG = Logger.getLogger(ServerDecryptingPacketProcessor.class.getName());

    // ========================= Dependencies =========================

    private final PacketProcessor delegate;
    private final ServerEncryptionService encryptionService;
    private final TchatsAppServer.ServerContext serverContext;

    // ========================= Optional Callbacks =========================

    /** Callback invoked when decryption fails */
    private volatile DecryptionErrorCallback errorCallback;

    // ========================= Constructor =========================

    /**
     * Creates a new ServerDecryptingPacketProcessor.
     *
     * @param delegate          the wrapped packet processor
     * @param encryptionService the server encryption service
     * @param serverContext     the server context (for accessing current connection state)
     * @throws NullPointerException if any parameter is null
     */
    public ServerDecryptingPacketProcessor(
            PacketProcessor delegate,
            ServerEncryptionService encryptionService,
            TchatsAppServer.ServerContext serverContext) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate processor cannot be null");
        this.encryptionService = Objects.requireNonNull(encryptionService, "Encryption service cannot be null");
        this.serverContext = Objects.requireNonNull(serverContext, "Server context cannot be null");
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
     * If the message is of type {@link MessageType#SERVER_ENCRYPTED}:
     * <ol>
     *   <li>Get the current connection's channel from ServerContext</li>
     *   <li>Decrypt using the ServerEncryptionService</li>
     *   <li>Pass the decrypted message to the delegate</li>
     * </ol>
     * <p>
     * Non-encrypted messages and key exchange messages pass through directly.
     *
     * @param message the incoming message
     */
    @Override
    public void process(ProtocolMessage message) {
        if (message == null) {
            LOG.warning("Received null message, ignoring");
            return;
        }

        // Key exchange messages are handled separately, pass through
        if (message.getMessageType() == MessageType.SERVER_KEY_EXCHANGE_RESPONSE) {
            delegate.process(message);
            return;
        }

        // Check if message needs decryption
        if (message.getMessageType() == MessageType.SERVER_ENCRYPTED) {
            ProtocolMessage decrypted = tryDecrypt(message);
            if (decrypted != null) {
                delegate.process(decrypted);
            }
            // If decryption failed, message is dropped (error already logged)
            return;
        }

        // Non-encrypted message, pass through
        delegate.process(message);
    }

    // ========================= Private Methods =========================

    /**
     * Attempts to decrypt an encrypted message.
     *
     * @param message the encrypted message
     * @return the decrypted message, or null if decryption failed
     */
    private ProtocolMessage tryDecrypt(ProtocolMessage message) {
        // Get current connection state
        TchatsAppServer.ConnectionState state = serverContext.getCurrentConnectionState();
        if (state == null) {
            LOG.warning("Cannot decrypt: no current connection state");
            notifyError(message.getFrom(), "No connection state available", null);
            return null;
        }

        SocketChannel channel = state.getChannel();
        if (channel == null) {
            LOG.warning("Cannot decrypt: null channel in connection state");
            notifyError(message.getFrom(), "No channel available", null);
            return null;
        }

        // Check if encryption is established
        if (!encryptionService.hasSecureSession(channel)) {
            LOG.warning("Cannot decrypt: no secure session for channel " + channel);
            notifyError(message.getFrom(), "No secure session established", null);
            return null;
        }

        try {
            // Convert message back to packet for decryption
            Packet encryptedPacket = message.toPacket();
            Packet decryptedPacket = encryptionService.decryptIncoming(channel, encryptedPacket);

            // Convert back to ProtocolMessage
            ProtocolMessage decrypted = MessageFactory.fromPacket(decryptedPacket);

            LOG.fine(() -> "Decrypted message from " + message.getFrom() +
                    ": " + decrypted.getMessageType());

            return decrypted;

        } catch (GeneralSecurityException e) {
            LOG.log(Level.WARNING, "Decryption failed for message from " + message.getFrom(), e);
            notifyError(message.getFrom(), "Decryption failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Notifies the error callback if set.
     *
     * @param senderId the sender of the failed message
     * @param errorMsg the error message
     * @param cause    the exception that caused the error (can be null)
     */
    private void notifyError(int senderId, String errorMsg, Exception cause) {
        DecryptionErrorCallback callback = this.errorCallback;
        if (callback != null) {
            try {
                callback.onDecryptionError(senderId, errorMsg, cause);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error callback threw exception", e);
            }
        }
    }

    // ========================= Callback Interface =========================

    /**
     * Callback interface for decryption errors.
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
        return "ServerDecryptingPacketProcessor{delegate=" + delegate.getClass().getSimpleName() + "}";
    }
}
