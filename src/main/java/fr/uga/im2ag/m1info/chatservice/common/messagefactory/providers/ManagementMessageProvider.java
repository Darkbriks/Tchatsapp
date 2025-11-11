package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.util.Set;

public class ManagementMessageProvider implements MessageProvider {
    @Override
    public Set<MessageType> getType() {
        return Set.of(
                MessageType.CREATE_USER,
                MessageType.CONNECT_USER,
                MessageType.ADD_CONTACT,
                MessageType.REMOVE_CONTACT,
                MessageType.UPDATE_PSEUDO
        );
    }

    @Override
    public ProtocolMessage createInstance() {
        return new ManagementMessage();
    }
}
