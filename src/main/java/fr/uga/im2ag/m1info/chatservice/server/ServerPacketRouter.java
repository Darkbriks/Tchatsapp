package fr.uga.im2ag.m1info.chatservice.server;

import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.PacketProcessor;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.handlers.ServerPacketHandler;

import java.util.List;

public class ServerPacketRouter implements PacketProcessor {
    private final List<ServerPacketHandler> handlers;
    private final TchatsAppServer.ServerContext serverContext;

    public ServerPacketRouter(TchatsAppServer.ServerContext serverContext) {
        this.serverContext = serverContext;
        this.handlers = new java.util.ArrayList<>();
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
                handler.handle(message, serverContext); return;
            }
        }
        throw new RuntimeException("No handler found for message type: " + message.getMessageType());
    }
}
