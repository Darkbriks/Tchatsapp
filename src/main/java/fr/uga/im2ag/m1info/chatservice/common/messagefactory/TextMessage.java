package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.nio.ByteBuffer;

/**
 * Class representing a text message in the chat service protocol.
 */
public class TextMessage extends ProtocolMessage {
    /*static {
        MessageFactory.register(MessageType.TEXT, TextMessage::new);
    }*/

    private String messageId;
    private String content;
    private String replyToMessageId;
    private long timestamp;

    /** Default constructor for TextMessage.
     * This is used for message factory registration only.
     */
    public TextMessage() {
        super(MessageType.NONE, -1, -1);
        timestamp = System.currentTimeMillis();
        messageId = MessageIdGenerator.generateMessageId(from, timestamp);
        this.content = content;
        this.replyToMessageId = replyToMessageId;
    }

    /** Get the message ID.
     *
     * @return the message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /** Get the text content of the message.
     *
     * @return the text content
     */
    public String getContent() {
        return content;
    }

    /** Get the ID of the message being replied to.
     *
     * @return the reply-to message ID, or null if not a reply
     */
    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    /** Get the timestamp of the message.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /** Set the text content of the message.
     *
     * @param content the text content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /** Set the ID of the message being replied to.
     *
     * @param replyToMessageId the reply-to message ID to set
     */
    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    @Override
    public Packet toPacket() {
        StringBuilder sb = new StringBuilder();
        sb.append(messageId).append("|").append(timestamp).append("|");
        if (replyToMessageId != null) {
            sb.append(replyToMessageId);
        }
        sb.append("|").append(content);
        int length = sb.length();
        return new Packet.PacketBuilder(length)
                .setMessageType(messageType)
                .setFrom(from)
                .setTo(to)
                .setPayload(sb.toString().getBytes())
                .build();
    }

    @Override
    public void fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();
        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 4);
        this.messageId = parts[0];
        this.timestamp = Long.parseLong(parts[1]);
        this.replyToMessageId = parts[2].isEmpty() ? null : parts[2];
        this.content = parts[3];
    }
}
