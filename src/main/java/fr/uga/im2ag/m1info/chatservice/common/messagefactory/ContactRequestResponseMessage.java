package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.time.Instant;

/**
 * Message representing a contact response.
 */
public class ContactRequestResponseMessage extends ProtocolMessage {
    private String requestId;
    private boolean accepted;

    /**
     * Default constructor.
     */
    public ContactRequestResponseMessage() {
        super(MessageType.CONTACT_REQUEST, -1, -1);
        this.requestId = null;
        this.accepted = false;
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
     * Check if the request was accepted.
     *
     * @return true if accepted
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Set the request ID.
     *
     * @param requestId the request ID
     * @return this message for chaining
     */
    public ContactRequestResponseMessage setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Set whether the request was accepted.
     *
     * @param accepted true if accepted
     * @return this message for chaining
     */
    public ContactRequestResponseMessage setAccepted(boolean accepted) {
        this.accepted = accepted;
        return this;
    }

    @Override
    public Packet toPacket() {
        StringBuilder sb = new StringBuilder();
        sb.append(messageId).append("|");
        sb.append(timestamp.toEpochMilli()).append("|");
        sb.append(requestId != null ? requestId : messageId).append("|");
        sb.append(accepted ? "1" : "0");

        byte[] payload = sb.toString().getBytes();
        return new Packet.PacketBuilder(payload.length)
                .setMessageType(messageType)
                .setFrom(from)
                .setTo(to)
                .setPayload(payload)
                .build();
    }

    @Override
    public ContactRequestResponseMessage fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();

        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 4);

        this.messageId = parts[0];
        this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        this.requestId = parts[2];
        this.accepted = "1".equals(parts[3]);

        return this;
    }
}