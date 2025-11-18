package fr.uga.im2ag.m1info.chatservice.client.handlers;

import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.types.*;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.client.model.GroupClient;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.KeyInMessage;
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
            case CREATE_GROUP -> createGroup(userMsg, context);
            case LEAVE_GROUP -> leaveGroup(userMsg, context);
            case ADD_GROUP_MEMBER -> addGroupMenber(userMsg, context);
            case REMOVE_GROUP_MEMBER -> removeGroupMenber(userMsg, context);
            case UPDATE_GROUP_NAME -> updateGroupName(userMsg, context);
            default -> throw new IllegalArgumentException("Unsupported management message type: " + userMsg.getMessageType());
        }
    }


    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.REMOVE_CONTACT
                || messageType == MessageType.UPDATE_PSEUDO
                || messageType == MessageType.CREATE_GROUP
                || messageType == MessageType.LEAVE_GROUP
                || messageType == MessageType.ADD_GROUP_MEMBER
                || messageType == MessageType.REMOVE_GROUP_MEMBER
                || messageType == MessageType.UPDATE_GROUP_NAME;
    }

    private void removeContact(ManagementMessage message, ClientController context) {
        int contactId = message.getParamAsType("contactId", Double.class).intValue();

        // TODO: Discuss about whether to delete the conversation or keep it
        context.getContactRepository().delete(contactId);
        publishEvent(new ContactRemovedEvent(this, contactId), context);
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


    private void createGroup(ManagementMessage message, ClientController context){
        String newGroupe= message.getParamAsType(KeyInMessage.GROUP_NAME, String.class);
        Integer groupId = message.getFrom();
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            System.out.println("[Client] Your Group has been created : " + newGroupe + " his ID is " + groupId);
            GroupClient group = new GroupClient(groupId, newGroupe, context.getClientId());
            context.getGroupRepository().add(group);
            publishEvent(new GroupCreateEvent(this, groupId), context);
        }
    }

    private void leaveGroup(ManagementMessage message, ClientController context){
        // TODO if admin leave group what happen ? e destroy the group or a new admin 
        // need to be choose
        int deleteMenber = getIntInParam(message, KeyInMessage.MENBER_REMOVE_ID);
        int groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            System.out.printf("[Client] You successfully leave group %d\n", groupId);

        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to leave group %d but it FAIL\n",deleteMenber, groupId);

        } else {
            // just a group menber not the admin
            if ( deleteMenber == context.getClientId()){
                System.out.printf("[Client] You leave the group %d !\n", groupId);
                context.getGroupRepository().delete(groupId);
            } else {
                GroupClient group = context.getGroupRepository().findById(groupId);
                group.removeMember(deleteMenber);
                context.getGroupRepository().update(groupId, group);
                System.out.printf("[Client] User %d leave the group %d !\n", deleteMenber, groupId);
            }
        }
    }

    private int getIntInParam(ManagementMessage message, String s){
        return (int ) Float.parseFloat( message.getParamAsType(s, String.class));
    }

    private void addGroupMenber(ManagementMessage message, ClientController context) {
        int newMenber = getIntInParam(message, KeyInMessage.MENBER_ADD_ID);
        int groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            // TODO admin is just a menber like other so he also receives the normal message
            publishEvent(new ChangeMenberInGroupEvent(this, groupId, newMenber), context);
        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to add menber %d to the group %d but it FAIL\n",newMenber, groupId);

        } else {
            // just a group menber not the admin
            if ( newMenber == context.getClientId()){
                System.out.printf("[Client] You have been add to the group %d !\n", groupId);
                String groupName = message.getParamAsType(KeyInMessage.GROUP_NAME, String.class);
                int adminId = getIntInParam(message, KeyInMessage.GROUP_ADMIN_ID);
                GroupClient group = new GroupClient(groupId, groupName, adminId);
                int i = 0;
                while ( true){
                    try{
                        int menber = getIntInParam(message, KeyInMessage.GROUP_MENBER_ID + i);
                        group.addMember(menber);
                        i++;
                    } catch (Exception e){
                        break;
                    }
                }
                context.getGroupRepository().add(group);
            } else {
                GroupClient group = context.getGroupRepository().findById(groupId);
                group.addMember(newMenber);
                context.getGroupRepository().update(groupId, group);
                System.out.printf("[Client] User %d have been add to the group %d !\n", newMenber, groupId);
            }

        }
    }

    private void removeGroupMenber(ManagementMessage message, ClientController context) {
        int deleteMenber = getIntInParam(message, KeyInMessage.MENBER_REMOVE_ID);
        int groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            publishEvent(new ChangeMenberInGroupEvent(this, groupId, deleteMenber), context);
            System.out.printf("[Client] You successfully remove menber %d to the group %d\n",deleteMenber, groupId);
            // TODO admin is just a menber like other so he also receives the normal message
            // GroupClient group = context.getGroupRepository().findById(groupId);
            // group.addMember(newMenber);
            // context.getGroupRepository().update(groupId, group);
        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to remove menber %d to the group %d but it FAIL\n",deleteMenber, groupId);

        } else {
            // just a group menber not the admin
            if ( deleteMenber == context.getClientId()){
                System.out.printf("[Client] You have been remove of the group %d !\n", groupId);
                // TODO  a new menber need to access every data like other menbers !!!
                context.getGroupRepository().delete(groupId);
            } else {
                GroupClient group = context.getGroupRepository().findById(groupId);
                group.removeMember(deleteMenber);
                context.getGroupRepository().update(groupId, group);
                System.out.printf("[Client] User %d have been removed to the group %d !\n", deleteMenber, groupId);
            }

        }
    }

    public void updateGroupName(ManagementMessage message, ClientController context){
        String newGroupName = message.getParamAsType(KeyInMessage.GROUP_NAME, String.class);
        Integer groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            System.out.printf("[Client] You successfully rename group %d to %s\n",groupId, newGroupName);
            // TODO admin is just a menber like other so he also receives the normal message
            // GroupClient group = context.getGroupRepository().findById(groupId);
            // group.addMember(newMenber);
            // context.getGroupRepository().update(groupId, group);
        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to rename group %d to %s but it FAIL",groupId, newGroupName);

        } else {
                GroupClient group = context.getGroupRepository().findById(groupId);
                group.setName(newGroupName);
                context.getGroupRepository().update(groupId, group);
                System.out.printf("[Client] Group %d have been renamed to %s !", groupId, newGroupName);

        }
    }
}
