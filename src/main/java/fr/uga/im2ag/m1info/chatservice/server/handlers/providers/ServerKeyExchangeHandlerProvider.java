package fr.uga.im2ag.m1info.chatservice.server.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerKeyExchangeHandler;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerPacketHandler;

import java.util.List;
import java.util.Set;

public class ServerKeyExchangeHandlerProvider implements ServerPacketHandlerProvider {

    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(MessageType.SERVER_KEY_EXCHANGE_RESPONSE);
    }

    @Override
    public List<ServerPacketHandler> createHandlers() {
        return List.of(new ServerKeyExchangeHandler());
    }
}
