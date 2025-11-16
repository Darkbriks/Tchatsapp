package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * Class representing a media message in the chat service protocol.
 */
public class MediaMessage extends ProtocolMessage {
    private byte[] content;
    private String mediaName;
    private String replyToMessageId;
    private int size;

    /** Default constructor for MediaMessage.
     * This is used for message factory registration only.
     */
    public MediaMessage() {
        super(MessageType.NONE, -1, -1);
        this.mediaName = "";
        this.replyToMessageId = null;
    }

    /** Get the media of the message.
     *
     * @return the media in byte representation
     */
    public byte[] getContent() {
        return content;
    }

    /** Get the ID of the message being replied to.
     *
     * @return the reply-to message ID, or null if not a reply
     */
    public String getReplyToMessageId() {
        return replyToMessageId;
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
    public void setContent(byte[] content) {
        this.content = content;
    }

    /** Set the size of the content.
     *
     * @param size the size
     */
    public void setSizeContent(int size){
        this.size = size;
    }

    /**
     * get The size of real data in payload
     *
     * @return  Size of real data in payload
     */
    public int getSizeContent() {
        return size;
    }

    /** Set the name of the media content of the message.
     *
     * @param name the media name to set
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
        StringBuilder sb = getStringBuilder();
        sb.append(messageId).append("|").append(timestamp.toEpochMilli()).append("|");
        if (replyToMessageId != null) {
            sb.append(replyToMessageId);
        }
        sb.append("|").append(mediaName);
        String encodedContent = Base64.getEncoder().encodeToString(Arrays.copyOfRange(content, 0, size));
        sb.append("|").append(encodedContent);
        byte[] payload = sb.toString().getBytes();
        int length = payload.length;
        return new Packet.PacketBuilder(length)
                .setMessageType(messageType)
                .setFrom(from)
                .setTo(to)
                .setPayload(payload)
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
        this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        this.replyToMessageId = parts[2].isEmpty() ? null : parts[2];
        this.mediaName = parts[3];
        this.content = Base64.getDecoder().decode(parts[4]);
        this.size = content.length;
        return this;
    }
}
