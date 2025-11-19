package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

/**
 * Abstract base class for handling client-side packets.
 * Handlers receive a ClientController to interact with client functionality.
 */
public abstract class ClientPacketHandler {
    /**
     * Handles the given protocol message.
     *
     * @param message the protocol message to be handled
     * @param context the client context providing access to client operations
     */
    public abstract void handle(ProtocolMessage message, ClientController context);

    /**
     * Determines if this handler can handle messages of the specified type.
     *
     * @param messageType the type of message to check
     * @return true if this handler can handle the specified message type, false otherwise
     */
    public abstract boolean canHandle(MessageType messageType);

    /**
     * Publishes an event using the provided client context.
     *
     * @param event the event to be published
     * @param context the client context used to publish the event
     */
    protected void publishEvent(Event event, ClientController context) {
        context.publishEvent(event);
    }
}
