package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.command.PendingCommandManager;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.AckMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

/**
 * Handler for processing acknowledgment messages from the server.
 */
public class AckMessageHandler extends ClientPacketHandler {
    private final PendingCommandManager commandManager;

    /**
     * Constructor for AckMessageHandler.
     *
     * @param commandManager the pending command manager
     */
    public AckMessageHandler(PendingCommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (!(message instanceof AckMessage ackMsg)) {
            throw new IllegalArgumentException("Invalid message type for AckMessageHandler");
        }

        commandManager.handleAck(ackMsg);
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.MESSAGE_ACK;
    }
}