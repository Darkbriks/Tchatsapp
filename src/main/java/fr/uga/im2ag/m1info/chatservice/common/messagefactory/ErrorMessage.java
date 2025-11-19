package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.time.Instant;

/**
 * Class representing an error message in the chat service protocol.
 */
// TODO: Delete this class to use Ack only
public class ErrorMessage extends ProtocolMessage {

    public enum ErrorLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL;

        @Override
        public String toString() {
            return name();
        }

        public static ErrorLevel fromString(String str) {
            for (ErrorLevel level : ErrorLevel.values()) {
                if (level.name().equalsIgnoreCase(str)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Unknown error level: " + str);
        }

        public int toInt() {
            return switch (this) {
                case INFO -> 0;
                case WARNING -> 1;
                case ERROR -> 2;
                case CRITICAL -> 3;
            };
        }

        public static ErrorLevel fromInt(int level) {
            return switch (level) {
                case 0 -> INFO;
                case 1 -> WARNING;
                case 2 -> ERROR;
                case 3 -> CRITICAL;
                default -> throw new IllegalStateException("Unexpected value: " + level);
            };
        }
    }

    private ErrorLevel errorLevel;
    private String errorType;
    private String errorMessage;

    public ErrorMessage() {
        super(MessageType.ERROR, -1, -1);
        this.errorLevel = ErrorLevel.INFO;
        this.errorType = "null";
        this.errorMessage = "null";
    }

    public ErrorLevel getErrorLevel() {
        return errorLevel;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ErrorMessage setErrorLevel(ErrorLevel errorLevel) {
        if (errorLevel == null) { errorLevel = ErrorLevel.INFO; }
        this.errorLevel = errorLevel;
        return this;
    }

    public ErrorMessage setErrorType(String errorType) {
        if (errorType == null || errorType.isEmpty()) { errorType = "null"; }
        this.errorType = errorType;
        return this;
    }

    public ErrorMessage setErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) { errorMessage = "null"; }
        this.errorMessage = errorMessage;
        return this;
    }

    @Override
    public Packet toPacket() {
        StringBuilder sb = getStringBuilder();
        sb.append(messageId).append("|").append(timestamp.toEpochMilli()).append("|");
        sb.append(errorLevel.toInt()).append('|').append(errorType).append('|').append(errorMessage);
        return new Packet.PacketBuilder(sb.length())
                .setMessageType(this.messageType)
                .setFrom(this.from)
                .setTo(this.to)
                .setPayload(sb.toString().getBytes())
                .build();
    }

    @Override
    public ErrorMessage fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();
        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 5);
        this.messageId = parts[0];
        this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        this.errorLevel = ErrorLevel.fromInt(Integer.parseInt(parts[2]));
        this.errorType = parts[3];
        this.errorMessage = parts[4];
        return this;
    }
}
