package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

public class ErrorMessageHandler extends  ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof ErrorMessage)) {
            throw new IllegalArgumentException("Invalid message type for ErrorMessageHandler");
        }

        System.err.println("[Server] Received error message: " +
                "\n\tLevel: " + ((ErrorMessage) message).getErrorLevel() +
                "\n\tType: " + ((ErrorMessage) message).getErrorType() +
                "\n\tMessage: " + ((ErrorMessage) message).getErrorMessage());
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.ERROR;
    }
}
