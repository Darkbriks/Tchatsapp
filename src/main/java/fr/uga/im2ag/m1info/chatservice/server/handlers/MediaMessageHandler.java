package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

public class MediaMessageHandler extends ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof MediaMessage mediaMsg)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageProcessor");
        }

        System.out.printf("[Server] Message Media reçu de %d à %d : nom du fichier = %s%n",
                mediaMsg.getFrom(), mediaMsg.getTo(), mediaMsg.getMediaName());

        serverContext.sendPacketToClient(message.toPacket());
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.MEDIA;
    }
}

