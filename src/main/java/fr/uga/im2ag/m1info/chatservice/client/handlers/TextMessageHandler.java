package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.types.TextMessageReceivedEvent;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

public class TextMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (!(message instanceof TextMessage textMsg)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageHandler");
        }

        String conversationId;
        int otherUserId;

        if (textMsg.getFrom() == context.getClientId()) {
            otherUserId = textMsg.getTo();
        } else {
            otherUserId = textMsg.getFrom();
        }

        ConversationClient conversation = context.getOrCreatePrivateConversation(otherUserId);
        conversationId = conversation.getConversationId();

        Message msg = new Message(
                textMsg.getMessageId(),
                textMsg.getFrom(),
                textMsg.getTo(),
                textMsg.getContent(),
                textMsg.getTimestamp(),
                textMsg.getReplyToMessageId()
        );

        conversation.addMessage(msg);
        context.getConversationRepository().update(conversationId, conversation);

        publishEvent(new TextMessageReceivedEvent(this, conversationId, msg), context);

        context.sendAck(textMsg, MessageStatus.DELIVERED);
        context.sendAck(textMsg, MessageStatus.READ); // TODO: Remove this line when read receipts are implemented properly
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT;
    }
}
