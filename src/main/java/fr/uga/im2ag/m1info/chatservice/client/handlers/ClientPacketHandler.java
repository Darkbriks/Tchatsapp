package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

public abstract class ClientPacketHandler {
    // TODO

    /**
     * Handles the given protocol message.
     *
     * @param message the protocol message to be handled
     */
    public abstract void handle(ProtocolMessage message);

    /**
     * Determines if this handler can handle messages of the specified type.
     *
     * @param messageType the type of message to check
     * @return true if this handler can handle the specified message type, false otherwise
     */
    public abstract boolean canHandle(MessageType messageType);
}
