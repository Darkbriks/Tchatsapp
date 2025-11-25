package fr.uga.im2ag.m1info.chatservice.client.command;

import fr.uga.im2ag.m1info.chatservice.client.event.system.EventBus;
import fr.uga.im2ag.m1info.chatservice.client.event.types.GroupCreateEvent;
import fr.uga.im2ag.m1info.chatservice.common.KeyInMessage;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.AckMessage;
import fr.uga.im2ag.m1info.chatservice.common.model.GroupInfo;
import fr.uga.im2ag.m1info.chatservice.common.repository.GroupRepository;

public class CreateGroupCommand extends SendManagementMessageCommand {
    private final GroupRepository repo;

    public CreateGroupCommand(String commandId, GroupRepository repo) {
        super(commandId, MessageType.CREATE_GROUP);
        this.repo = repo;
    }

    @Override
    public boolean handleAck(AckMessage message) {
        if (message.getAckType() == MessageStatus.FAILED) {
            onAckFailed(message.getErrorReason());
        } else {
            GroupInfo groupInfo = new GroupInfo(message.getParamAsType(KeyInMessage.GROUP_ID, Integer.class),
                                                message.getParamAsType(KeyInMessage.GROUP_ADMIN_ID, Integer.class),
                                                message.getParamAsType(KeyInMessage.GROUP_NAME, String.class));
            repo.add(groupInfo);
            EventBus.getInstance().publish(new GroupCreateEvent(this, groupInfo));
            System.out.printf("[CLIENT ] Group '%s' (ID: %d) successfully created.%n",
                              groupInfo.getGroupName(),
                              groupInfo.getGroupId());
        }
        return true;
    }

    @Override
    public boolean onAckReceived(MessageStatus ackType) {
        throw  new UnsupportedOperationException("Use handleAck instead");
    }
}

