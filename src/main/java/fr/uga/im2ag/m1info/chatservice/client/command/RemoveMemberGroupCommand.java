package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.ChangeMemberInGroupEvent;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.model.GroupInfo;
import fr.uga.im2ag.m1info.chatservice.common.repository.GroupRepository;

public class RemoveMemberGroupCommand extends SendManagementMessageCommand {
    private final int groupID;
    private final GroupRepository repo;
    private final int member;

    public RemoveMemberGroupCommand(String commandId, int groupID, GroupRepository repo, int member) {

        super(commandId, MessageType.ADD_GROUP_MEMBER);
        this.groupID = groupID;
        this.member = member;
        this.repo = repo;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        GroupInfo group = repo.findById(groupID);
        group.removeMember(member);
        repo.update(groupID, group);
        EventBus.getInstance().publish(new ChangeMemberInGroupEvent(this, groupID, member, false));
        System.out.printf("[CLIENT ] Menbre %d bien supprimé du groupe %d\n", member, groupID);
        return true;
    }
    
}



