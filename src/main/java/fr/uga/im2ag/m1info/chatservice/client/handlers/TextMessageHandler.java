package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.types.TextMessageReceivedEvent;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

import java.time.Instant;

public class TextMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (!(message instanceof TextMessage textMsg)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageHandler");
        }

        // TODO: Remove hardcoded conversation ID
        ConversationClient conversation = context.getConversationRepository().findById("0");
        if (conversation != null) {
            Message msg = new Message(
                    textMsg.getMessageId(),
                    textMsg.getFrom(),
                    textMsg.getTo(),
                    textMsg.getContent(),
                    Instant.ofEpochMilli(textMsg.getTimestamp()),
                    textMsg.getReplyToMessageId()
            );
            conversation.addMessage(msg);

            publishEvent(new TextMessageReceivedEvent(this, "0", msg), context);
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT;
    }
}
