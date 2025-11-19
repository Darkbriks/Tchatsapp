package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.ChangeMemberInGroupEvent;
import fr.uga.im2ag.m1info.chatservice.client.repository.GroupClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

public class AddMemberGroupCommand extends SendManagementMessageCommand {
    private final int groupID;
    private final GroupClientRepository repo;
    private final int member;

    public AddMemberGroupCommand(String commandId, int groupID, GroupClientRepository repo, int member) {

        super(commandId, MessageType.ADD_GROUP_MEMBER);
        this.groupID = groupID;
        this.member = member;
        this.repo = repo;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        EventBus.getInstance().publish(new ChangeMemberInGroupEvent(this, groupID, member));
            System.out.printf("[Client] You successfully add member %d to the group %d\n",member, groupID);
        // userClient.setPseudo(newPseudo);
        return true;
    }
    
}


