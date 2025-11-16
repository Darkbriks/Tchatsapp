package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.event.system.*;
import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.client.model.*;
import fr.uga.im2ag.m1info.chatservice.client.repository.ContactClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.ConversationClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.GroupClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ContactRequestMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Context providing access to client functionalities for packet handlers.
 * This class encapsulates the client and provides controlled access to its operations.
 */
public class ClientController {
    private final Client client;
    private volatile boolean connectionEstablished;
    private volatile String lastErrorMessage;

    private final ConversationClientRepository conversationRepository;
    private final ContactClientRepository contactRepository;
    private final GroupClientRepository groupRepository;
    private final UserClient activeUser;
    private final EventBus eventBus;

    /* ----------------------- Constructor ----------------------- */

    /**
     * Creates a new ClientController with specified repositories.
     *
     * @param client the client instance to wrap
     * @param conversationRepository the conversation repository
     * @param contactRepository the contact repository
     * @param groupRepository the group repository
     */
    public ClientController(Client client,
                            ConversationClientRepository conversationRepository,
                            ContactClientRepository contactRepository,
                            GroupClientRepository groupRepository,
                            UserClient user) {
        this.client = client;
        this.connectionEstablished = false;
        this.lastErrorMessage = null;
        this.conversationRepository = conversationRepository;
        this.contactRepository = contactRepository;
        this.groupRepository = groupRepository;
        this.activeUser = user;
        this.eventBus = EventBus.getInstance();
    }

    /**
     * Creates a new ClientController.
     *
     * @param client the client instance to wrap
     */
    public ClientController(Client client) {
        this(client, new ConversationClientRepository(),
             new ContactClientRepository(),
             new GroupClientRepository(),
             new UserClient());
    }

    /* ----------------------- Client Operations ----------------------- */

    /**
     * Connect to the server.
     *
     * @param host the server hostname or IP address
     * @param port the server port
     * @param username the username to use for the connection
     * @return true if connected successfully, false otherwise
     * @throws Exception if a network error occurs
     */
    public boolean connect(String host, int port, String username) throws Exception {
        return client.connect(host, port, username);
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        client.disconnect();
    }

    /* ----------------------- Accessors ----------------------- */

    /**
     * Get the client ID.
     *
     * @return the client ID
     */
    public int getClientId() {
        return client.getClientId();
    }

    /**
     * Get the conversation repository.
     *
     * @return the conversation repository
     */
    public ConversationClientRepository getConversationRepository() {
        return conversationRepository;
    }

    /**
     * Get the contact repository.
     *
     * @return the contact repository
     */
    public ContactClientRepository getContactRepository() {
        return contactRepository;
    }

    /**
     * Get the group repository.
     *
     * @return the group repository
     */
    public GroupClientRepository getGroupRepository() {
        return groupRepository;
    }

    /**
     * Get the active user.
     *
     * @return the active user
     */
    public UserClient getActiveUser() {
        return activeUser;
    }

    /**
     * Check if the client is connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Check if the connection handshake is complete.
     *
     * @return true if connection is fully established, false otherwise
     */
    public boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    /**
     * Mark the connection as established.
     */
    public void markConnectionEstablished() {
        this.connectionEstablished = true;
    }

    /**
     * Set the last error message.
     *
     * @param errorMessage the error message
     */
    public void setLastError(String errorMessage) {
        this.lastErrorMessage = errorMessage;
    }

    /**
     * Get the last error message.
     *
     * @return the last error message, or null if none
     */
    public String getLastError() {
        return lastErrorMessage;
    }

    /**
     * Clear the last error message.
     */
    public void clearLastError() {
        this.lastErrorMessage = null;
    }

    /**
     * Update the client ID.
     *
     * @param clientId the new client ID
     */
    public void updateClientId(int clientId) {
        client.updateClientId(clientId);
    }

    /* ----------------------- Conversation Management ----------------------- */

    /**
     * Generate a conversation ID for a private conversation between two users.
     *
     * @param userId1 the first user ID
     * @param userId2 the second user ID
     * @return the conversation ID
     */
    public static String generatePrivateConversationId(int userId1, int userId2) {
        int min = Math.min(userId1, userId2);
        int max = Math.max(userId1, userId2);
        return "private_" + min + "_" + max;
    }

    /**
     * Generate a conversation ID for a group conversation.
     *
     * @param groupId the group ID
     * @return the conversation ID
     */
    public static String generateGroupConversationId(int groupId) {
        return "group_" + groupId;
    }

    /**
     * Get or create a private conversation with another user.
     *
     * @param otherUserId the other user's ID
     * @return the conversation
     */
    public ConversationClient getOrCreatePrivateConversation(int otherUserId) {
        String conversationId = generatePrivateConversationId(getClientId(), otherUserId);
        ConversationClient conversation = conversationRepository.findById(conversationId);

        if (conversation == null) {
            Set<Integer> participants = new HashSet<>();
            participants.add(getClientId());
            participants.add(otherUserId);
            conversation = new ConversationClient(conversationId, participants, false);
            conversationRepository.add(conversation);
        }

        return conversation;
    }

    /**
     * Get or create a group conversation.
     *
     * @param groupId the group ID
     * @param participantIds the participant IDs (including current user)
     * @return the conversation
     */
    public ConversationClient getOrCreateGroupConversation(int groupId, Set<Integer> participantIds) {
        String conversationId = generateGroupConversationId(groupId);
        ConversationClient conversation = conversationRepository.findById(conversationId);

        if (conversation == null) {
            conversation = new ConversationClient(conversationId, participantIds, true);
            conversationRepository.add(conversation);
        }

        return conversation;
    }

    /* ----------------------- Contact Request Management ----------------------- */

    /**
     * Send a contact request to another user.
     *
     * @param targetUserId the user to send the request to
     * @return the request ID, or null if failed
     */
    public String sendContactRequest(int targetUserId) {
        if (contactRepository.isContact(targetUserId)) {
            System.err.println("[Client] User " + targetUserId + " is already a contact");
            return null;
        }

        if (contactRepository.hasSentRequestTo(targetUserId)) {
            System.err.println("[Client] Contact request to user " + targetUserId + " already sent");
            return null;
        }

        ContactRequestMessage crMsg = (ContactRequestMessage) MessageFactory.create(MessageType.CONTACT_REQUEST, getClientId(), targetUserId);

        Instant expiresAt = Instant.now().plus(ContactClientRepository.DEFAULT_REQUEST_EXPIRATION);
        crMsg.setExpirationTimestamp(expiresAt.toEpochMilli());

        ContactRequest request = ContactClientRepository.createRequest(
                crMsg.getRequestId(), getClientId(), targetUserId
        );
        contactRepository.addSentRequest(request);

        sendPacket(crMsg.toPacket());
        return crMsg.getRequestId();
    }

    /**
     * Respond to a contact request.
     *
     * @param senderId the ID of the user who sent the request
     * @param accept true to accept, false to reject
     */
    public void respondToContactRequest(int senderId, boolean accept) {
        ContactRequest request = contactRepository.getReceivedRequestFrom(senderId);
        if (request == null) {
            System.err.println("[Client] No pending request from user " + senderId);
            return;
        }

        if (request.isExpired()) {
            System.err.println("[Client] Request from user " + senderId + " has expired");
            contactRepository.removeRequest(request.getRequestId());
            return;
        }

        ContactRequestMessage response = (ContactRequestMessage) MessageFactory.create(MessageType.CONTACT_REQUEST_RESPONSE, getClientId(), senderId);
        response.setRequestId(request.getRequestId());
        response.setResponse(true);
        response.setAccepted(accept);

        request.setStatus(accept ? ContactRequest.Status.ACCEPTED : ContactRequest.Status.REJECTED);
        contactRepository.removeRequest(request.getRequestId());

        if (accept) {
            ContactClient newContact = new ContactClient(senderId, "User #" + senderId);
            contactRepository.add(newContact);
            getOrCreatePrivateConversation(senderId);

            ManagementMessage updateMsg = (ManagementMessage) MessageFactory.create(MessageType.UPDATE_PSEUDO, getClientId(), 0);
            updateMsg.addParam("newPseudo", getActiveUser().getPseudo());
            sendPacket(updateMsg.toPacket());
        }

        sendPacket(response.toPacket());
    }

    /* ----------------------- Event Subscription ----------------------- */

    /**
     * Subscribe to an event type with an observer, filter, and execution mode.
     *
     * @param eventType the class of the event type to subscribe to
     * @param observer the observer to notify when the event occurs
     * @param filter the filter to apply to events
     * @param mode the execution mode for the observer
     * @param <T> the type of the event
     * @return the event subscription
     */
    public <T extends Event> EventSubscription<T> subscribeToEvent(Class<T> eventType, EventObserver<T> observer, EventFilter<T> filter, ExecutionMode mode) {
        return eventBus.subscribe(eventType, observer, filter, mode);
    }

    /**
     * Subscribe to an event type with an observer and an execution mode.
     *
     * @param eventType the class of the event type to subscribe to
     * @param observer the observer to notify when the event occurs
     * @param mode the execution mode for the observer
     * @param <T> the type of the event
     * @return the event subscription
     */
    public <T extends Event> EventSubscription<T> subscribeToEvent(Class<T> eventType, EventObserver<T> observer, ExecutionMode mode) {
        return eventBus.subscribe(eventType, observer, mode);
    }

    /**
     * Subscribe to an event type with an observer using synchronous execution mode.
     *
     * @param eventType the class of the event type to subscribe to
     * @param observer the observer to notify when the event occurs
     * @param <T> the type of the event
     * @return the event subscription
     */
    public <T extends Event> EventSubscription<T> subscribeToEvent(Class<T> eventType, EventObserver<T> observer) {
        return eventBus.subscribe(eventType, observer, ExecutionMode.SYNC);
    }

    public void unsubscribe(EventSubscription<? extends Event> subscription) {
        eventBus.unsubscribe(subscription);
    }

    /**
     * Publish an event to the event bus.
     * Can only be called by ClientPacketHandler instances.
     * The token is used to simulate C++-style friend access control.
     *
     * @param event the event to publish
     * @param token the publish event
     */
    public void publishEvent(Event event, ClientPacketHandler.PublishEventToken token) {
        if (!token.isValidFor(this)) {
            throw new IllegalArgumentException("Invalid PublishEventToken for this ClientController");
        }

        eventBus.publish(event);
    }

    /* ----------------------- Packet Operations ----------------------- */

    /**
     * Send a packet to the server.
     *
     * @param packet the packet to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendPacket(Packet packet) {
        return client.sendPacket(packet);
    }

    /**
     * Send a text message to a recipient.
     *
     * @param msg the message content
     * @param to the recipient ID
     */
    public void sendMedia(String msg, int to) {
        client.sendMedia(msg, to);
    }
}