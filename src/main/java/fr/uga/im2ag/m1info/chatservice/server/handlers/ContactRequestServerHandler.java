package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ContactRequestMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ContactRequestResponseMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for contact request messages.
 */
public class ContactRequestServerHandler extends ValidatingServerPacketHandler {
    // Track pending requests: key = requestId, value = (senderId, receiverId)
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    private static class PendingRequest {
        final int senderId;
        final int receiverId;
        final long timestamp;

        PendingRequest(int senderId, int receiverId, long timestamp) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.timestamp = timestamp;
        }
    }

    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (message instanceof ContactRequestMessage crMsg) {
            handleRequest(crMsg, serverContext);
        } else if (message instanceof ContactRequestResponseMessage crrMsg) {
            handleResponse(crrMsg, serverContext);
        } else {
            throw new IllegalArgumentException("Invalid message type for ContactRequestServerHandler");
        }
    }

    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.CONTACT_REQUEST
                || messageType == MessageType.CONTACT_REQUEST_RESPONSE;
    }

    /**
     * Handle a contact request from sender to receiver.
     */
    private void handleRequest(ContactRequestMessage crMsg, TchatsAppServer.ServerContext serverContext) {
        if (!validateSenderRegistered(crMsg, serverContext)) { return; }
        if (!validateRecipientExists(crMsg, serverContext))  { return; }
        if (checkContactRelationship(crMsg.getFrom(), crMsg.getTo(), serverContext)) {
            AckHelper.sendFailedAck(serverContext, crMsg, "Already contacts");
            return;
        }

        int senderId = crMsg.getFrom();
        int receiverId = crMsg.getTo();

        if (senderId == receiverId) {
            AckHelper.sendFailedAck(serverContext, crMsg, "Cannot add yourself as contact");
            return;
        }

        String requestId = crMsg.getRequestId();

        // Store the pending request for validation later
        pendingRequests.put(requestId, new PendingRequest(senderId, receiverId, System.currentTimeMillis()));

        // Send ACK to sender
        AckHelper.sendSentAck(serverContext, crMsg);

        // Forward to receiver
        serverContext.sendPacketToClient(crMsg.toPacket());

        LOG.info(String.format("[Server] Contact request from %d to %d forwarded", senderId, receiverId));
    }

    /**
     * Handle a response to a contact request.
     */
    private void handleResponse(ContactRequestResponseMessage crMsg, TchatsAppServer.ServerContext serverContext) {
        int responderId = crMsg.getFrom();
        int originalSenderId = crMsg.getTo();
        String requestId = crMsg.getRequestId();
        boolean accepted = crMsg.isAccepted();

        // Validate that this request actually exists and matches
        PendingRequest pending = pendingRequests.get(requestId);
        if (pending == null) {
            LOG.warning(String.format("[Server] Response to unknown request %s from user %d%n", requestId, responderId));
            AckHelper.sendFailedAck(serverContext, crMsg, "Unknown request");
            return;
        }

        // Validate that the responder is the actual receiver
        if (pending.receiverId != responderId) {
            LOG.warning(String.format("[Server] Response to request %s from invalid responder %d (expected %d)%n", requestId, responderId, pending.receiverId));
            AckHelper.sendFailedAck(serverContext, crMsg, "Invalid responder");
            return;
        }

        // Validate that the target is the actual sender
        if (pending.senderId != originalSenderId) {
            LOG.warning(String.format("[Server] Response to request %s targets invalid user %d (expected %d)%n", requestId, originalSenderId, pending.senderId));
            AckHelper.sendFailedAck(serverContext, crMsg, "Invalid target");
            return;
        }

        pendingRequests.remove(requestId);

        if (accepted) {
            UserInfo responder = serverContext.getUserRepository().findById(responderId);
            UserInfo sender = serverContext.getUserRepository().findById(originalSenderId);

            if (responder == null || sender == null) {
                LOG.warning(String.format("[Server] User not found during contact acceptance for request %s%n", requestId));
                AckHelper.sendFailedAck(serverContext, crMsg, "User not found");
                return;
            }

            responder.addContact(originalSenderId);
            sender.addContact(responderId);
            serverContext.getUserRepository().update(responderId, responder);
            serverContext.getUserRepository().update(originalSenderId, sender);

            LOG.info(String.format("[Server] Contact request accepted: users %d and %d are now contacts%n", originalSenderId, responderId));
        } else {
            LOG.info(String.format("[Server] Contact request %s rejected by user %d%n", requestId, responderId));
        }

        // Send ACK to responder
        AckHelper.sendSentAck(serverContext, crMsg);

        // Forward response to original sender
        serverContext.sendPacketToClient(crMsg.toPacket());
    }

    /**
     * Clean up old pending requests (should be called periodically).
     * Requests older than 7 days are removed.
     */
    public void cleanupOldRequests() {
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().timestamp < sevenDaysAgo);
    }
}