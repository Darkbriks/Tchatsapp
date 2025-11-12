package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.event.system.*;
import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientPacketHandler;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.UserClient;
import fr.uga.im2ag.m1info.chatservice.client.repository.ContactClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.ConversationClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.GroupClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.MessageIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.util.HashSet;

/**
 * Context providing access to client functionalities for packet handlers.
 * This class encapsulates the client and provides controlled access to its operations.
 */
// TODO: Add access to repositories
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
     * Creates a new ClientContext with specified repositories.
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

        // TODO: For testing purposes only, remove later
        this.conversationRepository.add(new ConversationClient("0", new HashSet<>(), false));
    }

    /**
     * Creates a new ClientContext.
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
     * Get the message ID generator.
     *
     * @return the message ID generator
     */
    public MessageIdGenerator getMessageIdGenerator() {
        return client.getMessageIdGenerator();
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