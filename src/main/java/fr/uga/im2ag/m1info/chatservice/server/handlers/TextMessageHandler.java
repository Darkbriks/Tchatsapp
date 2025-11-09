package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

public class TextMessageHandler extends ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof TextMessage textMsg)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageProcessor");
        }

        int from = textMsg.getFrom();
        int to = textMsg.getTo();
        var userRepo = serverContext.getUserRepository();
        var fromUser = userRepo.findById(from);
        var toUser = userRepo.findById(to);
        if (fromUser == null) {
            System.out.printf("[Server] User %d not found while trying to send message to %d%n", from, to);
            return;
        }
        if (toUser == null) {
            System.out.printf("[Server] User %d tried to send message to non-existing user %d%n", from, to);
            return;
        }
        if (!fromUser.getContacts().contains(to)) {
            System.out.printf("[Server] User %d tried to send message to non-contact %d%n", from, to);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, to, from);
            errorMessage.setErrorLevel("WARNING");
            errorMessage.setErrorType("NOT_A_CONTACT");
            errorMessage.setErrorMessage("Cannot send message to user who is not in your contacts.");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            return;
        }

        serverContext.sendPacketToClient(message.toPacket());
        System.out.printf("[Server] Message from %d to %d forwarded%n", from, to);
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT;
    }
}
