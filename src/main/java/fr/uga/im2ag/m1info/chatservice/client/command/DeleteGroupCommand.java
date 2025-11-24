package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.DeleteGroupEvent;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.repository.GroupRepository;

public class DeleteGroupCommand extends SendManagementMessageCommand {
    private final int groupID;

    public DeleteGroupCommand(String commandId, int groupID, GroupRepository repo) {

        super(commandId, MessageType.DELETE_GROUP);
        this.groupID = groupID;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        EventBus.getInstance().publish(new DeleteGroupEvent(this, groupID, true));
        System.out.printf("[CLIENT ] Groupe %d bien détruit\n", groupID);
        return true;
    }
    
}

