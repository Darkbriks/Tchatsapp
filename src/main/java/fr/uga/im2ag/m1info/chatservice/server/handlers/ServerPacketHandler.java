package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

import java.util.logging.Logger;

/**
 * Abstract class for handling server packets.
 */
public abstract class ServerPacketHandler {
    protected static final Logger LOG = Logger.getLogger(ServerPacketHandler.class.getName());

    /**
     * Initializes the handler with required dependencies.
     * Called after construction by the ServiceLoader mechanism.
     * Default implementation does nothing; override if dependencies are needed.
     *
     * @param context the context providing dependencies
     */
    public void initialize(ServerHandlerContext context) {
    }

    /**
     * Handle the given protocol message.
     *
     * @param message the protocol message to handle
     * @param serverContext the server context
     */
    public abstract void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext);

    /**
     * Check if this handler can handle the given message type.
     *
     * @param messageType the message type to check
     * @return true if this handler can handle the message type, false otherwise
     */
    public abstract boolean canHandle(MessageType messageType);
}
