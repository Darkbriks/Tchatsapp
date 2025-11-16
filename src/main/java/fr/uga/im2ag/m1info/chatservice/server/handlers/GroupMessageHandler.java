package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.model.GroupInfo;
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
            case REMOVE_GROUP_MEMBER -> addGroupMenber(serverContext, userMsg);
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
        System.out.printf("[Server] Group %d updated name to %s%n", adminGroup, newGroupName);

        for (int contactId : group.getMenbers()) {
            if (serverContext.isClientConnected(contactId)) {
                serverContext.sendPacketToClient(((ManagementMessage) MessageFactory.create(MessageType.UPDATE_GROUP_NAME, groupId, contactId))
                        .addParam("contactId", groupId)
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


    private static void addGroupMenber(ServerContext serverContext, ManagementMessage userMsg) {
        throw new UnsupportedOperationException("Unimplemented method 'addGroupMenber'");
    }


    private static void leaveGroup(ServerContext serverContext, ManagementMessage userMsg) {
        throw new UnsupportedOperationException("Unimplemented method 'leaveGroup'");
    }


    private static void createGroup(ServerContext serverContext, ManagementMessage userMsg) {
        throw new UnsupportedOperationException("Unimplemented method 'createGroup'");
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
