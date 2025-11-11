package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

public class ErrorMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message) {
        if (!(message instanceof ErrorMessage)) {
            throw new IllegalArgumentException("Invalid message type for ErrorMessageHandler");
        }

        // TODO: Handle the error message appropriately (e.g., log it, notify the user, etc.)
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
