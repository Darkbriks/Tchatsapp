package fr.uga.im2ag.m1info.chatservice.server.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.server.handlers.RelayMessageHandler;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerPacketHandler;

import java.util.List;
import java.util.Set;

public class RelayMessageHandlerProvider implements ServerPacketHandlerProvider {
    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(MessageType.TEXT, MessageType.MEDIA);
    }

    @Override
    public List<ServerPacketHandler> createHandlers() {
        return List.of(new RelayMessageHandler());
    }
}