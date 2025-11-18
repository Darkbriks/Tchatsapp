package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

public class AckMessageHandler extends ValidatingServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (message.getTo() != 0) {
            if (!validateSenderRegistered(message, serverContext)) return;
            if (!validateRecipientExists(message, serverContext)) return;
            if (!checkContactRelationship(message.getFrom(), message.getTo(), serverContext)) {
                AckHelper.sendFailedAck(serverContext, message, "Not authorized");
                return;
            }

            serverContext.sendPacketToClient(message.toPacket());
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.MESSAGE_ACK;
    }
}
