package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

public class TextMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (!(message instanceof TextMessage textMsg)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageHandler");
        }

        // TODO: Add the message to ConversationRepository and notify observers
        System.out.println("[Message received]");
        System.out.println("\tFrom: " + textMsg.getFrom());
        System.out.println("\tTo: " + textMsg.getTo());
        System.out.println("\tMessageId: " + textMsg.getMessageId());
        System.out.println("\tTimestamp: " + textMsg.getTimestamp());
        if (textMsg.getReplyToMessageId() != null) {
            System.out.println("\tReply to: " + textMsg.getReplyToMessageId());
        }
        System.out.println("\tContent: " + textMsg.getContent());
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT;
    }
}
