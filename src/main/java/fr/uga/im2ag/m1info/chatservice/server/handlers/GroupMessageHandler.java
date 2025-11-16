package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.model.GroupInfo;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer.ServerContext;

public class GroupMessageHandler extends  ServerPacketHandler {
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for ErrorMessageHandler");
        }

        switch (userMsg.getMessageType()) {
            case CREATE_GROUP -> createGroup(serverContext, userMsg);
            case LEAVE_GROUP -> leaveGroup(serverContext, userMsg);
            case ADD_GROUP_MEMBER -> addGroupMenber(serverContext, userMsg);
            case REMOVE_GROUP_MEMBER -> removeGroupMenber(serverContext, userMsg);
            case UPDATE_GROUP_NAME -> updateGroupName(serverContext, userMsg);
            default -> throw new IllegalArgumentException("Unsupported group management message type");
        }
    }

    private static void updateGroupName(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        String newGroupName= groupManagementMessage.getParamAsType("newGroupName", String.class);

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        if (group == null) {
            System.out.printf("[Server] Group %d not found while trying to update name%n", groupId);
            return;
        }

        if (group.getAdminId() != adminGroup){
            System.out.printf("[Server] User %d is not the admin of group %d. Real admin is %d%n", adminGroup, groupId, group.getAdminId());
            return;
        }

        if (newGroupName == null || newGroupName.isEmpty()) {
            System.out.printf("[Server] User %d provided invalid new groupName for group %d %n", adminGroup, groupId);
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.ERROR, "INVALID_PSEUDO", "The new pseudo cannot be null or empty.");
            return;
        }

        group.setGroupName(newGroupName);
        serverContext.getGroupRepository().update(group.getId(), group);
        System.out.printf("[Server] Group %d updated name to %s%n", groupId, newGroupName);

        for (int menberId : group.getMenbers()) {
            if (serverContext.isClientConnected(menberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.UPDATE_GROUP_NAME, groupId, menberId))
                        .addParam("groupId", groupId)
                        .addParam("groupName", newGroupName)
                        .toPacket()
                );
            }
        }

        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.UPDATE_GROUP_NAME, groupId, adminGroup))
                .addParam("groupName", newGroupName)
                .addParam("ack", "true")
                .toPacket()
        );
    }

    private static void removeGroupMenber(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        int oldMenberID = groupManagementMessage.getParamAsType("newMenberID", Integer.class);

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        UserInfo oldMenber = serverContext.getUserRepository().findById(oldMenberID);
        if (group == null) {
            System.out.printf("[Server] Group %d not found while trying to remove menber%n", groupId);
            return;
        }

        if (group.getAdminId() != adminGroup){
            System.out.printf("[Server] User %d is not the admin of group %d. Real admin is %d%n", adminGroup, groupId, group.getAdminId());
            return;
        }

        if (oldMenber == null) {
            System.out.printf("[Server] User %d provided invalid menberId to remove , [%d] not found\n", adminGroup, oldMenberID);
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.WARNING, "MENBER_NOT_EXISTING", "Cannot remove non-existing user as menber.");
            return;
        }

        if (! group.hasMenber(oldMenberID)) {
            System.out.printf("[Server] User %d provided invalid menberId to remove , [%d] not in group\n", adminGroup, oldMenberID);
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.WARNING, "MENBER_NOT_INSIDE", "Cannot remove user who is not a menber.");
            return;
        }

        // Deleted menber is still in the group, when he will receive the message, need to leave the group by comparing his ID and the removed one
        for (int menberId : group.getMenbers()) {
            if (serverContext.isClientConnected(menberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.REMOVE_GROUP_MEMBER, groupId, menberId))
                        .addParam("groupId", groupId)
                        .addParam("deleteMenber", oldMenberID)
                        .toPacket()
                );
            }
        }
        
        group.removeMenber(oldMenberID);
        serverContext.getGroupRepository().update(group.getId(), group);
        System.out.printf("[Server] Group %d remove menber %d%n", groupId, oldMenberID);

        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.REMOVE_GROUP_MEMBER, groupId, adminGroup))
                .addParam("deleteMenber", oldMenberID)
                .addParam("ack", "true")
                .toPacket()
        );
    }

    private static void addGroupMenber(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        int newMenberID = groupManagementMessage.getParamAsType("newMenberID", Integer.class);

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        UserInfo newMenber = serverContext.getUserRepository().findById(newMenberID);
        if (group == null) {
            System.out.printf("[Server] Group %d not found while trying to add menber%n", groupId);
            return;
        }

        if (group.getAdminId() != adminGroup){
            System.out.printf("[Server] User %d is not the admin of group %d. Real admin is %d%n", adminGroup, groupId, group.getAdminId());
            return;
        }

        if (newMenber == null) {
            System.out.printf("[Server] User %d provided invalid menberId to add, [%d] not found\n", adminGroup, newMenberID);
            serverContext.sendErrorMessage(0, adminGroup, ErrorMessage.ErrorLevel.WARNING, "MENBER_NOT_EXISTING", "Cannot add non-existing user as menber.");
            return;
        }

        group.addMenber(newMenberID);
        serverContext.getGroupRepository().update(group.getId(), group);
        System.out.printf("[Server] Group %d add menber %d%n", adminGroup, newMenberID);

        // TODO  a new menber need to access every data like other menbers !!!
        for (int menberId : group.getMenbers()) {
            if (serverContext.isClientConnected(menberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, menberId))
                        .addParam("groupId", groupId)
                        .addParam("newMenberId", newMenberID)
                        .toPacket()
                );
            }
        }

        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, adminGroup))
                .addParam("newMenber", newMenberID)
                .addParam("ack", "true")
                .toPacket()
        );
    }


    private static void leaveGroup(ServerContext serverContext, ManagementMessage userMsg) {
        throw new UnsupportedOperationException("Unimplemented method 'leaveGroup'");
    }


    private static void createGroup(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        String newGroupName= groupManagementMessage.getParamAsType("newGroupName", String.class);
        int groupID = serverContext.generateClientId();
        GroupInfo group = new GroupInfo(groupID, adminGroup, newGroupName);
        serverContext.getGroupRepository().add(group);
        System.out.printf("[Server] Group %d now exist and admin is menber %d%n", groupID, adminGroup);
        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.CREATE_GROUP, groupID, adminGroup))
                .addParam("newGroupID", groupID)
                .addParam("newGroupName", newGroupName)
                .addParam("ack", "true")
                .toPacket()
        );
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
