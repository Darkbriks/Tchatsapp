package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

/**
 * Handler for relaying messages from sender to recipient with acknowledgments.
 */
public class RelayMessageHandler extends ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        int from = message.getFrom();
        int to = message.getTo();

        // Validate sender is registered
        if (!serverContext.isClientRegistered(from)) {
            System.err.printf("[Server] Message from unregistered user %d, ignoring%n", from);
            AckHelper.sendFailedAck(serverContext, message, "Sender not registered");
            return;
        }

        // Validate recipient exists
        if (!serverContext.isClientRegistered(to)) {
            System.err.printf("[Server] Message to unregistered user %d, ignoring%n", to);
            AckHelper.sendFailedAck(serverContext, message, "Recipient not found");
            return;
        }

        // Validate sender and recipient are contacts
        if (!serverContext.getUserRepository().findById(from).getContacts().contains(to)) {
            System.err.printf("[Server] User %d tried to send message to non-contact %d%n", from, to);
            AckHelper.sendFailedAck(serverContext, message, "Recipient not in contacts");
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