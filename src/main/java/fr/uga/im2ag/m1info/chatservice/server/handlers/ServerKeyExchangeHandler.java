package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ServerKeyExchangeMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.encryption.ServerEncryptionService;

import java.nio.channels.SocketChannel;
import java.util.Objects;

public class ServerKeyExchangeHandler extends ServerPacketHandler {

    private ServerEncryptionService encryptionService;

    /**
     * Default constructor for ServiceLoader.
     */
    public ServerKeyExchangeHandler() {
        // Encryption service will be set via setter
    }

    /**
     * Creates a handler with the specified encryption service.
     *
     * @param encryptionService the server encryption service
     */
    public ServerKeyExchangeHandler(ServerEncryptionService encryptionService) {
        this.encryptionService = Objects.requireNonNull(encryptionService,
                "Encryption service cannot be null");
    }

    /**
     * Sets the encryption service.
     * <p>
     * Must be called before handling any messages if using the default constructor.
     *
     * @param encryptionService the server encryption service
     */
    public void setEncryptionService(ServerEncryptionService encryptionService) {
        this.encryptionService = Objects.requireNonNull(encryptionService,
                "Encryption service cannot be null");
    }

    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (encryptionService == null) {
            LOG.severe("ServerKeyExchangeHandler: encryption service not set");
            return;
        }

        if (!(message instanceof ServerKeyExchangeMessage keyExchangeMsg)) {
            LOG.warning("Expected ServerKeyExchangeMessage but got: " + message.getClass().getName());
            return;
        }

        TchatsAppServer.ConnectionState state = serverContext.getCurrentConnectionState();
        if (state == null) {
            LOG.warning("SERVER_KEY_EXCHANGE_RESPONSE received without connection state");
            return;
        }

        SocketChannel channel = state.getChannel();
        if (channel == null) {
            LOG.warning("SERVER_KEY_EXCHANGE_RESPONSE received with null channel");
            return;
        }

        // Check if we're expecting a key exchange response
        if (!encryptionService.isKeyExchangePending(channel)) {
            LOG.warning("Received unexpected SERVER_KEY_EXCHANGE_RESPONSE for channel: " + channel);
            return;
        }

        // Complete the key exchange
        boolean success = encryptionService.handleKeyExchangeResponse(channel, message.toPacket());

        if (success) {
            state.setEncryptionEstablished(true);
            LOG.info("Encryption established for channel: " + channel);
        } else {
            LOG.warning("Key exchange failed for channel: " + channel);
            serverContext.closeConnection(state);
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.SERVER_KEY_EXCHANGE_RESPONSE;
    }
}
