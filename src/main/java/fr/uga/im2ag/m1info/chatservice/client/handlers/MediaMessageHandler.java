package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message) {
        if (!(message instanceof MediaMessage mediaMessage)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageHandler");
        }

        try {
            File outputFile = new File(mediaMessage.getMediaName() + "_test_receive");
            try (FileOutputStream outputStream = new FileOutputStream(outputFile, true)) {
                outputStream.write(mediaMessage.getContent());
            }
            System.out.println("Media message received and saved to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("exception occurred" + e);
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.MEDIA;
    }
}