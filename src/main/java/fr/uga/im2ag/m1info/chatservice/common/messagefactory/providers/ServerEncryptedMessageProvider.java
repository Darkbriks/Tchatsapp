package fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ServerEncryptedMessage;

import java.util.Set;

public class ServerEncryptedMessageProvider implements MessageProvider {

    @Override
    public Set<MessageType> getType() {
        return Set.of(MessageType.SERVER_ENCRYPTED);
    }

    @Override
    public ProtocolMessage createInstance() {
        return new ServerEncryptedMessage();
    }
}
