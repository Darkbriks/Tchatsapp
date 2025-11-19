package fr.uga.im2ag.m1info.chatservice.server.util;

import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.AckMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

import java.util.logging.Logger;

/**
 * Utility class for sending acknowledgment messages from the server to clients.
 */
public class AckHelper {
    private static final Logger LOGGER = Logger.getLogger(AckHelper.class.getName());

    /**
     * Send a SENT acknowledgment to the client who sent the message.
     * This indicates the server has received and processed the message.
     *
     * @param serverContext the server context
     * @param originalMessage the original message that was received
     */
    public static void sendSentAck(TchatsAppServer.ServerContext serverContext, ProtocolMessage originalMessage) {
        sendAck(serverContext, originalMessage, MessageStatus.SENT, null);
    }

    /**
     * Send a FAILED acknowledgment to the client.
     * This indicates the operation failed.
     *
     * @param serverContext the server context
     * @param originalMessage the original message that failed
     * @param errorReason the reason for failure
     */
    public static void sendFailedAck(TchatsAppServer.ServerContext serverContext, ProtocolMessage originalMessage, String errorReason) {
        sendAck(serverContext, originalMessage, MessageStatus.FAILED, errorReason);
    }

    /**
     * Send a FAILED acknowledgment to a specific client.
     *
     * @param serverContext the server context
     * @param clientId the client to send the ACK to
     * @param messageId the ID of the message that failed
     * @param errorReason the reason for failure
     */
    public static void sendFailedAck(TchatsAppServer.ServerContext serverContext, int clientId, String messageId, String errorReason) {
        AckMessage ack = (AckMessage) MessageFactory.create(MessageType.MESSAGE_ACK, 0, clientId);
        ack.setAcknowledgedMessageId(messageId);
        ack.setAckType(MessageStatus.FAILED);
        ack.setErrorReason(errorReason);

        serverContext.sendPacketToClient(ack.toPacket());
    }

    /**
     * Internal method to send an ACK message.
     *
     * @param serverContext the server context
     * @param originalMessage the original message being acknowledged
     * @param ackType the type of acknowledgment
     * @param errorReason the error reason (if FAILED)
     */
    private static void sendAck(TchatsAppServer.ServerContext serverContext, ProtocolMessage originalMessage, MessageStatus ackType, String errorReason) {
        if (originalMessage.getMessageId() == null || originalMessage.getMessageId().isEmpty()) {
            return;
        }

        AckMessage ack = (AckMessage) MessageFactory.create(MessageType.MESSAGE_ACK, 0, originalMessage.getFrom());
        ack.setAcknowledgedMessageId(originalMessage.getMessageId());
        ack.setAckType(ackType);

        if (errorReason != null) {
            ack.setErrorReason(errorReason);
        }

        serverContext.sendPacketToClient(ack.toPacket());

        LOGGER.info(String.format("Sent %s ACK for message %s to client %d",
                ackType,
                originalMessage.getMessageId(),
                originalMessage.getFrom()));
    }
}