package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing a media message in the chat service protocol.
 */
public class MediaMessage extends ProtocolMessage {
    private String messageId;
    private List<Byte> content;
    private String mediaName;
    private String replyToMessageId;
    private long timestamp;

    /** Default constructor for MediaMessage.
     * This is used for message factory registration only.
     */
    public MediaMessage() {
        super(MessageType.NONE, -1, -1);
        timestamp = 0;
        messageId = null;
        this.mediaName = "";
        this.content = new ArrayList<Byte>();;
        this.replyToMessageId = null;
    }

    /** Generate a new unique message ID using the provided MessageIdGenerator.
     *
     * @param messageIdGenerator the MessageIdGenerator to use for generating the ID
     * @throws IllegalStateException if the 'from' field is not set
     */
    public void generateNewMessageId(MessageIdGenerator messageIdGenerator) {
        if (this.from == -1) { throw new IllegalStateException("Cannot generate message ID: 'from' field is not set."); }
        timestamp = System.currentTimeMillis();
        messageId = messageIdGenerator.generateId(from, timestamp);
    }

    /** Get the message ID.
     *
     * @return the message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /** Get the media of the message.
     *
     * @return the media in byte representation
     */
    public List<Byte> getContent() {
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
    
    /** Get the name of the media of the message.
     *
     * @return the name of the media send in this packet 
     */
    public String getMediaName() {
        return mediaName;
    }

    /** Set the media content of the message.
     *
     * @param content the media content to set
     */
    public void setContent(List<Byte> content) {
        this.content = content;
    }
    
    /** Set the name of the media content of the message.
     *
     * @param mediaName the media name to set
     */
    public void setMediaName(String name) {
        this.mediaName = name;
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
        if (messageId == null) { throw  new IllegalArgumentException("Message id is null"); }
        StringBuilder sb = new StringBuilder();
        sb.append(messageId).append("|").append(timestamp).append("|");
        if (replyToMessageId != null) {
            sb.append(replyToMessageId);
        }
        sb.append("|").append(mediaName);
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
    public MediaMessage fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();
        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 5);
        this.messageId = parts[0];
        this.timestamp = Long.parseLong(parts[1]);
        this.replyToMessageId = parts[2].isEmpty() ? null : parts[2];
        this.mediaName = parts[3];
//        this.content = Arrays.asList(parts[4].getBytes());
        return this;
    }
}

