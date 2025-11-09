package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

public class TextMessageProvider implements MessageProvider {
    @Override
    public MessageType getType() {
        return MessageType.TEXT;
    }

    @Override
    public ProtocolMessage createInstance() {
        return new TextMessage();
    }
}
