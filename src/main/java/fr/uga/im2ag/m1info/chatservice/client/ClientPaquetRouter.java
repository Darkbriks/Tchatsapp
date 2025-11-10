package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.common.PacketProcessor;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * A ClientPaquetRouter that routes incoming protocol messages to the appropriate ClientPacketHandler.
 */
public class ClientPaquetRouter implements PacketProcessor {
    private List<ClientPacketHandler> handlers;

    /**
     * Creates a ClientPaquetRouter with the specified list of handlers.
     *
     * @param handlers the list of ClientPacketHandler to be used by this router
     */
    public ClientPaquetRouter(List<ClientPacketHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * Creates a ClientPaquetRouter with an empty list of handlers.
     */
    public ClientPaquetRouter() {
        this.handlers = new ArrayList<>();
    }

    /**
     * Adds a ClientPacketHandler to the router.
     *
     * @param handler the ClientPacketHandler to be added
     */
    public void addHandler(ClientPacketHandler handler) {
        this.handlers.add(handler);
    }

    /**
     * Removes a ClientPacketHandler from the router.
     *
     * @param handler the ClientPacketHandler to be removed
     */
    public void removeHandler(ClientPacketHandler handler) {
        this.handlers.remove(handler);
    }

    @Override
    public void process(ProtocolMessage message) {
        for (ClientPacketHandler handler : handlers) {
            if (handler.canHandle(message.getMessageType())) {
                handler.handle(message);
                return;
            }
        }
        throw new IllegalArgumentException("No handler found for message type: " + message.getMessageType());
    }
}
