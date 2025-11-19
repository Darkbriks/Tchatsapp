package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

/**
 * Handler for relaying messages from sender to recipient with acknowledgments.
 */
public class RelayMessageHandler extends ValidatingServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!validateSenderRegistered(message, serverContext)) return;
        if (!validateRecipientExists(message, serverContext)) return;
        if (!checkContactRelationship(message.getFrom(), message.getTo(), serverContext)) {
            AckHelper.sendFailedAck(serverContext, message, "Not authorized");
            return;
        }

        // Send SENT acknowledgment to sender
        AckHelper.sendSentAck(serverContext, message);

        // Relay message to recipient
        Packet packet = message.toPacket();
        serverContext.sendPacketToClient(packet);
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT || messageType == MessageType.MEDIA;
    }
}