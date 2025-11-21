package fr.uga.im2ag.m1info.chatservice.client.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.util.List;
import java.util.Set;

/**
 * Provider interface for client packet handlers.
 */
public interface ClientPacketHandlerProvider {

    /**
     * Returns the set of message types that the handlers from this provider can handle.
     * Used for conflict detection during registration.
     *
     * @return set of handled message types
     */
    Set<MessageType> getHandledTypes();

    /**
     * Creates new instances of the handlers provided by this provider.
     * Each call should return fresh instances.
     *
     * @return list of handler instances
     */
    List<ClientPacketHandler> createHandlers();
}