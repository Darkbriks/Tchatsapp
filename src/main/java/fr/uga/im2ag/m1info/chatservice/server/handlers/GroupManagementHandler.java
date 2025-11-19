package fr.uga.im2ag.m1info.chatservice.server.handlers;

import java.util.Map;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer.ServerContext;
import fr.uga.im2ag.m1info.chatservice.server.model.GroupInfo;

class GroupManagementHandler extends ServerPacketHandler {
            
    public void addMember(int groupID,  int memberId , int requesterId){
        //TODO : implement the message type in Enum + method
    }
    
    public void removeMember(int groupId, int memberId, int requesterId){
        //TODO : implement the message type in Enum + method

    }
    
    public void deleteGroup(int groupId, int requesterId){
        //TODO : implement the message type in Enum + method

    }
    
    public void updateNameGroup(int groupId, int requesterId){}

    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof ManagementMessage userMsg)) {
            throw new IllegalArgumentException("Invalid message type for UserManagementHandler");
        }

        switch (userMsg.getMessageType()) {
            case CREATE_GROUP -> createGroup(serverContext, userMsg);
            case UPDATE_GROUP_NAME -> updateGroupName(serverContext, userMsg);
            default -> throw new IllegalArgumentException("Unsupported management message type");
        }
    }
    private Object updateGroupName(ServerContext serverContext, ManagementMessage userMsg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateGroupName'");
    }

    private Object createGroup(ServerContext serverContext, ManagementMessage userMsg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createGroup'");
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.CREATE_GROUP
                || messageType == MessageType.UPDATE_GROUP_NAME;
    }
}
 
