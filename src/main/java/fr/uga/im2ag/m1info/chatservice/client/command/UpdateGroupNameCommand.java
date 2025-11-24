package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.UpdateGroupNameEvent;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.repository.GroupRepository;

public class UpdateGroupNameCommand extends SendManagementMessageCommand {
    private final int groupID;
    private final GroupRepository repo;
    private final String newName;

    public UpdateGroupNameCommand(String commandId, int groupID, GroupRepository repo, String newName) {

        super(commandId, MessageType.UPDATE_GROUP_NAME);
        this.groupID = groupID;
        this.newName = newName;
        this.repo = repo;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        EventBus.getInstance().publish(new UpdateGroupNameEvent(this, groupID, newName, true));
        System.out.printf("[Client] You successfully change group name to %s for group %d\n", newName, groupID);
        return true;
    }
    
}


