package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.types.ErrorEvent;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

public class ErrorMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (!(message instanceof ErrorMessage errorMsg)) {
            throw new IllegalArgumentException("Invalid message type for ErrorMessageHandler");
        }

        // Store error in context for potential retrieval
        context.setLastError(errorMsg.getErrorMessage());

        publishEvent(new ErrorEvent(
                this,
                errorMsg.getErrorLevel(),
                errorMsg.getErrorType(),
                errorMsg.getErrorMessage()
        ), context);

        // If the error is critical and connection is not yet established, disconnect
        if (errorMsg.getErrorLevel() == ErrorMessage.ErrorLevel.CRITICAL && !context.isConnectionEstablished()) {
            System.err.println("[Client] Critical error during connection. Disconnecting...");
            context.disconnect();
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.ERROR;
    }
}
