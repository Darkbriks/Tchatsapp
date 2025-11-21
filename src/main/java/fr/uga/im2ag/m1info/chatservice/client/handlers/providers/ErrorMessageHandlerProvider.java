package fr.uga.im2ag.m1info.chatservice.client.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.ErrorMessageHandler;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.util.List;
import java.util.Set;

public class ErrorMessageHandlerProvider implements ClientPacketHandlerProvider {
    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(MessageType.ERROR);
    }

    @Override
    public List<ClientPacketHandler> createHandlers() {
        return List.of(new ErrorMessageHandler());
    }
}