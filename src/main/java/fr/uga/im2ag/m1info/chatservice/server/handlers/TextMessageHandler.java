package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

public class TextMessageHandler extends ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof TextMessage textMsg)) {
            throw new IllegalArgumentException("Invalid message type for TextMessageProcessor");
        }

        System.out.printf("[Server] Message reçu de %d à %d : %s%n",
                textMsg.getFrom(), textMsg.getTo(), textMsg.getContent());

        serverContext.sendPacketToClient(message.toPacket());
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT;
    }
}
