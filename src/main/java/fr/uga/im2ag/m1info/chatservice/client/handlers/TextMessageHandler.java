package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

public class TextMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message) {
        if (!(message instanceof TextMessage textMsg)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageHandler");
        }

        // TODO: Add the message to ConversationRepository and notify observers
        System.out.println("Received text message: " +
                "\n\tFrom: " + textMsg.getFrom() +
                "\n\tTo: " + textMsg.getTo() +
                "\n\tMessageId: " + textMsg.getMessageId() +
                "\n\tTimestamp: " + textMsg.getTimestamp() +
                "\n\tContent: " + textMsg.getContent());
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT;
    }
}
