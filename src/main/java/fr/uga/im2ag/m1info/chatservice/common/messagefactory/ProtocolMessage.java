package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.nio.ByteBuffer;

/**
 * Abstract class representing a protocol message in the chat service.
 */
public abstract class ProtocolMessage {
    protected MessageType messageType;
    protected int from;
    protected int to;

    public abstract Packet toPacket();
    public abstract ProtocolMessage fromPacket(Packet packet);

    /** Constructor for ProtocolMessage.
     *
     * @param messageType the type of the message
     * @param from the sender ID
     * @param to the recipient ID
     */
    public ProtocolMessage(MessageType messageType, int from, int to) {
        this.messageType = messageType;
        this.from = from;
        this.to = to;
    }

    /** Get the type of the message.
     *
     * @return the message type
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /** Get the sender ID of the message.
     *
     * @return the sender ID
     */
    public int getFrom() {
        return from;
    }

    /** Get the recipient ID of the message.
     *
     * @return the recipient ID
     */
    public int getTo() {
        return to;
    }

    /** Set the sender ID of the message.
     *
     * @param from the sender ID
     */
    void setFrom(int from) {
        this.from = from;
    }

    /** Set the recipient ID of the message.
     *
     * @param to the recipient ID
     */
    void setTo(int to) {
        this.to = to;
    }

    /** Set the type of the message.
     *
     * @param messageType the message type
     */
    void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
