package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

public class MediaMessageProvider implements MessageProvider {
    @Override
    public Set<MessageType> getType() {
        return Set.of(MessageType.MEDIA);
    }

    @Override
    public ProtocolMessage createInstance() {
        return new MediaMessage();
    }
}

