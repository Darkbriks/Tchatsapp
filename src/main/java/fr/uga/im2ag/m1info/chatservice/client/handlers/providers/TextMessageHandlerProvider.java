package fr.uga.im2ag.m1info.chatservice.client.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.TextMessageHandler;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.util.List;
import java.util.Set;

public class TextMessageHandlerProvider implements ClientPacketHandlerProvider {
    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(MessageType.TEXT);
    }

    @Override
    public List<ClientPacketHandler> createHandlers() {
        return List.of(new TextMessageHandler());
    }
}