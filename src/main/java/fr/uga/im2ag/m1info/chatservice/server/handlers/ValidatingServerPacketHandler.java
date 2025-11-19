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

    protected boolean checkContactRelationship(int from, int to, TchatsAppServer.ServerContext ctx) {
        UserInfo sender = ctx.getUserRepository().findById(from);
        return sender.hasContact(to);
    }
}
