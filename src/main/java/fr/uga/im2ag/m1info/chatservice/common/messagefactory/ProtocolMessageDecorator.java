package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

/**
 * Abstract decorator class for ProtocolMessage.
 * This class implements the Decorator design pattern, allowing for dynamic
 * addition of responsibilities to ProtocolMessage objects.
 */
public abstract class ProtocolMessageDecorator extends ProtocolMessage {
    protected ProtocolMessage decoratedMessage;

    /** Constructor for ProtocolMessageDecorator.
     *
     * @param decoratedMessage the ProtocolMessage to be decorated
     */
    public ProtocolMessageDecorator(ProtocolMessage decoratedMessage) {
        super(decoratedMessage.getMessageType(), decoratedMessage.getFrom(), decoratedMessage.getTo());
        this.decoratedMessage = decoratedMessage;
    }

    @Override
    public Packet toPacket() {
        return decoratedMessage.toPacket();
    }

    @Override
    public ProtocolMessageDecorator fromPacket(Packet packet) {
        return (ProtocolMessageDecorator) decoratedMessage.fromPacket(packet);
    }

    @Override
    public MessageType getMessageType() {
        return decoratedMessage.getMessageType();
    }

    @Override
    public int getFrom() {
        return decoratedMessage.getFrom();
    }

    @Override
    public int getTo() {
        return decoratedMessage.getTo();
    }
}
