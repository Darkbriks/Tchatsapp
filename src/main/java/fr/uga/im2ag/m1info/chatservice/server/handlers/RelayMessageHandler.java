package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
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

        if (message.getMessageType() == MessageType.KEY_EXCHANGE ||
            message.getMessageType() == MessageType.KEY_EXCHANGE_RESPONSE) {
            sendPacketToRecipient(message, serverContext);
            return;
        }

        if (isGroupId(message.getTo(), serverContext)) {
            if (!validateSenderMemberOfGroup(message, serverContext)) return;
        } else {
            if (!validateRecipientExists(message, serverContext)) return;
            if (!checkContactRelationship(message.getFrom(), message.getTo(), serverContext)) {
                AckHelper.sendFailedAck(serverContext, message, "Not authorized");
                return;
            }
        }

        // Send SENT acknowledgment to sender
        AckHelper.sendSentAck(serverContext, message);

        // Relay message to recipient
        sendPacketToRecipient(message, serverContext);
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.TEXT ||
                messageType == MessageType.MEDIA ||
                messageType == MessageType.REACTION ||
                messageType == MessageType.ENCRYPTED ||
                messageType == MessageType.KEY_EXCHANGE ||
                messageType == MessageType.KEY_EXCHANGE_RESPONSE;
    }
}