package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.command.*;
import fr.uga.im2ag.m1info.chatservice.client.encryption.ClientEncryptionService;
import fr.uga.im2ag.m1info.chatservice.client.event.system.*;
import fr.uga.im2ag.m1info.chatservice.client.event.types.ErrorEvent;
import fr.uga.im2ag.m1info.chatservice.client.handlers.ClientHandlerContext;
import fr.uga.im2ag.m1info.chatservice.client.handlers.KeyExchangeHandler;
import fr.uga.im2ag.m1info.chatservice.client.model.*;
import fr.uga.im2ag.m1info.chatservice.client.processor.DecryptingPacketProcessor;
import fr.uga.im2ag.m1info.chatservice.client.repository.ContactClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.ConversationClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.GroupClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.*;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.*;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.KeyExchangeManager;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    private ClientEncryptionService encryptionService;
    private KeyExchangeHandler keyExchangeHandler;

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
        if (encryptionService != null) {
            encryptionService.shutdown();
        }

        client.disconnect();
        connectionEstablished = false;
    }

    /**
     * Initializes the encryption service.
     * <p>
     * Must be called before initializing handlers,
     * or never if encryption is not desired.
     */
    public void initializeEncryption() {
        int clientId = getClientId();

        this.encryptionService = new ClientEncryptionService(clientId);

        if (clientId > 0) {
            encryptionService.setMessageSender(this::sendKeyExchangeMessage);
            encryptionService.start();
        }

        System.out.println("[Client] Encryption service initialized for client " + clientId);
    }

    /**
     * Initializes packet handlers and the packet processor.
     * <p>
     * Must be called after encryption initialization if encryption is desired.
     */
    public void initializeHandlers() {
        ClientHandlerContext handlerContext = ClientHandlerContext.builder()
                .commandManager(client.getCommandManager())
                .build();

        ClientPaquetRouter router = ClientPaquetRouter.createWithServiceLoader(
                this,
                handlerContext
        );


        if (encryptionService != null) {
            keyExchangeHandler = new KeyExchangeHandler();
            keyExchangeHandler.setEncryptionService(encryptionService);
            router.addHandler(keyExchangeHandler);
        }

        PacketProcessor processor = createPacketProcessor(router);

        client.setPacketProcessor(processor);
    }

    private PacketProcessor createPacketProcessor(ClientPaquetRouter router) {
        if (encryptionService != null && encryptionService.isEncryptionEnabled()) {
            DecryptingPacketProcessor decryptingProcessor = new DecryptingPacketProcessor(router, encryptionService.getEncryptionStrategy());
            decryptingProcessor.setErrorCallback((senderId, message, cause) -> publishEvent(new ErrorEvent(this, ErrorEvent.ErrorLevel.ERROR, "DECRYPTION_ERROR", "Decryption error from " + senderId + ": " + message)));
            return decryptingProcessor;
        } else {
            return router;
        }
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
     * Get the pending command manager.
     *
     * @return the pending command manager
     */
    public PendingCommandManager getCommandManager() {
        return client.getCommandManager();
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

        if (encryptionService != null) {
            encryptionService.updateClientId(clientId);
            encryptionService.setMessageSender(this::sendKeyExchangeMessage);

            // Start the service if not already started
            if (!encryptionService.isRunning()) {
                encryptionService.start();
            }

            // Update the handler
            if (keyExchangeHandler != null) {
                keyExchangeHandler.setEncryptionService(encryptionService);
            }
        }
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
            if (encryptionService != null) {
                encryptionService.initiateSecureConversation(otherUserId);
            }
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

        ContactRequestResponseMessage response = (ContactRequestResponseMessage) MessageFactory.create(MessageType.CONTACT_REQUEST_RESPONSE, getClientId(), senderId);
        response.setRequestId(request.getRequestId());
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
     *
     * @param event the event to publish
     */
    public void publishEvent(Event event) {
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

    /* ----------------------- ACK System Message Sending ----------------------- */

    /**
     * Sends a protocol message, encrypting it if appropriate.
     * <p>
     * This is a convenience method that handles encryption transparently.
     *
     * @param message the message to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendEncryptedMessage(ProtocolMessage message) {
        System.out.println("[Client] Encryption of " + message);
        ProtocolMessage messageToSend = message;

        if (encryptionService != null && encryptionService.shouldEncrypt(message)) {
            try {
                System.out.println("[Client] Encrypting message");
                messageToSend = encryptionService.prepareForSending(message);
            } catch (GeneralSecurityException e) {
                System.err.println("[Client] Encryption failed: " + e.getMessage());
                return false;
            }
        }
        System.out.println("[Client] Sending " + messageToSend);
        return sendPacket(messageToSend.toPacket());
    }

    /**
     * Send a text message to a recipient using the ACK system.
     *
     * @param content the message content
     * @param toUserId the recipient ID
     * @return the message ID, or null if failed
     */
    public String sendTextMessage(String content, int toUserId) {
        TextMessage textMsg = (TextMessage) MessageFactory.create(
                MessageType.TEXT,
                getClientId(),
                toUserId
        );
        textMsg.setContent(content);

        if (!sendEncryptedMessage(textMsg)) {
            System.err.println("[Client] Failed to send text message to user " + toUserId);
            return null;
        }

        Message msg = new Message(
                textMsg.getMessageId(),
                getClientId(),
                toUserId,
                content,
                textMsg.getTimestamp(),
                null
        );

        ConversationClient conversation = getOrCreatePrivateConversation(toUserId);
        conversation.addMessage(msg);

        SendTextMessageCommand command = new SendTextMessageCommand(
                textMsg.getMessageId(),
                msg,
                conversationRepository
        );

        client.getCommandManager().addPendingCommand(command);
        return textMsg.getMessageId();
    }

    /**
     * Send a management message using the ACK system.
     *
     * @param messageType the type of management message
     * @param toUserId the recipient ID (usually 0 for server)
     * @return the created ManagementMessage, or null if failed
     */
    public ManagementMessage sendManagementMessage(MessageType messageType, int toUserId) {
        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                messageType,
                getClientId(),
                toUserId
        );

        SendManagementMessageCommand command = new SendManagementMessageCommand(
                mgmtMsg.getMessageId(),
                messageType
        );

        client.getCommandManager().addPendingCommand(command);

        return mgmtMsg;
    }

    /**
     * Update the user's pseudo using the ACK system.
     *
     * @param newPseudo the new pseudo
     * @return true if the request was sent, false otherwise
     */
    public boolean updatePseudo(String newPseudo) {
        if (newPseudo == null || newPseudo.isEmpty()) {
            System.err.println("[Client] New pseudo cannot be null or empty");
            return false;
        }

        ManagementMessage mgmtMsg = sendManagementMessage(MessageType.UPDATE_PSEUDO, 0);
        if (mgmtMsg == null) {
            return false;
        }

        mgmtMsg.addParam("newPseudo", newPseudo);
        sendPacket(mgmtMsg.toPacket());

        client.getCommandManager().addPendingCommand(new UpdatePseudoCommand(
                mgmtMsg.getMessageId(),
                newPseudo,
                activeUser
        ));

        return true;
    }

    /**
     * Remove a contact using the ACK system.
     *
     * @param contactId the contact ID to remove
     * @return true if the request was sent, false otherwise
     */
    public boolean removeContact(int contactId) {
        if (!contactRepository.isContact(contactId)) {
            System.err.println("[Client] User " + contactId + " is not a contact");
            return false;
        }

        ManagementMessage mgmtMsg = sendManagementMessage(MessageType.REMOVE_CONTACT, 0);
        if (mgmtMsg == null) {
            return false;
        }

        mgmtMsg.addParam("contactId", contactId);
        sendPacket(mgmtMsg.toPacket());

        client.getCommandManager().addPendingCommand(new RemoveContactCommand(
                mgmtMsg.getMessageId(),
                contactId,
                contactRepository
        ));

        return true;
    }

    /**
     * Creat a new group and user is the admin 
     *
     * @param name the desired name of the group 
     * @return true if the request was sent, false otherwise
     */
    public boolean createGroup(String name) {
        if (name == null || name.isEmpty()) {
            System.err.println("[Client] Group name cannot be null or empty");
            return false;
        }

        ManagementMessage mgmtMsg = sendManagementMessage(MessageType.CREATE_GROUP, 0);
        if (mgmtMsg == null) {
            return false;
        }

        mgmtMsg.addParam(KeyInMessage.GROUP_NAME, name);
        sendPacket(mgmtMsg.toPacket());
        client.getCommandManager().addPendingCommand(new CreateGroupCommand(
                mgmtMsg.getMessageId(),
                name,
                groupRepository 
        ));

        return true;
    }

    /**
     * Leave a group 
     *
     * @param groupID the id of the group to leave 
     * @return true if the request was sent, false otherwise
     */
    public boolean leaveGroup(int groupID) {

        ManagementMessage mgmtMsg = sendManagementMessage(MessageType.LEAVE_GROUP, groupID);
        if (mgmtMsg == null) {
            return false;
        }

        mgmtMsg.addParam(KeyInMessage.GROUP_ID, groupID);

        sendPacket(mgmtMsg.toPacket());
        client.getCommandManager().addPendingCommand(new LeaveGroupCommand(
                mgmtMsg.getMessageId(),
                groupID,
                groupRepository
        ));

        return true;
    }

    /**
     * Add member to a group, Need to be the admin of the group for this work
     *
     * @param groupID the id of the group 
     * @param newMember the member to add to the group
     * @return true if the request was sent, false otherwise
     */
    public boolean addMemberToGroup(int groupID, int newMember) {
        /* We send the message and the server handle we are not the admin */

        ManagementMessage mgmtMsg = sendManagementMessage(MessageType.ADD_GROUP_MEMBER, groupID);
        if (mgmtMsg == null) {
            return false;
        }

        mgmtMsg.addParam(KeyInMessage.MEMBER_ADD_ID, newMember);

        sendPacket(mgmtMsg.toPacket());

        client.getCommandManager().addPendingCommand(new AddMemberGroupCommand(
                mgmtMsg.getMessageId(),
                groupID,
                groupRepository,
                newMember
        ));
        return true;
    }

    /**
     * Remove a member from a group, Need to be the admin of the group for this work
     *
     * @param groupID the id of the group 
     * @param deleteMember the member to add to the group
     * @return true if the request was sent, false otherwise
     */
    public boolean removeMemberToGroup(int groupID, int deleteMember) {
        /* We send the message and the server handle we are not the admin */

        ManagementMessage mgmtMsg = sendManagementMessage(MessageType.REMOVE_GROUP_MEMBER, groupID);
        if (mgmtMsg == null) {
            return false;
        }

        mgmtMsg.addParam(KeyInMessage.MEMBER_REMOVE_ID, deleteMember);

        sendPacket(mgmtMsg.toPacket());
        client.getCommandManager().addPendingCommand(new RemoveMemberGroupCommand(
                mgmtMsg.getMessageId(),
                groupID,
                groupRepository,
                deleteMember
        ));
        return true;
    }

    public void sendAck(ProtocolMessage originalMessage, MessageStatus ackType) {
        client.sendAck(originalMessage, ackType);
    }

    /**
     * Sends a key exchange message.
     * <p>
     * Used as callback for the KeyExchangeManager.
     *
     * @param data the key exchange message data
     */
    private void sendKeyExchangeMessage(KeyExchangeManager.KeyExchangeMessageData data) {
        ProtocolMessage message;

        if (data.isResponse()) {
            KeyExchangeResponseMessage response = (KeyExchangeResponseMessage) MessageFactory.create(
                    MessageType.KEY_EXCHANGE_RESPONSE,
                    data.getFromId(),
                    data.getToId()
            );
            response.setPublicKey(data.getPublicKey());
            message = response;
        } else {
            KeyExchangeMessage request = (KeyExchangeMessage) MessageFactory.create(
                    MessageType.KEY_EXCHANGE,
                    data.getFromId(),
                    data.getToId()
            );
            request.setPublicKey(data.getPublicKey());
            message = request;
        }

        sendPacket(message.toPacket());
    }

    /**
     * Initiates a secure conversation with a peer.
     * <p>
     * Performs key exchange if no session exists. The returned future completes
     * when the session is established.
     *
     * @param peerId the peer ID
     * @return a future that completes with true if successful
     */
    public CompletableFuture<Boolean> initiateSecureConversation(int peerId) {
        if (encryptionService == null) {
            return CompletableFuture.completedFuture(false);
        }
        return encryptionService.initiateSecureConversation(peerId);
    }

    /**
     * Checks if a secure session exists with a peer.
     *
     * @param peerId the peer ID
     * @return true if encrypted communication is possible
     */
    public boolean hasSecureSession(int peerId) {
        return encryptionService != null && encryptionService.hasSecureSession(peerId);
    }

    /**
     * Gets the encryption service.
     * <p>
     * For advanced use cases.
     *
     * @return the encryption service, or null if not initialized
     */
    public ClientEncryptionService getEncryptionService() {
        return encryptionService;
    }
}
