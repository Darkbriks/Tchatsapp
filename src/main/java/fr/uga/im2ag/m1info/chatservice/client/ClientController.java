package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.model.UserClient;
import fr.uga.im2ag.m1info.chatservice.client.repository.ContactClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.ConversationClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.GroupClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

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
     * Send a packet to the server.
     *
     * @param packet the packet to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendPacket(Packet packet) {
        return client.sendPacket(packet);
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        client.disconnect();
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
     * Mark the connection as established.
     */
    public void markConnectionEstablished() {
        this.connectionEstablished = true;
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
}