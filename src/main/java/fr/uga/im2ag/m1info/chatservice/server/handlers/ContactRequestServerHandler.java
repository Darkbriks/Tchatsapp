package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ContactRequestMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;
import fr.uga.im2ag.m1info.chatservice.server.util.AckHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for contact request messages.
 */
public class ContactRequestServerHandler extends ServerPacketHandler {
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
        if (!(message instanceof ContactRequestMessage crMsg)) {
            throw new IllegalArgumentException("Invalid message type for ContactRequestServerHandler");
        }

        if (crMsg.isResponse()) {
            handleResponse(crMsg, serverContext);
        } else {
            handleRequest(crMsg, serverContext);
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
        int senderId = crMsg.getFrom();
        int receiverId = crMsg.getTo();
        String requestId = crMsg.getRequestId();

        // Validate sender exists
        UserInfo sender = serverContext.getUserRepository().findById(senderId);
        if (sender == null) {
            System.err.printf("[Server] Contact request from non-existent user %d%n", senderId);
            AckHelper.sendFailedAck(serverContext, crMsg, "Sender not found");
            return;
        }

        // Validate receiver exists
        if (serverContext.getUserRepository().findById(receiverId) == null) {
            System.err.printf("[Server] Contact request to non-existent user %d%n", receiverId);
            AckHelper.sendFailedAck(serverContext, crMsg, "Recipient not found");
            return;
        }

        // Validate not already contacts
        if (sender.hasContact(receiverId)) {
            System.err.printf("[Server] Users %d and %d are already contacts%n", senderId, receiverId);
            AckHelper.sendFailedAck(serverContext, crMsg, "Already contacts");
            return;
        }

        // Store the pending request for validation later
        pendingRequests.put(requestId, new PendingRequest(senderId, receiverId, System.currentTimeMillis()));

        // Send ACK to sender
        AckHelper.sendSentAck(serverContext, crMsg);

        // Forward to receiver
        serverContext.sendPacketToClient(crMsg.toPacket());

        System.out.printf("[Server] Contact request from %d to %d forwarded%n", senderId, receiverId);
    }

    /**
     * Handle a response to a contact request.
     */
    private void handleResponse(ContactRequestMessage crMsg, TchatsAppServer.ServerContext serverContext) {
        int responderId = crMsg.getFrom();
        int originalSenderId = crMsg.getTo();
        String requestId = crMsg.getRequestId();
        boolean accepted = crMsg.isAccepted();

        // Validate that this request actually exists and matches
        PendingRequest pending = pendingRequests.get(requestId);
        if (pending == null) {
            System.err.printf("[Server] Response to unknown request %s from user %d%n", requestId, responderId);
            AckHelper.sendFailedAck(serverContext, crMsg, "Unknown request");
            return;
        }

        // Validate that the responder is the actual receiver
        if (pending.receiverId != responderId) {
            System.err.printf("[Server] User %d attempted to respond to request meant for user %d (SPOOFING ATTEMPT)%n",
                    responderId, pending.receiverId);
            AckHelper.sendFailedAck(serverContext, crMsg, "Invalid responder");
            return;
        }

        // Validate that the target is the actual sender
        if (pending.senderId != originalSenderId) {
            System.err.printf("[Server] Response targets wrong user %d instead of %d%n",
                    originalSenderId, pending.senderId);
            AckHelper.sendFailedAck(serverContext, crMsg, "Invalid target");
            return;
        }

        pendingRequests.remove(requestId);

        if (accepted) {
            UserInfo responder = serverContext.getUserRepository().findById(responderId);
            UserInfo sender = serverContext.getUserRepository().findById(originalSenderId);

            if (responder == null || sender == null) {
                System.err.printf("[Server] User not found during contact acceptance%n");
                AckHelper.sendFailedAck(serverContext, crMsg, "User not found");
                return;
            }

            responder.addContact(originalSenderId);
            sender.addContact(responderId);
            serverContext.getUserRepository().update(responderId, responder);
            serverContext.getUserRepository().update(originalSenderId, sender);

            System.out.printf("[Server] Contact request accepted: users %d and %d are now contacts%n",
                    originalSenderId, responderId);
        } else {
            System.out.printf("[Server] Contact request rejected by user %d%n", responderId);
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