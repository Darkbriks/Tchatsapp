package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

/**
 * Abstract class for handling server packets.
 */
public abstract class ServerPacketHandler {
    /** Handle the given protocol message.
     *
     * @param message the protocol message to handle
     * @param serverContext the server context
     */
    public abstract void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext);

     /** Check if this handler can handle the given message type.
     *
     * @param messageType the message type to check
     * @return true if this handler can handle the message type, false otherwise
     */
    public abstract boolean canHandle(MessageType messageType);
}
