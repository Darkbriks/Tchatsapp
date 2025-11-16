package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.nio.charset.StandardCharsets;

/**
 * Message representing an acknowledgment for another message.
 * Used to track message delivery and read status.
 */
public class AckMessage extends ProtocolMessage {
    private String acknowledgedMessageId;
    private MessageStatus ackType;
    private String errorReason;

    /**
     * Constructor for AckMessage.
     *
     * @param from the sender ID
     * @param to the recipient ID
     */
    public AckMessage(int from, int to) {
        super(MessageType.MESSAGE_ACK, from, to);
        this.acknowledgedMessageId = "";
        this.ackType = MessageStatus.SENT;
        this.errorReason = null;
    }

    /**
     * Get the ID of the message being acknowledged.
     *
     * @return the acknowledged message ID
     */
    public String getAcknowledgedMessageId() {
        return acknowledgedMessageId;
    }

    /**
     * Set the ID of the message being acknowledged.
     *
     * @param acknowledgedMessageId the acknowledged message ID
     * @return this AckMessage for chaining
     */
    public AckMessage setAcknowledgedMessageId(String acknowledgedMessageId) {
        this.acknowledgedMessageId = acknowledgedMessageId;
        return this;
    }

    /**
     * Get the acknowledgment type.
     *
     * @return the acknowledgment type
     */
    public MessageStatus getAckType() {
        return ackType;
    }

    /**
     * Set the acknowledgment type.
     *
     * @param ackType the acknowledgment type
     * @return this AckMessage for chaining
     */
    public AckMessage setAckType(MessageStatus ackType) {
        this.ackType = ackType;
        return this;
    }

    /**
     * Get the error reason.
     *
     * @return the error reason, or null if no error
     */
    public String getErrorReason() {
        return errorReason;
    }

    /**
     * Set the error reason.
     *
     * @param errorReason the error reason
     * @return this AckMessage for chaining
     */
    public AckMessage setErrorReason(String errorReason) {
        this.errorReason = errorReason;
        return this;
    }

    @Override
    public Packet toPacket() {
        StringBuilder payload = new StringBuilder();
        payload.append(acknowledgedMessageId).append('|');
        payload.append(ackType.toByte()).append('|');
        if (ackType == MessageStatus.FAILED && errorReason != null) {
            payload.append(errorReason);
        }
        return new Packet.PacketBuilder(payload.length())
                .setMessageType(MessageType.MESSAGE_ACK)
                .setFrom(getFrom())
                .setTo(getTo())
                .setPayload(payload.toString().getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Override
    public AckMessage fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();
        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 3);
        this.acknowledgedMessageId = parts[0];
        this.ackType = MessageStatus.fromByte(Byte.parseByte(parts[1]));
        if (ackType == MessageStatus.FAILED && parts.length > 2) {
            this.errorReason = parts[2];
        } else {
            this.errorReason = null;
        }
        return this;
    }
}