package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

public abstract class ValidatingServerPacketHandler extends ServerPacketHandler {
    protected boolean validateSenderRegistered(ProtocolMessage message, TchatsAppServer.ServerContext ctx) {
        if (!ctx.isClientRegistered(message.getFrom())) {
            LOG.warning(() -> String.format(
                    "Message from unregistered user %d (type: %s)",
                    message.getFrom(), message.getMessageType()
            ));
            AckHelper.sendFailedAck(ctx, message, "Sender not registered");
            return false;
        }
        return true;
    }

    protected boolean validateRecipientExists(ProtocolMessage message, TchatsAppServer.ServerContext ctx) {
        if (!ctx.isClientRegistered(message.getTo())) {
            LOG.warning(() -> String.format(
                    "Message to non-existent user %d (type: %s)",
                    message.getTo(), message.getMessageType()
            ));
            AckHelper.sendFailedAck(ctx, message, "Recipient not found");
            return false;
        }
        return true;
    }

    protected boolean validateSenderMemberOfGroup(ProtocolMessage message, TchatsAppServer.ServerContext ctx) {
        UserInfo sender = ctx.getUserRepository().findById(message.getFrom());
        //if (!sender.isMemberOfGroup(message.getTo())) {
        if (!ctx.getGroupRepository().findById(message.getTo()).hasMember(message.getFrom())) {
            LOG.warning(() -> String.format(
                    "User %d is not a member of group %d (type: %s)",
                    message.getFrom(), message.getTo(), message.getMessageType()
            ));
            AckHelper.sendFailedAck(ctx, message, "Sender not member of group");
            return false;
        }
        return true;
    }

    protected boolean checkContactRelationship(int from, int to, TchatsAppServer.ServerContext ctx) {
        UserInfo sender = ctx.getUserRepository().findById(from);
        return sender.hasContact(to);
    }

    protected boolean isGroupId(int id, TchatsAppServer.ServerContext ctx) {
        return ctx.getGroupRepository().findById(id) != null;
    }

    protected void sendPacketToRecipient(ProtocolMessage message, TchatsAppServer.ServerContext ctx) {
        if (isGroupId(message.getTo(), ctx)) {
            for (int memberId : ctx.getGroupRepository().findById(message.getTo()).getMembersId()) {
                if (memberId != message.getFrom()) {
                    ctx.sendPacketToClient(message.toPacket(), memberId);
                }
            }
        } else {
            ctx.sendPacketToClient(message.toPacket());
        }
    }
}
