package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.event.types.*;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.common.KeyInMessage;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.model.GroupInfo;

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
            case ADD_GROUP_MEMBER -> addGroupMember(userMsg, context);
            case REMOVE_GROUP_MEMBER -> removeGroupMember(userMsg, context);
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
        int contactId = message.getParamAsType("contactId", Integer.class);

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
            GroupInfo group = new GroupInfo(groupId, context.getClientId(), newGroupe);
            context.getGroupRepository().add(group);
            publishEvent(new GroupCreateEvent(this, group), context);
        }
    }

    private void leaveGroup(ManagementMessage message, ClientController context){
        // TODO if admin leave group what happen ? e destroy the group or a new admin 
        // need to be choose
        int deleteMember = getIntInParam(message, KeyInMessage.MEMBER_REMOVE_ID);
        int groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            System.out.printf("[Client] You successfully leave group %d\n", groupId);

        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to leave group %d but it FAIL\n",deleteMember, groupId);

        } else {
            // just a group member not the admin
            if ( deleteMember == context.getClientId()){
                System.out.printf("[Client] You leave the group %d !\n", groupId);
                context.getGroupRepository().delete(groupId);
            } else {
                GroupInfo group = context.getGroupRepository().findById(groupId);
                group.removeMember(deleteMember);
                context.getGroupRepository().update(groupId, group);
                System.out.printf("[Client] User %d leave the group %d !\n", deleteMember, groupId);
            }
        }
    }

    private int getIntInParam(ManagementMessage message, String s){
        return (int ) Float.parseFloat( message.getParamAsType(s, String.class));
    }

    private void addGroupMember(ManagementMessage message, ClientController context) {
        int newMember = getIntInParam(message, KeyInMessage.MEMBER_ADD_ID);
        int groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            // TODO admin is just a member like other so he also receives the normal message
            publishEvent(new ChangeMemberInGroupEvent(this, groupId, newMember, true), context);
        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to add member %d to the group %d but it FAIL\n",newMember, groupId);

        } else {
            // just a group member not the admin
            if ( newMember == context.getClientId()){
                System.out.printf("[Client] You have been add to the group %d !\n", groupId);
                String groupName = message.getParamAsType(KeyInMessage.GROUP_NAME, String.class);
                int adminId = getIntInParam(message, KeyInMessage.GROUP_ADMIN_ID);
                GroupInfo group = new GroupInfo(groupId, adminId, groupName);
                int i = 0;
                while (true){
                    try{
                        int member = getIntInParam(message, KeyInMessage.GROUP_MEMBER_ID + i);
                        group.addMember(member);
                        i++;
                    } catch (Exception e){
                        break;
                    }
                }
                context.getGroupRepository().add(group);
            } else {
                GroupInfo group = context.getGroupRepository().findById(groupId);
                group.addMember(newMember);
                context.getGroupRepository().update(groupId, group);
                System.out.printf("[Client] User %d have been add to the group %d !\n", newMember, groupId);
            }

        }
    }

    private void removeGroupMember(ManagementMessage message, ClientController context) {
        int deleteMember = getIntInParam(message, KeyInMessage.MEMBER_REMOVE_ID);
        int groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            publishEvent(new ChangeMemberInGroupEvent(this, groupId, deleteMember, false), context);
            System.out.printf("[Client] You successfully remove member %d to the group %d\n",deleteMember, groupId);
            // TODO admin is just a member like other so he also receives the normal message
            // GroupClient group = context.getGroupRepository().findById(groupId);
            // group.addMember(newMember);
            // context.getGroupRepository().update(groupId, group);
        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to remove member %d to the group %d but it FAIL\n",deleteMember, groupId);

        } else {
            // just a group member not the admin
            if ( deleteMember == context.getClientId()){
                System.out.printf("[Client] You have been remove of the group %d !\n", groupId);
                // TODO  a new member need to access every data like other members !!!
                context.getGroupRepository().delete(groupId);
            } else {
                GroupInfo group = context.getGroupRepository().findById(groupId);
                group.removeMember(deleteMember);
                context.getGroupRepository().update(groupId, group);
                System.out.printf("[Client] User %d have been removed to the group %d !\n", deleteMember, groupId);
            }

        }
    }

    public void updateGroupName(ManagementMessage message, ClientController context){
        String newGroupName = message.getParamAsType(KeyInMessage.GROUP_NAME, String.class);
        Integer groupId = getIntInParam(message, KeyInMessage.GROUP_ID);
        if (Boolean.TRUE.equals(message.getParamAsType("ack", Boolean.class))) {
            // This is an acknowledgment of our own pseudo update
            System.out.printf("[Client] You successfully rename group %d to %s\n",groupId, newGroupName);
            // TODO admin is just a member like other so he also receives the normal message
            // GroupClient group = context.getGroupRepository().findById(groupId);
            // group.addMember(newMember);
            // context.getGroupRepository().update(groupId, group);
        } else if ( Boolean.FALSE.equals(message.getParamAsType("ack", Boolean.class))){
            System.out.printf("[Client] You try to rename group %d to %s but it FAIL",groupId, newGroupName);

        } else {
            GroupInfo group = context.getGroupRepository().findById(groupId);
            group.setGroupName(newGroupName);
            context.getGroupRepository().update(groupId, group);
            System.out.printf("[Client] Group %d have been renamed to %s !", groupId, newGroupName);

        }
    }
}
