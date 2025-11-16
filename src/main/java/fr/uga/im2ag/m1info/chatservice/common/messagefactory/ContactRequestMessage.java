package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.time.Instant;

/**
 * Message representing a contact request.
 */
public class ContactRequestMessage extends ProtocolMessage {
    private String requestId;
    private long expirationTimestamp; // only used when isResponse = false

    /**
     * Default constructor.
     */
    public ContactRequestMessage() {
        super(MessageType.CONTACT_REQUEST, -1, -1);
        this.requestId = null;
        this.expirationTimestamp = 0;
    }

    /**
     * Get the request ID.
     *
     * @return the request ID
     */
    public String getRequestId() {
        return requestId != null ? requestId : messageId;
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
        StringBuilder sb = getStringBuilder();
        sb.append(messageId).append("|");
        sb.append(timestamp.toEpochMilli()).append("|");
        sb.append(requestId != null ? requestId : messageId).append("|");
        sb.append(expirationTimestamp);

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

        this.messageId = parts[0];
        this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        this.requestId = parts[2];
        this.expirationTimestamp = Long.parseLong(parts[3]);

        return this;
    }
}