package fr.uga.im2ag.m1info.chatservice.client.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.ContactRequestHandler;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.util.List;
import java.util.Set;

public class ContactRequestHandlerProvider implements ClientPacketHandlerProvider {
    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(MessageType.CONTACT_REQUEST, MessageType.CONTACT_REQUEST_RESPONSE);
    }

    @Override
    public List<ClientPacketHandler> createHandlers() {
        return List.of(new ContactRequestHandler());
    }
}