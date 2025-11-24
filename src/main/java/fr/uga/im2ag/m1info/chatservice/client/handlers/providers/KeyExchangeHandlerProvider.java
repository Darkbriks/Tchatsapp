package fr.uga.im2ag.m1info.chatservice.client.handlers.providers;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.KeyExchangeHandler;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.util.List;
import java.util.Set;

/**
 * Provider for {@link KeyExchangeHandler}.
 * <p>
 * Registers handlers for KEY_EXCHANGE and KEY_EXCHANGE_RESPONSE message types.
 */
public class KeyExchangeHandlerProvider implements ClientPacketHandlerProvider {
    
    @Override
    public Set<MessageType> getHandledTypes() {
        return Set.of(
            MessageType.KEY_EXCHANGE,
            MessageType.KEY_EXCHANGE_RESPONSE
        );
    }
    
    @Override
    public List<ClientPacketHandler> createHandlers() {
        return List.of(new KeyExchangeHandler());
    }
}
