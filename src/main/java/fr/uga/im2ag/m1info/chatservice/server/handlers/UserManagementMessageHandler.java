package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

/**
 * Handler for user management messages such as user creation, connection, and contact management.
 */
public class UserManagementMessageHandler extends ValidatingServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for UserManagementHandler");
        }

        switch (userMsg.getMessageType()) {
            case CREATE_USER -> createUser(serverContext, userMsg);
            case CONNECT_USER -> connectUser(serverContext, userMsg);
            case REMOVE_CONTACT -> removeContact(serverContext, userMsg);
            case UPDATE_PSEUDO -> updatePseudo(serverContext, userMsg);
            default -> throw new IllegalArgumentException("Unsupported management message type");
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.CREATE_USER
                || messageType == MessageType.CONNECT_USER
                || messageType == MessageType.REMOVE_CONTACT
                || messageType == MessageType.UPDATE_PSEUDO;
    }

    private void connectionFailed(TchatsAppServer.ServerContext serverContext, TchatsAppServer.ConnectionState state, int clientId, String reason) {
        ServerPacketHandler.LOG.warning("Connection failed for client " + clientId + ": " + reason);
        AckHelper.sendCriticalAck(serverContext, "-1", clientId, reason);
        serverContext.closeConnection(state);
    }

    /**
     * Handles user creation (registration).
     * Creates a new user account with a generated ID and the provided pseudo.
     *
     * @param serverContext the server context
     * @param managementMessage the management message containing the CREATE_USER request
     */
    private void createUser(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        TchatsAppServer.ConnectionState state = serverContext.getCurrentConnectionState();
        if (state == null) {
            ServerPacketHandler.LOG.warning("CREATE_USER called without connection state");
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
            connectionFailed(serverContext, state, newClientId, "Failed to register connection");
            return;
        }
        
        ManagementMessage response = ((ManagementMessage) MessageFactory.create(MessageType.ACK_CONNECTION, 0, newClientId))
                .addParam("clientId", newClientId)
                .addParam("pseudo", pseudo)
                .addParam("newUser", true);
        serverContext.sendPacketToClient(response.toPacket());

        ServerPacketHandler.LOG.info(String.format("Created new user: id=%d, pseudo=%s", newClientId, pseudo));
    }

    /**
     * Handles user connection (login).
     * Validates the client ID exists and is not already connected.
     *
     * @param serverContext the server context
     * @param managementMessage the management message containing the CONNECT_USER request
     */
    private void connectUser(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        TchatsAppServer.ConnectionState state = serverContext.getCurrentConnectionState();
        if (state == null) {
            ServerPacketHandler.LOG.warning("CONNECT_USER called without connection state");
            return;
        }

        int clientId = managementMessage.getFrom();

        if (!validateSenderRegistered(managementMessage, serverContext)) { return; }

        // Check if already connected
        if (serverContext.isClientConnected(clientId)) {
            connectionFailed(serverContext, state, clientId, "Client is already connected");
            return;
        }

        // Register the connection
        if (!serverContext.registerConnection(state, clientId)) {
            connectionFailed(serverContext, state, clientId, "Failed to register connection");
            return;
        }

        // Update last login
        UserInfo user = serverContext.getUserRepository().findById(clientId);
        if (user != null) {
            user.updateLastLogin();
            serverContext.getUserRepository().update(clientId, user);
        }

        ManagementMessage response = (ManagementMessage) MessageFactory.create(MessageType.ACK_CONNECTION, 0, clientId);
        response.addParam("clientId", clientId);
        if (user != null) {
            response.addParam("pseudo", user.getUsername());
        }
        response.addParam("newUser", false);
        serverContext.sendPacketToClient(response.toPacket());

        ServerPacketHandler.LOG.info("Client " + clientId + " connected successfully");
    }

    /**
     * Handles removing a contact for a user.
     *
     * @param serverContext    the server context
     * @param managementMessage the management message containing the remove contact request
     */
    private void removeContact(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        int from = managementMessage.getFrom();
        int contactId = managementMessage.getParamAsType("contactId", Integer.class);

        if (!validateSenderRegistered(managementMessage, serverContext)) { return; }
        if (!checkContactRelationship(from, contactId, serverContext)) {
            ServerPacketHandler.LOG.warning(String.format("User %d tried to remove non-existing contact %d", from, contactId));
            AckHelper.sendFailedAck(serverContext, managementMessage, "Contact not found");
            return;
        }

        try {
            UserInfo user = serverContext.getUserRepository().findById(from);
            UserInfo contact = serverContext.getUserRepository().findById(contactId);

            user.removeContact(contactId);
            serverContext.getUserRepository().update(user.getId(), user);

            contact.removeContact(from);
            serverContext.getUserRepository().update(contact.getId(), contact);

            ServerPacketHandler.LOG.info(String.format("User %d removed contact %d", from, contactId));

            serverContext.sendPacketToClient((
                    (ManagementMessage) MessageFactory.create(MessageType.REMOVE_CONTACT, from, contactId))
                    .addParam("contactId", from)
                    .toPacket()
            );
            AckHelper.sendSentAck(serverContext, managementMessage);

        } catch (Exception e) {
            ServerPacketHandler.LOG.severe(String.format("Error while removing contact %d for user %d: %s", contactId, from, e.getMessage()));
            AckHelper.sendFailedAck(serverContext, managementMessage, "Internal server error");
        }
    }

    /**
     * Handles updating a user's pseudo (username).
     *
     * @param serverContext    the server context
     * @param managementMessage the management message containing the update pseudo request
     */
    private void updatePseudo(TchatsAppServer.ServerContext serverContext, ManagementMessage managementMessage) {
        int from = managementMessage.getFrom();
        String newPseudo = managementMessage.getParamAsType("newPseudo", String.class);

        UserInfo user = serverContext.getUserRepository().findById(from);
        if (user == null) {
            ServerPacketHandler.LOG.warning(String.format("User %d not found while trying to update pseudo", from));
            AckHelper.sendFailedAck(serverContext, managementMessage, "User not found");
            return;
        }

        if (newPseudo == null || newPseudo.isEmpty()) {
            ServerPacketHandler.LOG.warning(String.format("User %d provided invalid new pseudo", from));
            AckHelper.sendFailedAck(serverContext, managementMessage, "Invalid pseudo");
            return;
        }

        user.setUsername(newPseudo);
        serverContext.getUserRepository().update(user.getId(), user);
        ServerPacketHandler.LOG.info(String.format("User %d updated pseudo to %s", from, newPseudo));

        for (int contactId : user.getContacts()) {
            if (serverContext.isClientConnected(contactId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.UPDATE_PSEUDO, from, contactId))
                        .addParam("contactId", from)
                        .addParam("newPseudo", newPseudo)
                        .toPacket()
                );
            }
        }

        AckHelper.sendSentAck(serverContext, managementMessage);
    }
}
