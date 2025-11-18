package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.ChangeMenberInGroupEvent;
import fr.uga.im2ag.m1info.chatservice.client.repository.GroupClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

public class AddMenberGroupCommand extends SendManagementMessageCommand {
    private final int groupID;
    private final GroupClientRepository repo;
    private final int menber;

    public AddMenberGroupCommand(String commandId, int groupID, GroupClientRepository repo, int menber) {

        super(commandId, MessageType.ADD_GROUP_MEMBER);
        this.groupID = groupID;
        this.menber = menber;
        this.repo = repo;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        EventBus.getInstance().publish(new ChangeMenberInGroupEvent(this, groupID, menber));
        System.out.printf("[CLIENT ] Menbre %d bien ajouté au groupe %d\n", menber, groupID);
        // userClient.setPseudo(newPseudo);
        return true;
    }
    
}


