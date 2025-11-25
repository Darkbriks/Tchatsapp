package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;
import fr.uga.im2ag.m1info.chatservice.client.event.types.MediaMessageReceivedEvent;
import fr.uga.im2ag.m1info.chatservice.client.event.types.ReactionMessageReceivedEvent;
import fr.uga.im2ag.m1info.chatservice.client.event.types.TextMessageReceivedEvent;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ReactionMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

public class ConversationMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        int recipientId = message.getTo();
        ConversationClient conversation;
        if (recipientId == context.getClientId()) {
            conversation = context.getOrCreatePrivateConversation(message.getFrom());
        } else if (context.getGroupRepository().findById(recipientId) != null) {
            // TODO: Find a way to know list of group members to create group conversation properly
            conversation = context.getOrCreateGroupConversation(recipientId, null);
        } else {
            throw new IllegalArgumentException("Message recipient not recognized by ConversationMessageHandler");
        }

        String conversationId = conversation.getConversationId();

        Message msg;
        Event event;
        if (message instanceof TextMessage textMsg) {
            msg = new Message(
                    textMsg.getMessageId(),
                    textMsg.getFrom(),
                    textMsg.getTo(),
                    textMsg.getContent(),
                    textMsg.getTimestamp(),
                    textMsg.getReplyToMessageId()
            );
            event = new TextMessageReceivedEvent(this, conversationId, msg);
        } else if (message instanceof MediaMessage mediaMsg) {
            msg = new Message(
                    mediaMsg.getMessageId(),
                    mediaMsg.getFrom(),
                    mediaMsg.getTo(),
                    "[Media: " + mediaMsg.getMediaName() + "]",
                    mediaMsg.getTimestamp(),
                    mediaMsg.getReplyToMessageId()
            );
            event = new MediaMessageReceivedEvent(this, conversationId, msg);
        } else if (message instanceof ReactionMessage reactionMsg) {
            msg = new Message(
                    reactionMsg.getMessageId(),
                    reactionMsg.getFrom(),
                    reactionMsg.getTo(),
                    "[Reaction: " + reactionMsg.getContent() + "]",
                    reactionMsg.getTimestamp(),
                    reactionMsg.getReactionToMessageId()
            );
            event = new ReactionMessageReceivedEvent(this, conversationId, msg);

        } else {
            throw new IllegalArgumentException("Unsupported message type for ConversationMessageHandler");
        }

        conversation.addMessage(msg);
        context.getConversationRepository().update(conversationId, conversation);
        publishEvent(event, context);

        context.sendAck(message, MessageStatus.DELIVERED);
        context.sendAck(message, MessageStatus.READ); // TODO: Remove this line when read receipts are implemented properly
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT || messageType == MessageType.MEDIA || messageType == MessageType.REACTION;
    }
}
