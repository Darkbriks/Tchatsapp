package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.common.PacketProcessor;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * A ClientPaquetRouter that routes incoming protocol messages to the appropriate ClientPacketHandler.
 * This router maintains a ClientContext that is passed to all handlers.
 */
public class ClientPaquetRouter implements PacketProcessor {
    private final List<ClientPacketHandler> handlers;
    private final ClientController context;

    /**
     * Creates a ClientPaquetRouter with the specified list of handlers and context.
     *
     * @param handlers the list of ClientPacketHandler to be used by this router
     * @param context the client context to pass to handlers
     */
    public ClientPaquetRouter(List<ClientPacketHandler> handlers, ClientController context) {
        this.handlers = handlers;
        this.context = context;
    }

    /**
     * Creates a ClientPaquetRouter with an empty list of handlers and the given context.
     *
     * @param context the client context to pass to handlers
     */
    public ClientPaquetRouter(ClientController context) {
        this.handlers = new ArrayList<>();
        this.context = context;
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
                handler.handle(message, context);
                return;
            }
        }
        throw new IllegalArgumentException("No handler found for message type: " + message.getMessageType());
    }
}
