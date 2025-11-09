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
            case ADD_CONTACT -> addContact(serverContext, userMsg);
            case REMOVE_CONTACT -> removeContact(serverContext, userMsg);
            default -> throw new IllegalArgumentException("Unsupported management message type");
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.ADD_CONTACT || messageType == MessageType.REMOVE_CONTACT;
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
}
