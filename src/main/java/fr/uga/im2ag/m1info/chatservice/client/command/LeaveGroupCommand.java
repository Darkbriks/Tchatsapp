package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.LeaveGroupEvent;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.repository.GroupRepository;

import java.util.Map;

public class LeaveGroupCommand extends SendManagementMessageCommand {
    private final int groupID;
    private final GroupRepository repo;

    public LeaveGroupCommand(String commandId, int groupID, GroupRepository repo) {
        super(commandId, MessageType.ADD_GROUP_MEMBER);
        this.groupID = groupID;
        this.repo = repo;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType, Map<String, Object> params) {
        repo.delete(groupID);
        EventBus.getInstance().publish(new LeaveGroupEvent(this, groupID));
        System.out.printf("[CLIENT ] Groupe %d bien quitté\n", groupID);
        return true;
    }
    
}



