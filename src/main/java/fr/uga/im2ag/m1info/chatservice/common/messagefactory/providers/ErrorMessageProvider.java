package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.util.Set;

public class ErrorMessageProvider implements MessageProvider {
    @Override
    public Set<MessageType> getType() {
        return Set.of(MessageType.ERROR);
    }

    @Override
    public ProtocolMessage createInstance() {
        return new ErrorMessage();
    }
}
