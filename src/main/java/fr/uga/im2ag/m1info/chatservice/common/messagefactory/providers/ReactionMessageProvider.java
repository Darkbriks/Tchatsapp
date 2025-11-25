package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ReactionMessage;

public class ReactionMessageProvider implements MessageProvider {
    @Override
    public Set<MessageType> getType() {
        return Set.of(MessageType.REACTION);
    }

    @Override
    public ProtocolMessage createInstance() {
        return new ReactionMessage();
    }

}
