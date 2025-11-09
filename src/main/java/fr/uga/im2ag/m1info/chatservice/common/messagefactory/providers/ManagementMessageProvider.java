package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

public class ManagementMessageProvider implements MessageProvider {
    @Override
    public MessageType getType() {
        return MessageType.CREATE_USER; // TODO
    }

    @Override
    public ProtocolMessage createInstance() {
        return new ManagementMessage();
    }
}
