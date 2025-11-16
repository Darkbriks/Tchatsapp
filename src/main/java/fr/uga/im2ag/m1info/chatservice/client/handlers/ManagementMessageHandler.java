package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientContext;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

// TODO: Interact with repositories and notify observers
public class ManagementMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientContext context) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for ManagementMessageHandler");
        }

        switch (userMsg.getMessageType()) {
            case ADD_CONTACT -> addContact(userMsg, context);
            case REMOVE_CONTACT -> removeContact(userMsg, context);
            case UPDATE_PSEUDO -> updatePseudo(userMsg, context);
            default -> throw new IllegalArgumentException("Unsupported management message type: " + userMsg.getMessageType());
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.ADD_CONTACT
                || messageType == MessageType.REMOVE_CONTACT
                || messageType == MessageType.UPDATE_PSEUDO;
    }

    private void addContact(ManagementMessage message, ClientContext context) {
        String contactPseudo = message.getParamAsType("contactPseudo", String.class);
        Integer contactId = message.getParamAsType("contactId", Integer.class);

        if (contactPseudo != null && contactId != null) {
            System.out.println("[Client] Contact added: " + contactPseudo + " (ID: " + contactId + ")");
            ContactClient contact = new ContactClient(contactId, contactPseudo);
            context.getContactRepository().add(contact);
        }
    }

    private void removeContact(ManagementMessage message, ClientContext context) {
        String contactPseudo = message.getParamAsType("contactPseudo", String.class);
        Integer contactId = message.getParamAsType("contactId", Integer.class);

        if (contactPseudo != null && contactId != null) {
            System.out.println("[Client] Contact removed: " + contactPseudo + " (ID: " + contactId + ")");
            context.getContactRepository().delete(contactId);
        }
    }

    private void updatePseudo(ManagementMessage message, ClientContext context) {
        String newPseudo = message.getParamAsType("newPseudo", String.class);
        Integer contactId = message.getParamAsType("contactId", Integer.class);

        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            System.out.println("[Client] Your pseudo has been updated to: " + newPseudo);
            // TODO: Update in UserRepository
        } else if (contactId != null && newPseudo != null) {
            // A contact has updated their pseudo
            System.out.println("[Client] Contact " + contactId + " updated pseudo to: " + newPseudo);
            context.getContactRepository().update(contactId, new ContactClient(contactId, newPseudo));
        }
    }
}
