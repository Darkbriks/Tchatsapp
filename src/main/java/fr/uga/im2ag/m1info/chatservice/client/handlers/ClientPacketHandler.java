package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientContext;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

/**
 * Abstract base class for handling client-side packets.
 * Handlers receive a ClientContext to interact with client functionality.
 */
public abstract class ClientPacketHandler {
    /**
     * Handles the given protocol message.
     *
     * @param message the protocol message to be handled
     * @param context the client context providing access to client operations
     */
    public abstract void handle(ProtocolMessage message, ClientContext context);

    /**
     * Determines if this handler can handle messages of the specified type.
     *
     * @param messageType the type of message to check
     * @return true if this handler can handle the specified message type, false otherwise
     */
    public abstract boolean canHandle(MessageType messageType);
}
