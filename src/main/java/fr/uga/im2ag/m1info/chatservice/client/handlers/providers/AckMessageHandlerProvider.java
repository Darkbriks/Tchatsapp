package fr.uga.im2ag.m1info.chatservice.client.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.client.handlers.AckMessageHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.util.List;
import java.util.Set;

public class AckMessageHandlerProvider implements ClientPacketHandlerProvider {
    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(MessageType.MESSAGE_ACK);
    }

    @Override
    public List<ClientPacketHandler> createHandlers() {
        return List.of(new AckMessageHandler());
    }
}