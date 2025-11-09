package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

/**
 * Class representing an error message in the chat service protocol.
 * TODO: improve error message structure (enum for error level and type? multiple types for different categories of errors? ...)
 */
public class ErrorMessage extends ProtocolMessage {
    private String errorLevel;
    private String errorType;
    private String errorMessage;

    public ErrorMessage() {
        super(MessageType.ERROR, -1, -1);
        this.errorLevel = "null";
        this.errorType = "null";
        this.errorMessage = "null";
    }

    public String getErrorLevel() {
        return errorLevel;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorLevel(String errorLevel) {
        if (errorLevel == null || errorLevel.isEmpty()) { errorLevel = "null"; }
        this.errorLevel = errorLevel;
    }

    public void setErrorType(String errorType) {
        if (errorType == null || errorType.isEmpty()) { errorType = "null"; }
        this.errorType = errorType;
    }

    public void setErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) { errorMessage = "null"; }
        this.errorMessage = errorMessage;
    }

    @Override
    public Packet toPacket() {
        StringBuilder sb = new StringBuilder();
        sb.append(errorLevel).append('|').append(errorType).append('|').append(errorMessage);
        return new Packet.PacketBuilder(sb.length())
                .setMessageType(this.messageType)
                .setFrom(this.from)
                .setTo(this.to)
                .setPayload(sb.toString().getBytes())
                .build();
    }

    @Override
    public void fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();
        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 3);
        this.errorLevel = parts[0];
        this.errorType = parts[1];
        this.errorMessage = parts[2];
    }
}
