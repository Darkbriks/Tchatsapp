package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.AckMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.util.Set;

/**
 * Provider for creating AckMessage instances.
 */
public class AckMessageProvider implements MessageProvider {
    @Override
    public Set<MessageType> getType() {
        return Set.of(MessageType.MESSAGE_ACK);
    }

    @Override
    public ProtocolMessage createInstance() {
        return new AckMessage(0, 0);
    }
}