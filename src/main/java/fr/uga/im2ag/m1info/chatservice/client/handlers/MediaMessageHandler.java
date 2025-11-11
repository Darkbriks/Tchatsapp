package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientContext;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientContext context) {
        if (!(message instanceof MediaMessage mediaMessage)) {
            throw new IllegalArgumentException("Invalid message type for MediaMessageHandler");
        }

        try {
            File outputFile = new File(mediaMessage.getMediaName() + "_test_receive");
            try (FileOutputStream outputStream = new FileOutputStream(outputFile, true)) {
                outputStream.write(mediaMessage.getContent());
            }

            // TODO: Add the media to ConversationRepository and notify observers
            System.out.println("[Media received]");
            System.out.println("\tFrom: " + mediaMessage.getFrom());
            System.out.println("\tTo: " + mediaMessage.getTo());
            System.out.println("\tMedia name: " + mediaMessage.getMediaName());
            System.out.println("\tSize: " + mediaMessage.getSizeContent());
            System.out.println("\tSaved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Client] Failed to save media file: " + e.getMessage());
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.MEDIA;
    }
}