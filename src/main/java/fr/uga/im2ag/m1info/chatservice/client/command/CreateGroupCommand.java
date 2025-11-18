package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.UserPseudoUpdatedEvent;
import fr.uga.im2ag.m1info.chatservice.client.model.UserClient;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

public class CreateGroupCommand extends SendManagementMessageCommand {
    private final String groupName;
    private final UserClient userClient;

    public CreateGroupCommand(String commandId, String newPseudo, UserClient userClient) {
        super(commandId, MessageType.CREATE_GROUP);
        this.groupName = newPseudo;
        this.userClient = userClient;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        System.out.printf("[CLIENT ] Groupe %s bien crée \n", groupName);
        // userClient.setPseudo(newPseudo);
        return true;
    }
    
}

