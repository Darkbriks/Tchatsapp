package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.types.*;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

public class ManagementMessageHandler extends ClientPacketHandler {
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for ManagementMessageHandler");
        }

        switch (userMsg.getMessageType()) {
            case REMOVE_CONTACT -> removeContact(userMsg, context);
            case UPDATE_PSEUDO -> updatePseudo(userMsg, context);
            default -> throw new IllegalArgumentException("Unsupported management message type: " + userMsg.getMessageType());
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.REMOVE_CONTACT
                || messageType == MessageType.UPDATE_PSEUDO;
    }

    private void removeContact(ManagementMessage message, ClientController context) {
        String contactPseudo = message.getParamAsType("contactPseudo", String.class);
        Integer contactId = message.getParamAsType("contactId", Integer.class);

        if (contactPseudo != null && contactId != null) {
            // TODO: Discuss about whether to delete the conversation or keep it
            context.getContactRepository().delete(contactId);
            publishEvent(new ContactRemovedEvent(this, contactId), context);
        }
    }

    private void updatePseudo(ManagementMessage message, ClientController context) {
        String newPseudo = message.getParamAsType("newPseudo", String.class);
        Integer contactId = message.getParamAsType("contactId", Integer.class);

        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            context.getActiveUser().setPseudo(newPseudo);
            publishEvent(new UserPseudoUpdatedEvent(this, newPseudo), context);
        } else if (contactId != null && newPseudo != null) {
            ContactClient contact = context.getContactRepository().findById(contactId);
            if (contact != null) {
                contact.updatePseudo(newPseudo);
                context.getContactRepository().update(contactId, contact);
                publishEvent(new ContactUpdatedEvent(this, contactId), context);
            }
        }
    }
}