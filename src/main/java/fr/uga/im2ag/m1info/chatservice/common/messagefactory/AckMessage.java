package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

/**
 * Message representing an acknowledgment for another message.
 * Used to track message delivery and read status.
 */
public class AckMessage extends AbstractSerializableMessage {
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

    // ========================= Getters/Setters =========================

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

    // ========================= Serialization Methods =========================

    @Override
    protected void serializeContent(StringBuilder sb) {
        joinFields(sb,
                acknowledgedMessageId,
                Byte.toString(ackType.toByte()),
                ackType == MessageStatus.FAILED && errorReason != null ? errorReason : ""
        );
    }

    @Override
    protected void deserializeContent(String[] parts, int startIndex) {
        this.acknowledgedMessageId = parts[startIndex];
        this.ackType = MessageStatus.fromByte(Byte.parseByte(parts[startIndex + 1]));
        if (ackType == MessageStatus.FAILED && parts.length > startIndex + 2) {
            this.errorReason = parts[startIndex + 2];
        } else {
            this.errorReason = null;
        }
    }

    @Override
    protected int getExpectedPartCount() {
        return 5;
    }
}