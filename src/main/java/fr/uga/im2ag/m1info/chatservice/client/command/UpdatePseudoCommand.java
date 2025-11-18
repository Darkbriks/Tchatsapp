package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.UserPseudoUpdatedEvent;
import fr.uga.im2ag.m1info.chatservice.client.model.UserClient;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

public class UpdatePseudoCommand extends SendManagementMessageCommand {
    private final String newPseudo;
    private final UserClient userClient;

    public UpdatePseudoCommand(String commandId, String newPseudo, UserClient userClient) {
        super(commandId, MessageType.UPDATE_PSEUDO);
        this.newPseudo = newPseudo;
        this.userClient = userClient;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        userClient.setPseudo(newPseudo);
        EventBus.getInstance().publish(new UserPseudoUpdatedEvent(this, newPseudo));
        System.out.printf("[Client] User pseudo has been updated to '%s'.%n", newPseudo);
        return true;
    }
}
