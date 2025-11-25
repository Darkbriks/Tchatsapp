package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import java.time.Instant;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;

public class ReactionMessage extends AbstractSerializableMessage {

    private String content;
    private String reactionToMessageId;


    public ReactionMessage() {
        super(MessageType.NONE, -1, -1);
        this.timestamp = Instant.EPOCH;
        this.messageId = null;
        this.content = "";
        this.reactionToMessageId = null;
    }

    public String getContent() {
        return content;
    }

    public ReactionMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public String getReactionToMessageId() {
        return reactionToMessageId;
    }

    public ReactionMessage setReactionToMessageId(String reactionToMessageId) {
        this.reactionToMessageId = reactionToMessageId;
        return this;
    }

    @Override
    protected void serializeContent(StringBuilder sb) {
        joinFields(sb, reactionToMessageId, content);
    }

    @Override
    protected void deserializeContent(String[] parts, int startIndex) {
        this.reactionToMessageId = parts[startIndex];
        this.content = parts[startIndex + 1];
    }

    @Override
    protected int getExpectedPartCount() {
        return 4;
    }

    @Override
    public String toString() {
        return "ReactionMessage{" +
                "messageId='" + messageId + '\'' +
                ", from=" + from +
                ", to=" + to +
                ", content='" + (content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", replyTo=" + reactionToMessageId +
                '}';
    }

}
