package fr.uga.im2ag.m1info.chatservice.server.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerPacketHandler;
import fr.uga.im2ag.m1info.chatservice.server.handlers.UserManagementMessageHandler;

import java.util.List;
import java.util.Set;

public class UserManagementMessageHandlerProvider implements ServerPacketHandlerProvider {
    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(
                MessageType.CREATE_USER,
                MessageType.CONNECT_USER,
                MessageType.REMOVE_CONTACT,
                MessageType.UPDATE_PSEUDO
        );
    }

    @Override
    public List<ServerPacketHandler> createHandlers() {
        return List.of(new UserManagementMessageHandler());
    }
}