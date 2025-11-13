package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.types.MediaMessageReceivedEvent;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (!(message instanceof MediaMessage mediaMessage)) {
            throw new IllegalArgumentException("Invalid message type for MediaMessageHandler");
        }

        String conversationId;
        int otherUserId;

        if (mediaMessage.getFrom() == context.getClientId()) {
            otherUserId = mediaMessage.getTo();
        } else {
            otherUserId = mediaMessage.getFrom();
        }

        ConversationClient conversation = context.getOrCreatePrivateConversation(otherUserId);
        conversationId = conversation.getConversationId();

        // TODO: Implement media storage and Media proxy pattern
        try {
            File outputFile = new File(mediaMessage.getMediaName() + "_test_receive");
            try (FileOutputStream outputStream = new FileOutputStream(outputFile, true)) {
                outputStream.write(mediaMessage.getContent());
            }

            System.out.println("[Media received]");
            System.out.println("\tFrom: " + mediaMessage.getFrom());
            System.out.println("\tTo: " + mediaMessage.getTo());
            System.out.println("\tMedia name: " + mediaMessage.getMediaName());
            System.out.println("\tSize: " + mediaMessage.getSizeContent());
            System.out.println("\tSaved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Client] Failed to save media file: " + e.getMessage());
        }

        // TODO: Set attached media using Media proxy pattern
        Message msg = new Message(
                mediaMessage.getMessageId(),
                mediaMessage.getFrom(),
                mediaMessage.getTo(),
                "[Media: " + mediaMessage.getMediaName() + "]",
                mediaMessage.getTimestamp(),
                mediaMessage.getReplyToMessageId()
        );

        conversation.addMessage(msg);
        context.getConversationRepository().update(conversationId, conversation);

        publishEvent(new MediaMessageReceivedEvent(this, conversationId, msg), context);
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.MEDIA;
    }
}