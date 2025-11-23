package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.GroupCreateEvent;
import fr.uga.im2ag.m1info.chatservice.client.repository.GroupClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

public class CreateGroupCommand extends SendManagementMessageCommand {
    private final String groupName;
    private final GroupClientRepository repo;

    public CreateGroupCommand(String commandId, String groupName, GroupClientRepository repo) {

        super(commandId, MessageType.CREATE_GROUP);
        this.groupName = groupName;
        this.repo = repo;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        EventBus.getInstance().publish(new GroupCreateEvent(this, -1, groupName));
        System.out.printf("[CLIENT ] Groupe %s bien crée \n", groupName);
        // userClient.setPseudo(newPseudo);
        return true;
    }
    
}

