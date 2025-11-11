package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

// TODO: Interact with repositories and notify observers
public class ManagementMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for UserManagementHandler");
        }

        switch (userMsg.getMessageType()) {
            case CREATE_USER -> createUser(userMsg);
            case CONNECT_USER -> connectUser(userMsg);
            case ADD_CONTACT -> addContact(userMsg);
            case REMOVE_CONTACT -> removeContact(userMsg);
            case UPDATE_PSEUDO -> updatePseudo(userMsg);
            default -> throw new IllegalArgumentException("Unsupported management message type");
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.CREATE_USER
                || messageType == MessageType.CONNECT_USER
                || messageType == MessageType.ADD_CONTACT
                || messageType == MessageType.REMOVE_CONTACT
                || messageType == MessageType.UPDATE_PSEUDO;
    }

    private void createUser(ManagementMessage message) {
        System.out.println("User created with ID: " + message.getParam("userId") + " and pseudo: " + message.getParam("pseudo"));
    }

    private void connectUser(ManagementMessage message) {
        System.out.println("User connected with ID: " + message.getParam("userId") + " and pseudo: " + message.getParam("pseudo"));
    }

    private void addContact(ManagementMessage message) {
        System.out.println("Contact added: " + message.getParam("contactPseudo") + " (ID: " + message.getParam("contactId") + ")");
    }

    private void removeContact(ManagementMessage message) {
        System.out.println("Contact removed: " + message.getParam("contactPseudo") + " (ID: " + message.getParam("contactId") + ")");
    }

    private void updatePseudo(ManagementMessage message) {
        if (message.getParamAsType("ack", Boolean.class) == null) {
            System.out.println("Contact pseudo updated: " + message.getParam("newPseudo") + " (ID: " + message.getParam("contactId") + ")");
        } else {
            System.out.println("Pseudo updated to: " + message.getParam("newPseudo"));
        }
    }
}
