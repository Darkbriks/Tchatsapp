package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;

/**
 * Handler for user management messages such as adding or removing contacts.
 * TODO: Discuss if we keep handlers like this one, with multiple message types,
 * or if we create one handler per message type for clarity (but more classes).
 */
public class UserManagementMessageHandler extends ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for UserManagementHandler");
        }

        switch (userMsg.getMessageType()) {
            case CREATE_USER -> createUser(serverContext, userMsg);
            case CONNECT_USER -> connectUser(serverContext, userMsg);
            case ADD_CONTACT -> addContact(serverContext, userMsg);
            case REMOVE_CONTACT -> removeContact(serverContext, userMsg);
            case UPDATE_PSEUDO -> updatePseudo(serverContext, userMsg);
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

    /**
     * Handles user creation (registration).
     * Creates a new user account with a generated ID and the provided pseudo.
     *
     * @param serverContext the server context
     * @param managementMessage the management message containing the CREATE_USER request
     */
    private static void createUser(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        TchatsAppServer.ConnectionState state = serverContext.getCurrentConnectionState();
        if (state == null) {
            System.err.println("[Server] CREATE_USER called without connection state");
            return;
        }

        int newClientId = serverContext.generateClientId();

        String pseudo = managementMessage.getParamAsType("pseudo", String.class);
        if (pseudo == null || pseudo.isEmpty()) {
            pseudo = "User" + newClientId;
        }

        UserInfo newUser = new UserInfo(newClientId, pseudo);
        serverContext.getUserRepository().add(newUser);

        if (!serverContext.registerConnection(state, newClientId)) {
            System.err.println("[Server] Failed to register connection for new user " + newClientId);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, 0, 0);
            errorMessage.setErrorLevel("ERROR");
            errorMessage.setErrorType("CONNECTION_FAILED");
            errorMessage.setErrorMessage("Failed to register connection");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            serverContext.closeConnection(state);
            return;
        }

        ManagementMessage response = (ManagementMessage) MessageFactory.create(MessageType.CREATE_USER, 0, newClientId);
        response.addParam("clientId", newClientId);
        response.addParam("pseudo", pseudo);
        serverContext.sendPacketToClient(response.toPacket());

        System.out.printf("[Server] Created new user: id=%d, pseudo=%s%n", newClientId, pseudo);
    }

    /**
     * Handles user connection (login).
     * Validates the client ID exists and is not already connected.
     *
     * @param serverContext the server context
     * @param managementMessage the management message containing the CONNECT_USER request
     */
    private static void connectUser(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        TchatsAppServer.ConnectionState state = serverContext.getCurrentConnectionState();
        if (state == null) {
            System.err.println("[Server] CONNECT_USER called without connection state");
            return;
        }

        int clientId = managementMessage.getFrom();

        // Check if client is registered
        if (!serverContext.isClientRegistered(clientId)) {
            System.out.printf("[Server] Client %d tried to connect but is not registered%n", clientId);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, 0, clientId);
            errorMessage.setErrorLevel("ERROR");
            errorMessage.setErrorType("USER_NOT_FOUND");
            errorMessage.setErrorMessage("Client ID not registered. Please create an account first.");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            serverContext.closeConnection(state);
            return;
        }

        // Check if already connected
        if (serverContext.isClientConnected(clientId)) {
            System.out.printf("[Server] Client %d is already connected%n", clientId);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, 0, clientId);
            errorMessage.setErrorLevel("ERROR");
            errorMessage.setErrorType("ALREADY_CONNECTED");
            errorMessage.setErrorMessage("This account is already connected from another location.");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            serverContext.closeConnection(state);
            return;
        }

        // Register the connection
        if (!serverContext.registerConnection(state, clientId)) {
            System.err.println("[Server] Failed to register connection for user " + clientId);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, 0, clientId);
            errorMessage.setErrorLevel("ERROR");
            errorMessage.setErrorType("CONNECTION_FAILED");
            errorMessage.setErrorMessage("Failed to register connection");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            serverContext.closeConnection(state);
            return;
        }

        // Update last login
        UserInfo user = serverContext.getUserRepository().findById(clientId);
        if (user != null) {
            user.updateLastLogin();
            serverContext.getUserRepository().update(clientId, user);
        }

        // Send success response
        ManagementMessage response = (ManagementMessage) MessageFactory.create(MessageType.CONNECT_USER, 0, clientId);
        response.addParam("clientId", clientId);
        if (user != null) {
            response.addParam("pseudo", user.getUsername());
        }
        serverContext.sendPacketToClient(response.toPacket());

        System.out.printf("[Server] Client %d connected successfully%n", clientId);
    }

    /**
     * Handles adding a contact for a user.
     * TODO: Discuss if both users should add each other as contacts, or if one-sided is enough
     * For now and for simplicity, both users must add each other manually
     *
     * @param serverContext    the server context
     * @param managementMessage the management message containing the add contact request
     */
    private static void addContact(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        int from = managementMessage.getFrom();
        int contactId = managementMessage.getParamAsType("contactId", Integer.class);

        UserInfo user = serverContext.getUserRepository().findById(from);
        if (user == null) {
            System.out.printf("[Server] User %d not found while trying to add contact %d%n", from, contactId);
            return;
        }

        if (serverContext.getUserRepository().findById(contactId) == null) {
            System.out.printf("[Server] User %d tried to add non-existing contact %d%n", from, contactId);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, 0, from);
            errorMessage.setErrorLevel("WARNING");
            errorMessage.setErrorType("CONTACT_NOT_EXISTING");
            errorMessage.setErrorMessage("Cannot add non-existing user as contact.");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            return;
        }

        user.addContact(contactId);
        serverContext.getUserRepository().update(user.getId(), user);
        System.out.printf("[Server] User %d added contact %d%n", from, contactId);

        UserInfo contactUser = serverContext.getUserRepository().findById(contactId);
        if (contactUser != null && serverContext.isClientConnected(contactId)) {
            ManagementMessage notifyMsg = (ManagementMessage) MessageFactory.create(MessageType.ADD_CONTACT, from, contactId);
            notifyMsg.addParam("contactId", from);
            notifyMsg.addParam("contactPseudo", user.getUsername());
            serverContext.sendPacketToClient(notifyMsg.toPacket());
        }
    }

    /**
     * Handles removing a contact for a user.
     * TODO: Discuss if both users should remove each other as contacts, or if one-sided is enough
     * For now and for simplicity, both users must remove each other manually
     *
     * @param serverContext    the server context
     * @param managementMessage the management message containing the remove contact request
     */
    private static void removeContact(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        int from = managementMessage.getFrom();
        int contactId = managementMessage.getParamAsType("contactId", Integer.class);

        UserInfo user = serverContext.getUserRepository().findById(from);
        if (user == null) {
            System.out.printf("[Server] User %d not found while trying to remove contact %d%n", from, contactId);
            return;
        }

        if (!user.getContacts().contains(contactId)) {
            System.out.printf("[Server] User %d tried to remove non-existing contact %d%n", from, contactId);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, 0, from);
            errorMessage.setErrorLevel("WARNING");
            errorMessage.setErrorType("CONTACT_NOT_FOUND");
            errorMessage.setErrorMessage("Cannot remove contact who is not in your contacts.");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            return;
        }

        user.removeContact(contactId);
        serverContext.getUserRepository().update(user.getId(), user);
        System.out.printf("[Server] User %d removed contact %d%n", from, contactId);
    }

    /**
     * Handles updating a user's pseudo (username).
     *
     * @param serverContext    the server context
     * @param managementMessage the management message containing the update pseudo request
     */
    private static void updatePseudo(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        int from = managementMessage.getFrom();
        String newPseudo = managementMessage.getParamAsType("newPseudo", String.class);

        UserInfo user = serverContext.getUserRepository().findById(from);
        if (user == null) {
            System.out.printf("[Server] User %d not found while trying to update pseudo%n", from);
            return;
        }

        if (newPseudo == null || newPseudo.isEmpty()) {
            System.out.printf("[Server] User %d provided invalid new pseudo%n", from);
            ErrorMessage errorMessage = (ErrorMessage) MessageFactory.create(MessageType.ERROR, 0, from);
            errorMessage.setErrorLevel("WARNING");
            errorMessage.setErrorType("INVALID_PSEUDO");
            errorMessage.setErrorMessage("The new pseudo cannot be null or empty.");
            serverContext.sendPacketToClient(errorMessage.toPacket());
            return;
        }

        user.setUsername(newPseudo);
        serverContext.getUserRepository().update(user.getId(), user);
        System.out.printf("[Server] User %d updated pseudo to %s%n", from, newPseudo);

        for (int contactId : user.getContacts()) {
            if (serverContext.isClientConnected(contactId)) {
                ManagementMessage notifyMsg = (ManagementMessage) MessageFactory.create(MessageType.UPDATE_PSEUDO, from, contactId);
                notifyMsg.addParam("contactId", from);
                notifyMsg.addParam("newPseudo", newPseudo);
                serverContext.sendPacketToClient(notifyMsg.toPacket());
            }
        }

        // TODO: Send confirmation to the user (ACK message) ?
        // TODO: Maybe a ACK Message type can be useful in several places
        // TODO: Chained methods for ProtocolMessage building would be nice
        // TODO: Simplify ErrorMessage creation (static factory methods?)
    }
}
