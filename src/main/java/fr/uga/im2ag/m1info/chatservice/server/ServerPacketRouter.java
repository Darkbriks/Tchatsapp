package fr.uga.im2ag.m1info.chatservice.server;

import fr.uga.im2ag.m1info.chatservice.common.PacketProcessor;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerHandlerContext;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerPacketHandler;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerPacketHandlerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerPacketRouter implements PacketProcessor {
    private final List<ServerPacketHandler> handlers;
    private final TchatsAppServer.ServerContext serverContext;

    public ServerPacketRouter(TchatsAppServer.ServerContext serverContext) {
        this.serverContext = serverContext;
        this.handlers = new ArrayList<>();
    }

    /**
     * Creates a ServerPacketRouter loading handlers via ServiceLoader.
     *
     * @param serverContext the server context
     * @param handlerContext the context for handler initialization
     * @return a new ServerPacketRouter with loaded handlers
     */
    public static ServerPacketRouter createWithServiceLoader(
            TchatsAppServer.ServerContext serverContext,
            ServerHandlerContext handlerContext) {
        ServerPacketRouter router = new ServerPacketRouter(serverContext);
        List<ServerPacketHandler> handlers = ServerPacketHandlerFactory.loadHandlers(handlerContext);
        for (ServerPacketHandler handler : handlers) {
            router.addHandler(handler);
        }
        return router;
    }

    public void addHandler(ServerPacketHandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(ServerPacketHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void process(ProtocolMessage message) {
        for (ServerPacketHandler handler : handlers) {
            if (handler.canHandle(message.getMessageType())) {
                handler.handle(message, serverContext);
                return;
            }
        }
        throw new RuntimeException("No handler found for message type: " + message.getMessageType());
    }
}
