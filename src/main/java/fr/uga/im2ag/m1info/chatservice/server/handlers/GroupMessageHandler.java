package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.KeyInMessage;
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

        String newGroupName= groupManagementMessage.getParamAsType(KeyInMessage.GROUP_NAME, String.class);

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

    private static void removeGroupMenber(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        int oldMenberID = groupManagementMessage.getParamAsType(KeyInMessage.MENBER_REMOVE_ID, Integer.class);

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
                        .addParam(KeyInMessage.GROUP_ID, groupId)
                        .addParam(KeyInMessage.MENBER_REMOVE_ID, oldMenberID)
                        .toPacket()
                );
            }
        }
        
        group.removeMenber(oldMenberID);
        serverContext.getGroupRepository().update(group.getId(), group);
        System.out.printf("[Server] Group %d remove menber %d%n", groupId, oldMenberID);

        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.REMOVE_GROUP_MEMBER, groupId, adminGroup))
                .addParam(KeyInMessage.MENBER_REMOVE_ID, oldMenberID)
                .addParam(KeyInMessage.GROUP_ID, groupId)
                .addParam("ack", "true")
                .toPacket()
        );
    }

    private static void addGroupMenber(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        int newMenberID = groupManagementMessage.getParamAsType(KeyInMessage.MENBER_ADD_ID, Integer.class);

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


        for (int menberId : group.getMenbers()) {
            if (serverContext.isClientConnected(menberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, menberId))
                        .addParam(KeyInMessage.GROUP_ID, groupId)
                        .addParam(KeyInMessage.MENBER_ADD_ID, newMenberID)
                        .toPacket()
                );
            }
        }

        group.addMenber(newMenberID);
        serverContext.getGroupRepository().update(group.getId(), group);
        System.out.printf("[Server] Group %d add menber %d%n", adminGroup, newMenberID);
        ManagementMessage message  = (ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, newMenberID);
        message.addParam(KeyInMessage.MENBER_ADD_ID, newMenberID)
                .addParam(KeyInMessage.GROUP_ID, groupId)
                .addParam(KeyInMessage.GROUP_ADMIN_ID, adminGroup)
                .addParam(KeyInMessage.GROUP_NAME, group.getGroupName());
        int i = 0;
        for ( int user : group.getMenbers()){
            message.addParam(KeyInMessage.GROUP_MENBER_ID + i, user);
            i++;
        }
                //.addParam("groupMenbers", group.getMenbers())
        serverContext.sendPacketToClient(message.toPacket());

        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, adminGroup))
                .addParam(KeyInMessage.MENBER_ADD_ID, newMenberID)
                .addParam(KeyInMessage.GROUP_ID, groupId)
                .addParam("ack", "true")
                .toPacket()
        );
    }


    private static void leaveGroup(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int groupMenber = groupManagementMessage.getFrom();
        int groupId = groupManagementMessage.getTo();

        GroupInfo group = serverContext.getGroupRepository().findById(groupId);
        UserInfo newMenber = serverContext.getUserRepository().findById(groupMenber);
        if (group == null) {
            System.out.printf("[Server] Group %d not found while a menber trying to leave%n", groupId);
            return;
        }

        if (! group.hasMenber(groupMenber)){
            System.out.printf("[Server] User %d is not in the group %d but try to leave i\n", groupMenber, groupId);
            return;
        }

        if (newMenber == null) {
            System.out.printf("[Server] User %d doesn't exist and tyr to leave group %d\n", groupMenber, groupId);
            serverContext.sendErrorMessage(0, groupMenber, ErrorMessage.ErrorLevel.WARNING, "MENBER_NOT_EXISTING", "Cannot add non-existing user as menber.");
            return;
        }


        for (int menberId : group.getMenbers()) {
            if (serverContext.isClientConnected(menberId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.ADD_GROUP_MEMBER, groupId, menberId))
                        .addParam(KeyInMessage.GROUP_ID, groupId)
                        .addParam(KeyInMessage.MENBER_REMOVE_ID, groupMenber )
                        .toPacket()
                );
            }
        }

        group.removeMenber(groupMenber);
        serverContext.getGroupRepository().update(group.getId(), group);
        System.out.printf("[Server] menber %d leave group %d %n", groupMenber, groupId);

        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.LEAVE_GROUP, groupId, groupMenber))
                .addParam(KeyInMessage.GROUP_ID, groupId)
                .addParam(KeyInMessage.MENBER_REMOVE_ID, groupMenber )
                .addParam("ack", "true")
                .toPacket()
        );
    }


    private static void createGroup(ServerContext serverContext, ManagementMessage groupManagementMessage) {
        int adminGroup = groupManagementMessage.getFrom();
        String newGroupName= groupManagementMessage.getParamAsType(KeyInMessage.GROUP_NAME, String.class);
        int groupID = serverContext.generateClientId();
        GroupInfo group = new GroupInfo(groupID, adminGroup, newGroupName);
        serverContext.getGroupRepository().add(group);
        System.out.printf("[Server] Group %d with name %s now exist and admin is menber %d%n", groupID, newGroupName, adminGroup);
        serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.CREATE_GROUP, groupID, adminGroup))
                .addParam(KeyInMessage.GROUP_ID, groupID)
                .addParam(KeyInMessage.GROUP_NAME, newGroupName)
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
