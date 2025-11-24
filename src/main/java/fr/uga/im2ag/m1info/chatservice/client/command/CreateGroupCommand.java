package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.repository.GroupRepository;

public class CreateGroupCommand extends SendManagementMessageCommand {
    private final String groupName;
    private final GroupRepository repo;

    public CreateGroupCommand(String commandId, String groupName, GroupRepository repo) {

        super(commandId, MessageType.CREATE_GROUP);
        this.groupName = groupName;
        this.repo = repo;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        //EventBus.getInstance().publish(new GroupCreateEvent(this, -1, groupName));
        System.out.printf("[CLIENT ] Groupe %s bien crée \n", groupName);
        return true;
    }
    
}

