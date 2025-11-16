package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.time.Instant;

/**
 * Message representing a contact request or its response.
 */
public class ContactRequestMessage extends ProtocolMessage {
    private String requestId;
    private Instant timestamp;
    private boolean isResponse; // false for request, true for response
    private boolean accepted; // only used when isResponse = true
    private long expirationTimestamp; // only used when isResponse = false

    /**
     * Default constructor.
     */
    public ContactRequestMessage() {
        super(MessageType.CONTACT_REQUEST, -1, -1);
        this.timestamp = Instant.EPOCH;
        this.requestId = null;
        this.isResponse = false;
        this.accepted = false;
        this.expirationTimestamp = 0;
    }

    /**
     * Generate a new request ID using the provided MessageIdGenerator.
     *
     * @param messageIdGenerator the generator to use
     * @throws IllegalStateException if 'from' is not set
     */
    public void generateNewRequestId(MessageIdGenerator messageIdGenerator) {
        if (this.from == -1) {
            throw new IllegalStateException("Cannot generate request ID: 'from' field is not set.");
        }
        timestamp = Instant.now();
        requestId = messageIdGenerator.generateId(from, timestamp.toEpochMilli());
    }

    /**
     * Get the request ID.
     *
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Get the timestamp.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Check if this is a response message.
     *
     * @return true if response, false if request
     */
    public boolean isResponse() {
        return isResponse;
    }

    /**
     * Check if the request was accepted (only valid for responses).
     *
     * @return true if accepted
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Get the expiration timestamp (only valid for requests).
     *
     * @return expiration timestamp in epoch millis
     */
    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    /**
     * Set the request ID.
     *
     * @param requestId the request ID
     * @return this message for chaining
     */
    public ContactRequestMessage setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Set whether this is a response.
     *
     * @param isResponse true if response
     * @return this message for chaining
     */
    public ContactRequestMessage setResponse(boolean isResponse) {
        this.isResponse = isResponse;
        if (isResponse) {
            this.messageType = MessageType.CONTACT_REQUEST_RESPONSE;
        } else {
            this.messageType = MessageType.CONTACT_REQUEST;
        }
        return this;
    }

    /**
     * Set whether the request was accepted.
     *
     * @param accepted true if accepted
     * @return this message for chaining
     */
    public ContactRequestMessage setAccepted(boolean accepted) {
        this.accepted = accepted;
        return this;
    }

    /**
     * Set the expiration timestamp.
     *
     * @param expirationTimestamp expiration time in epoch millis
     * @return this message for chaining
     */
    public ContactRequestMessage setExpirationTimestamp(long expirationTimestamp) {
        this.expirationTimestamp = expirationTimestamp;
        return this;
    }

    @Override
    public Packet toPacket() {
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID is null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(requestId).append("|");
        sb.append(timestamp.toEpochMilli()).append("|");
        sb.append(isResponse ? "1" : "0").append("|");

        if (isResponse) {
            sb.append(accepted ? "1" : "0");
        } else {
            sb.append(expirationTimestamp);
        }

        byte[] payload = sb.toString().getBytes();
        return new Packet.PacketBuilder(payload.length)
                .setMessageType(messageType)
                .setFrom(from)
                .setTo(to)
                .setPayload(payload)
                .build();
    }

    @Override
    public ContactRequestMessage fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();

        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 4);

        this.requestId = parts[0];
        this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        this.isResponse = "1".equals(parts[2]);

        if (isResponse) {
            this.accepted = "1".equals(parts[3]);
        } else {
            this.expirationTimestamp = Long.parseLong(parts[3]);
        }

        return this;
    }
}