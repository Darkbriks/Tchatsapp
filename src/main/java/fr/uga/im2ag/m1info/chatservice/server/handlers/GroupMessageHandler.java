package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.KeyInMessage;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer.ServerContext;
import fr.uga.im2ag.m1info.chatservice.server.model.GroupInfo;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

public class GroupMessageHandler extends  ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for ErrorMessageHandler");
        }

        switch (userMsg.getMessageType()) {
            case CREATE_GROUP -> createGroup(serverContext, userMsg);
            case LEAVE_GROUP -> leaveGroup(serverContext, userMsg);
            case ADD_GROUP_MEMBER -> addGroupMember(serverContext, userMsg);
            case REMOVE_GROUP_MEMBER -> removeGroupMember(serverContext, userMsg);
            case UPDATE_GROUP_NAME -> updateGroupName(serverContext, userMsg);
            default -> throw new IllegalArgumentException("Unsupported group management message type");
        }
    }

    private static void updateGroupName(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        String newGroupName= groupManagementMessage.getParamAsType(KeyInMessage.GROUP_NAME, String.class);

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        if (group == null) {
            LOG.warning(String.format("Group %d not found while trying to update name", groupId));
            return;
        }

        if (group.getAdminId() != adminGroup){
            LOG.warning(String.format("User %d is not the admin of group %d. Real admin is %d", adminGroup, groupId, group.getAdminId()));
            return;
        }

        if (newGroupName == null || newGroupName.isEmpty()) {
            LOG.warning(String.format("User %d provided invalid new groupName for group %d", adminGroup, groupId));
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.ERROR, "INVALID_PSEUDO", "The new pseudo cannot be null or empty.");
            return;
        }

        group.setGroupName(newGroupName);
        serverContext.getGroupRepository().update(group.getId(), group);
        LOG.info(String.format("Group %d updated name to %s", groupId, newGroupName));

        for (int memberId : group.getMembers()) {
            if (serverContext.isClientConnected(memberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.UPDATE_GROUP_NAME, groupId, memberId))
                        .addParam(KeyInMessage.GROUP_ID, groupId)
                        .addParam(KeyInMessage.GROUP_NAME, newGroupName)
                        .toPacket()
                );
            }
        }

        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.UPDATE_GROUP_NAME, groupId, adminGroup))
                .addParam(KeyInMessage.GROUP_NAME, newGroupName)
                .addParam(KeyInMessage.GROUP_ID, groupId)
                .addParam("ack", "true")
                .toPacket()
        );
    }

    private static void removeGroupMember(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        int oldMemberID =  (int) Float.parseFloat(groupManagementMessage.getParamAsType(KeyInMessage.MEMBER_REMOVE_ID, String.class));

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        UserInfo oldMember = serverContext.getUserRepository().findById(oldMemberID);
        if (group == null) {
            LOG.warning(String.format("Group %d not found while trying to remove member", groupId));
            return;
        }

        if (group.getAdminId() != adminGroup){
            LOG.warning(String.format("User %d is not the admin of group %d. Real admin is %d", adminGroup, groupId, group.getAdminId()));
            return;
        }

        if (oldMember == null) {
            LOG.warning(String.format("User %d provided invalid memberId to remove , [%d] not found", adminGroup, oldMemberID));
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.WARNING, "MEMBER_NOT_EXISTING", "Cannot remove non-existing user as member.");
            return;
        }

        if (! group.hasMember(oldMemberID)) {
            LOG.warning(String.format("User %d provided invalid memberId to remove , [%d] not in group", adminGroup, oldMemberID));
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.WARNING, "MEMBER_NOT_INSIDE", "Cannot remove user who is not a member.");
            return;
        }

        // Deleted member is still in the group, when he will receive the message, need to leave the group by comparing his ID and the removed one

        for (int memberId : group.getMembers()) {
            if (serverContext.isClientConnected(memberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.REMOVE_GROUP_MEMBER, groupId, memberId))
                        .addParam(KeyInMessage.GROUP_ID, groupId)
                        .addParam(KeyInMessage.MEMBER_REMOVE_ID, oldMemberID)
                        .toPacket()
                );
            }
        }
        
        group.removeMember(oldMemberID);
        serverContext.getGroupRepository().update(group.getId(), group);
        LOG.info(String.format("Group %d remove member %d", groupId, oldMemberID));

        AckHelper.sendSentAck(serverContext, groupManagementMessage);
    }

    private static void addGroupMember(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();
        int newMemberID = (int) Float.parseFloat(groupManagementMessage.getParamAsType(KeyInMessage.MEMBER_ADD_ID, String.class));

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        UserInfo newMember = serverContext.getUserRepository().findById(newMemberID);
        if (group == null) {
            LOG.warning(String.format("Group %d not found while trying to add member", groupId));
            AckHelper.sendFailedAck(serverContext, groupManagementMessage, "Group not found");
            return;
        }

        if (group.getAdminId() != adminGroup){
            LOG.warning(String.format("User %d is not the admin of group %d. Real admin is %d", adminGroup, groupId, group.getAdminId()));
            AckHelper.sendFailedAck(serverContext, groupManagementMessage, "You are not the admin");
            return;
        }

        if (newMember == null) {
            LOG.warning(String.format("User %d provided invalid memberId to add , [%d] not found", adminGroup, newMemberID));
            AckHelper.sendFailedAck(serverContext, groupManagementMessage, "User to add don't exists");
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.WARNING, "MEMBER_NOT_EXISTING", "Cannot add non-existing user as member.");
            return;
        }
        if (newMemberID == adminGroup) {
            LOG.warning(String.format("User %d provided invalid memberId to add , [%d] add himself", adminGroup, newMemberID));
            AckHelper.sendFailedAck(serverContext, groupManagementMessage, "User add himself");
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.WARNING, "MEMBER_NOT_EXISTING", "Cannot add already inside user as member.");
            return;
        }


        for (int memberId : group.getMembers()) {
            if (serverContext.isClientConnected(memberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, memberId))
                        .addParam(KeyInMessage.GROUP_ID, groupId)
                        .addParam(KeyInMessage.MEMBER_ADD_ID, newMemberID)
                        .toPacket()
                );
            }
        }

        group.addMember(newMemberID);
        serverContext.getGroupRepository().update(group.getId(), group);
        LOG.info(String.format("Group %d add member %d", groupId, newMemberID));
        ManagementMessage message  = (ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, newMemberID);
        message.addParam(KeyInMessage.MEMBER_ADD_ID, newMemberID)
                .addParam(KeyInMessage.GROUP_ID, groupId)
                .addParam(KeyInMessage.GROUP_ADMIN_ID, adminGroup)
                .addParam(KeyInMessage.GROUP_NAME, group.getGroupName());
        int i = 0;
        for ( int user : group.getMembers()){
            message.addParam(KeyInMessage.GROUP_MEMBER_ID + i, user);
            i++;
        }
                //.addParam("groupMembers", group.getMembers())
        serverContext.sendPacketToClient(message.toPacket());

        AckHelper.sendSentAck(serverContext, groupManagementMessage);
    }


    private static void leaveGroup(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int groupMember = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        UserInfo newMember = serverContext.getUserRepository().findById(groupMember);
        if (group == null) {
            LOG.warning(String.format("Group %d not found while a member trying to leave", groupId));
            return;
        }

        if (! group.hasMember(groupMember)){
            LOG.warning(String.format("User %d is not in the group %d but try to leave i", groupMember, groupId));
            return;
        }

        if (newMember == null) {
            LOG.warning(String.format("User %d provided invalid memberId to leave , [%d] not found", groupMember, groupMember));
            serverContext.sendErrorMessage(0, groupMember, ErrorMessage.ErrorLevel.WARNING, "MEMBER_NOT_EXISTING", "Cannot add non-existing user as member.");
            return;
        }


        for (int memberId : group.getMembers()) {
            if (serverContext.isClientConnected(memberId)) {
                LOG.info(String.format("member %d leave group %d Send this info to %d", groupMember, groupId, memberId));
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.LEAVE_GROUP, groupId, memberId))
                        .addParam(KeyInMessage.GROUP_ID, groupId)
                        .addParam(KeyInMessage.MEMBER_REMOVE_ID, groupMember )
                        .toPacket()
                );
            }
        }

        group.removeMember(groupMember);
        serverContext.getGroupRepository().update(group.getId(), group);
        LOG.info(String.format("member %d leave group %d", groupMember, groupId));

        AckHelper.sendSentAck(serverContext, groupManagementMessage);
    }


    private static void createGroup(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        String newGroupName= groupManagementMessage.getParamAsType(KeyInMessage.GROUP_NAME, String.class);
        int groupID = serverContext.generateClientId();
        GroupInfo group = new GroupInfo(groupID, adminGroup, newGroupName);
        group.addMember(adminGroup);
        serverContext.getGroupRepository().add(group);
        LOG.info(String.format("Group %d with name %s now exist and admin is member %d", groupID, newGroupName, adminGroup));
        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.CREATE_GROUP, groupID, adminGroup))
                .addParam(KeyInMessage.GROUP_ID, groupID)
                .addParam(KeyInMessage.GROUP_NAME, newGroupName)
                .addParam("ack", "true")
                .toPacket()
        );
        AckHelper.sendSentAck(serverContext, groupManagementMessage);
    }


    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.CREATE_GROUP
                || messageType == MessageType.LEAVE_GROUP
                || messageType == MessageType.ADD_GROUP_MEMBER
                || messageType == MessageType.REMOVE_GROUP_MEMBER
                || messageType == MessageType.UPDATE_GROUP_NAME;
    }
}
