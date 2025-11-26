package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.event.types.*;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactRequest;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;
import fr.uga.im2ag.m1info.chatservice.common.MessageStatus;
import fr.uga.im2ag.m1info.chatservice.common.model.GroupInfo;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Command-line interface for the TchatsApp client.
 * Provides a text-based menu to interact with the chat service.
 */
public class CliClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1666;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ClientController clientController;
    private final Scanner scanner;

    /**
     * Creates a new CLI client.
     */
    CliClient(int clientId, Scanner scanner) {
        Client client = new Client(clientId);
        this.clientController = new ClientController(client);
        this.clientController.initializeEncryption();
        this.clientController.initializeHandlers();
        this.scanner = scanner;
        registerEventListeners();
    }

    public static CliClient createClient() {
        System.out.println("=== TchatsApp CLI Client ===\n");
        System.out.println("Your client ID? (0 to create a new account)");
        Scanner scanner = new Scanner(System.in);
        int clientId;
        try {
            clientId = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid input. Please enter a number.");
            scanner.nextLine();
            clientId = 0;
        }

        return new CliClient(clientId, scanner);
    }

    /**
     * Register event listeners for all client events.
     */
    private void registerEventListeners() {
        // Connection events
        clientController.subscribeToEvent(
                ConnectionEstablishedEvent.class,
                this::onConnectionEstablished
        );

        // Message events
        clientController.subscribeToEvent(
                TextMessageReceivedEvent.class,
                this::onTextMessageReceived
        );

        clientController.subscribeToEvent(
                MediaMessageReceivedEvent.class,
                this::onMediaMessageReceived
        );

        // Contact events
        clientController.subscribeToEvent(
                ContactAddedEvent.class,
                this::onContactAdded
        );

        clientController.subscribeToEvent(
                ContactRemovedEvent.class,
                this::onContactRemoved
        );

        clientController.subscribeToEvent(
                ContactUpdatedEvent.class,
                this::onContactUpdated
        );

        // User events
        clientController.subscribeToEvent(
                UserPseudoUpdatedEvent.class,
                this::onUserPseudoUpdated
        );

        // Error events
        clientController.subscribeToEvent(
                ErrorEvent.class,
                this::onError
        );

        // Contact request events
        clientController.subscribeToEvent(
                ContactRequestReceivedEvent.class,
                this::onContactRequestReceived
        );

        // Contact request response events
        clientController.subscribeToEvent(
                ContactRequestResponseEvent.class,
                this::onContactRequestResponse
        );

        // ACK System events
        clientController.subscribeToEvent(
                MessageStatusChangedEvent.class,
                this::onMessageStatusChanged
        );

        clientController.subscribeToEvent(
                ManagementOperationSucceededEvent.class,
                this::onManagementOperationSucceeded
        );

        clientController.subscribeToEvent(
                ManagementOperationFailedEvent.class,
                this::onManagementOperationFailed
        );
    }

    /* ----------------------- Event Callbacks ----------------------- */

    private void onConnectionEstablished(ConnectionEstablishedEvent event) {
        System.out.println("\n=== Connection Established ===");
        if (event.isNewUser()) {
            System.out.println("✓ New account created!");
        } else {
            System.out.println("✓ Welcome back!");
        }
        System.out.println("Client ID: " + event.getClientId());
        System.out.println("Pseudo: " + event.getPseudo());
        System.out.println("==============================\n");
    }

    private void onTextMessageReceived(TextMessageReceivedEvent event) {
        Message msg = event.getMessage();
        String conversationId = event.getConversationId();

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║         NEW TEXT MESSAGE RECEIVED              ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║ Conversation: " + conversationId);
        System.out.println("║ From: User #" + msg.getFromUserId());
        System.out.println("║ To: User #" + msg.getToUserId());
        System.out.println("║ Time: " + TIME_FORMATTER.format(msg.getTimestamp()));
        if (msg.getReplyToMessageId() != null) {
            System.out.println("║ Reply to: " + msg.getReplyToMessageId().substring(0, 8) + "...");
        }
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║ " + msg.getContent());
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }

    private void onMediaMessageReceived(MediaMessageReceivedEvent event) {
        Message msg = event.getMessage();
        String conversationId = event.getConversationId();

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║        NEW MEDIA MESSAGE RECEIVED              ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║ Conversation: " + conversationId);
        System.out.println("║ From: User #" + msg.getFromUserId());
        System.out.println("║ To: User #" + msg.getToUserId());
        System.out.println("║ Time: " + TIME_FORMATTER.format(msg.getTimestamp()));
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║ " + msg.getContent());
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }

    private void onContactAdded(ContactAddedEvent event) {
        ContactClient contact = clientController.getContactRepository().findById(event.getContactId());
        System.out.println("\n✓ Contact added: " +
                (contact != null ? contact.getPseudo() : "User #" + event.getContactId()));
    }

    private void onContactRemoved(ContactRemovedEvent event) {
        System.out.println("\n✓ Contact removed: User #" + event.getContactId());
    }

    private void onContactUpdated(ContactUpdatedEvent event) {
        ContactClient contact = clientController.getContactRepository().findById(event.getContactId());
        if (contact != null) {
            System.out.println("\n✓ Contact updated: " + contact.getPseudo() + " (User #" + event.getContactId() + ")");
        }
    }

    private void onUserPseudoUpdated(UserPseudoUpdatedEvent event) {
        System.out.println("\n✓ Your pseudo has been updated to: " + event.getNewPseudo());
    }

    private void onError(ErrorEvent event) {
        System.err.println("\n╔════════════════════════════════════════════════╗");
        System.err.println("║                    ERROR                       ║");
        System.err.println("╠════════════════════════════════════════════════╣");
        System.err.println("║ Level: " + event.getErrorLevel());
        System.err.println("║ Type: " + event.getErrorType());
        System.err.println("║ Message: " + event.getErrorMessage());
        System.err.println("╚════════════════════════════════════════════════╝\n");
    }

    private void onContactRequestReceived(ContactRequestReceivedEvent event) {
        System.out.println("\nContact request received from user #" + event.getSenderId());
        System.out.println("Use menu option to accept/reject");
    }

    private void onContactRequestResponse(ContactRequestResponseEvent event) {
        if (event.wasSentByUs()) {
            if (event.isAccepted()) {
                System.out.println("\nContact request accepted by user #" + event.getOtherUserId());
            } else {
                System.out.println("\nContact request rejected by user #" + event.getOtherUserId());
            }
        }
    }

    private void onMessageStatusChanged(MessageStatusChangedEvent event) {
        String statusIcon = switch (event.getNewStatus()) {
            case SENDING -> "⏳";
            case SENT -> "📤";
            case DELIVERED -> "📬";
            case READ -> "📖";
            case FAILED -> "❌";
            case CRITICAL_FAILURE -> "⚠️";
        };

        String msgIdShort = event.getMessageId().substring(0, Math.min(8, event.getMessageId().length()));
        System.out.printf("%s Message %s: %s%n", statusIcon, msgIdShort, event.getNewStatus());

        if (event.getNewStatus() == MessageStatus.FAILED && event.getErrorReason() != null) {
            System.err.println("   Reason: " + event.getErrorReason());
        }
    }

    private void onManagementOperationSucceeded(ManagementOperationSucceededEvent event) {
        System.out.println("\n✓ Management operation succeeded: " + event.getOperationType());
    }

    private void onManagementOperationFailed(ManagementOperationFailedEvent event) {
        System.err.println("\n✗ Management operation failed: " + event.getOperationType());
        System.err.println("   Reason: " + event.getReason());
    }

    /* ----------------------- Connection ----------------------- */

    /**
     * Connect to the server with credentials.
     *
     * @return true if connection initiated successfully, false otherwise
     */
    public boolean connect() {
        String pseudo = "";
        if (clientController.getClientId() == 0) {
            System.out.println("Choose your username:");
            pseudo = scanner.nextLine().trim();
        }
        try {
            return clientController.connect(DEFAULT_HOST, DEFAULT_PORT, pseudo);
        } catch (Exception e) {
            System.err.println("[Client] Connection error: " + e.getMessage());
            return false;
        }
    }

    /* ----------------------- Menu ----------------------- */

    /** 
     * Display the group menu.
     */
    private void displayGroupMenu() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║             TCHATSAPP GROUP MENU               ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║ 1. Create a group                              ║");
        System.out.println("║ 2. Leave a group                               ║");
        System.out.println("║ 3. Add member ( admin only )                   ║");
        System.out.println("║ 4. Remove member ( admin only )                ║");
        System.out.println("║ 5. Change Group name ( admin only )            ║");
        System.out.println("║ 6. Delete a Group ( admin only )               ║");
        System.out.println("║ 0. Back to Main menu                           ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.print("Your choice: ");
    }

    /**
     * Display the main menu.
     */
    private void displayMenu() {
        System.out.println("\n╔═════════════════════════════════════════════════╗");
        System.out.println("║               TCHATSAPP MAIN MENU               ║");
        System.out.println("╠═════════════════════════════════════════════════╣");
        System.out.println("║  1. Send a message                              ║");
        System.out.println("║  2. Send contact request                        ║");
        System.out.println("║  3. View pending contact requests               ║");
        System.out.println("║  4. Accept/Reject contact request               ║");
        System.out.println("║  5. Remove a contact                            ║");
        System.out.println("║  6. Change your username                        ║");
        System.out.println("║  7. Group gestion                               ║");
        System.out.println("║  8. List contacts                               ║");
        System.out.println("║  9. List groups                                 ║");
        System.out.println("║ 10. List conversations                          ║");
        System.out.println("║ 11. View conversation history                   ║");
        System.out.println("║  0. Quit                                        ║");
        System.out.println("╚═════════════════════════════════════════════════╝");
        System.out.print("Your choice: ");
    }

    /* ----------------------- Actions ----------------------- */

    /**
     * Handle sending a message.
     */
    private void handleSendMessage() {
        System.out.print("Recipient ID: ");
        int to;
        try {
            to = readIntegerFromUser("Invalid ID.");
        } catch (Exception e) {
            return;
        }

        System.out.print("Your message (/file <path> for file, /reply <msgId> <text> for reply): ");
        String msg = scanner.nextLine();

        if(!msg.isEmpty() && msg.startsWith("/reply")){
            String[] parts = msg.split(" ", 3);
            if(parts.length >= 3) {
                String messageId = parts[1];
                String reaction = parts[2];
                clientController.sendReactionMessage(reaction, to, messageId);
            } else {
                System.out.println("Correct format : /reply <messageId> <reaction>");
            }
        } else if (msg.startsWith("/file ")) {
            String filePath = msg.substring(6).trim();
            if (!filePath.isEmpty()) {
                clientController.sendFile(filePath, to);
            } else {
                System.err.println("File path cannot be empty.");
            }
        } else {
            clientController.sendTextMessage(msg, to);
        }
    }

    /**
     * Read an integer from user
     *
     * @param errorMessage Error message to display if invalid input 
     * @return The integer user provide 
     * @throws throw e; 
     */
    private int readIntegerFromUser(String errorMessage) throws IOException{
        int result = 0;
        try {
            result = scanner.nextInt();
            scanner.nextLine();
        // TODO: Fix (non integer value trigger this case)
        /*} catch (NoSuchElementException e){
            System.out.println("Control D catch ");
            System.exit(1);*/
        } catch (Exception e) {
            System.err.println("Invalid ID.");
            scanner.nextLine();
            throw e;
        }
        return result;
    }

    /*
     * Handle removing a contact.
     */
    private void handleRemoveContact() {
        System.out.print("Contact ID to remove: ");
        int contactId;
        try {
            contactId = readIntegerFromUser("Invalid ID.");
        } catch (Exception e) {
            return;
        }

        clientController.removeContact(contactId);
    }

    /**
     * Handle changing the user's username.
     */
    private void handleUpdatePseudo() {
        System.out.print("New username: ");
        String newPseudo = scanner.nextLine().trim();

        if (newPseudo.isEmpty()) {
            System.err.println("Username cannot be empty.");
            return;
        }

        clientController.updatePseudo(newPseudo);
    }

    /**
     * Handle creating a group 
     */
    private void handleCreateGroup() {
        System.out.print("Group name: ");
        String groupName = scanner.nextLine().trim();

        if (groupName.isEmpty()) {
            System.err.println("GroupName cannot be empty.");
            return;
        }

        clientController.createGroup(groupName);
    }

    /**
     * Handle destroy a group 
     */
    private void handleDestroyGroup() {
        System.out.print("Group id: ");
        int groupId;
        try {
            groupId= readIntegerFromUser("Invalid group ID.");
        } catch (Exception e) {
            return;
        }
        clientController.deleteGroup(groupId);
    }

    private void handleChangeGroupName() {
        System.out.print("Group id: ");
        int groupId;
        try {
            groupId= readIntegerFromUser("Invalid group ID.");
        } catch (Exception e) {
            return;
        }
        System.out.print("new group name: ");
        String groupName = scanner.nextLine().trim();

        if (groupName.isEmpty()) {
            System.err.println("GroupName cannot be empty.");
            return;
        }

        clientController.renameGroup(groupName, groupId);
    }

    /**
     * Handle Leaving a group 
     */
    private void handleLeaveGroup() {
        System.out.print("Group id: ");
        int groupId;
        try {
            groupId= readIntegerFromUser("Invalid group ID.");
        } catch (Exception e) {
            return;
        }

        clientController.leaveGroup(groupId);
    }

    /**
     * Handle add member to a group 
     */
    private void handleAddMemberGroup() {
        System.out.print("Group id: ");
        int groupId;
        int newMember;
        try {
            groupId= readIntegerFromUser("Invalid group ID.");
            System.out.print("Member id: ");
            newMember = readIntegerFromUser("Invalid member ID.");
        } catch (Exception e) {
            return;
        }
        clientController.addMemberToGroup(groupId, newMember);

    }

    /**
     * Handle remove member to a group 
     */
    private void handleRemoveMemberGroup() {
        System.out.print("Group id: ");
        int groupId;
        int deleteMember;
        try {
            groupId= readIntegerFromUser("Invalid group ID.");
        System.out.print("Member id: ");
            deleteMember = readIntegerFromUser("Invalid member ID.");
        } catch (Exception e) {
            return;
        }

        clientController.removeMemberToGroup(groupId, deleteMember);
    }

    /*
     * Get choice from user about group and dispatch event
     */
    private void groupGestion(){
        displayGroupMenu();
        int action;
        try {
            action = readIntegerFromUser("Invalid action");
        } catch (Exception e) {
            groupGestion();
            return;
        }

        if (action == 0) {
            return;
        }

        switch (action) {
            case 1 -> handleCreateGroup();
            case 2 -> handleLeaveGroup();
            case 3 -> handleAddMemberGroup();
            case 4 -> handleRemoveMemberGroup();
            case 5 -> handleChangeGroupName();
            case 6 -> handleDestroyGroup();
            default -> { 
                System.out.println("Invalid choice. Please try again.");
                groupGestion();
            }

        }

    }


    /**
     * List all groups.
     */
    private void handleListGroup() {
        var groups = clientController.getGroupRepository().findAll();

        if (groups.isEmpty()) {
            System.out.println("\nNo groups yet.");
            return;
        }

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                 YOUR GROUPS                    ║");
        System.out.println("╠════════════════════════════════════════════════╣");

        for (GroupInfo group : groups) {
            System.out.println("║ ID: " + group.getGroupId());
            System.out.println("║ NAME: " + group.getGroupName());
            for (var entry : group.getMembers().entrySet()) {
                int memberId = entry.getKey();
                String memberName = entry.getValue();
                if (memberName.isEmpty()) {
                    System.out.println("║ MEMBER_ID: " + memberId);
                } else {
                    System.out.println("║ MEMBER_NAME: " + memberName + " (ID: " + memberId + ")");
                }
            }
            System.out.println("╚════════════════════════════════════════════════╝");
        }
    }

    /**
     * List all contacts.
     */
    private void handleListContacts() {
        var contacts = clientController.getContactRepository().findAll();

        if (contacts.isEmpty()) {
            System.out.println("\nNo contacts yet.");
            return;
        }

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                 YOUR CONTACTS                  ║");
        System.out.println("╠════════════════════════════════════════════════╣");

        for (ContactClient contact : contacts) {
            System.out.println("║ ID: " + contact.getContactId() +
                    " | Pseudo: " + contact.getPseudo());
            if (contact.getLastSeen() != null) {
                System.out.println("║   Last seen: " +
                        DATE_TIME_FORMATTER.format(contact.getLastSeen()));
            }
        }

        System.out.println("╚════════════════════════════════════════════════╝");
    }

    /**
     * List all conversations.
     */
    private void handleListConversations() {
        var conversations = clientController.getConversationRepository().findAll();

        if (conversations.isEmpty()) {
            System.out.println("\nNo conversations yet.");
            return;
        }

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║              YOUR CONVERSATIONS                ║");
        System.out.println("╠════════════════════════════════════════════════╣");

        for (ConversationClient conv : conversations) {
            System.out.println("║ ID: " + conv.getConversationId());
            System.out.println("║ Name: " + conv.getConversationName());
            System.out.println("║ Type: " + (conv.isGroupConversation() ? "Group" : "Private"));
            System.out.println("║ Participants: " + conv.getParticipantIds(clientController.getGroupRepository()).size());

            // Get message count
            var messages = conv.getMessagesFrom(null, -1, true, true);
            System.out.println("║ Messages: " + messages.size());

            if (!messages.isEmpty()) {
                Message lastMsg = messages.get(messages.size() - 1);
                System.out.println("║ Last message: " +
                        TIME_FORMATTER.format(lastMsg.getTimestamp()));
            }
            System.out.println("╠════════════════════════════════════════════════╣");
        }

        System.out.println("╚════════════════════════════════════════════════╝");
    }

    /**
     * View conversation history.
     */
    private void handleViewConversationHistory() {
        System.out.print("Enter conversation ID or user/group ID directly: ");
        String input = scanner.nextLine().trim();

        ConversationClient conversation;

        // Try to parse as user ID first
        try {
            int userId = Integer.parseInt(input);
            String conversationId = ClientController.generatePrivateConversationId(clientController.getClientId(), userId);
            conversation = clientController.getConversationRepository().findById(conversationId);
            if (conversation == null) {
                conversationId = ClientController.generateGroupConversationId(userId);
                conversation = clientController.getConversationRepository().findById(conversationId);
            }
        } catch (NumberFormatException e) {
            // Not a number, use as conversation ID directly
            conversation = clientController.getConversationRepository().findById(input);
        }

        if (conversation == null) {
            System.out.println("\nConversation not found.");
            return;
        }

        System.out.print("Number of messages to display (or -1 for all): ");
        int count;
        try {
            count = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid number.");
            scanner.nextLine();
            return;
        }

        var messages = conversation.getMessagesFrom(null, count, true, false);

        if (messages.isEmpty()) {
            System.out.println("\nNo messages in this conversation.");
            return;
        }

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║         CONVERSATION: " + conversation.getConversationId());
        System.out.println("╠════════════════════════════════════════════════╣");

        for (Message msg : messages) {
            System.out.println("║ message ID: " + msg.getMessageId());
            String fromLabel = (msg.getFromUserId() == clientController.getClientId())
                    ? "You"
                    : "User #" + msg.getFromUserId();

            System.out.println("║ [" + TIME_FORMATTER.format(msg.getTimestamp()) + "] " + fromLabel);

            if (msg.getReplyToMessageId() != null) {
                System.out.println("║   ↳ Reply to: " + msg.getReplyToMessageId().substring(0, 8) + "...");
            }

            if (msg.getFromUserId() == clientController.getClientId()) {
                System.out.println("║   Status: " + msg.getStatus());
            }

            System.out.println("║   " + msg.getContent());

            if (!msg.getReactions().isEmpty()) {
                System.out.print("║   Reactions: ");
                msg.getReactions().forEach((emoji, users) ->
                        System.out.print(emoji + "(" + users.size() + ") ")
                );
                System.out.println();
            }

            System.out.println("╠════════════════════════════════════════════════╣");
        }

        System.out.println("╚════════════════════════════════════════════════╝");
    }

    private void handleSendContactRequest() {
        System.out.print("User ID to add as contact: ");
        int targetId;
        try {
            targetId = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid ID.");
            scanner.nextLine();
            return;
        }

        String requestId = clientController.sendContactRequest(targetId);
        if (requestId != null) {
            System.out.println("Contact request sent. Waiting for response...");
        }
    }

    private void handleViewContactRequests() {
        var requests = clientController.getContactRepository().getPendingReceivedRequests();

        if (requests.isEmpty()) {
            System.out.println("\nNo pending contact requests.");
            return;
        }

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║          PENDING CONTACT REQUESTS              ║");
        System.out.println("╠════════════════════════════════════════════════╣");

        for (ContactRequest req : requests) {
            System.out.println("║ From: User #" + req.getSenderId());
            System.out.println("║ Received: " + DATE_TIME_FORMATTER.format(req.getTimestamp()));
            System.out.println("║ Expires: " + DATE_TIME_FORMATTER.format(req.getExpiresAt()));
            System.out.println("╠════════════════════════════════════════════════╣");
        }

        System.out.println("╚════════════════════════════════════════════════╝");
    }

    private void handleRespondToContactRequest() {
        System.out.print("Sender ID: ");
        int senderId;
        try {
            senderId = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid ID.");
            scanner.nextLine();
            return;
        }

        System.out.print("Accept? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();
        boolean accept = response.equals("y") || response.equals("yes");

        clientController.respondToContactRequest(senderId, accept);
    }

    /* ----------------------- Main Loop ----------------------- */

    /**
     * Run the main interaction loop.
     */
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (true) {
            displayMenu();

            int action;
            try {
                action = readIntegerFromUser("Invalid input");
            } catch (Exception e) {
                continue;
            }

            if (action == 0) {
                System.out.println("\nGoodbye!");
                break;
            }

            switch (action) {
                case 1 -> handleSendMessage();
                case 2 -> handleSendContactRequest();
                case 3 -> handleViewContactRequests();
                case 4 -> handleRespondToContactRequest();
                case 5 -> handleRemoveContact();
                case 6 -> handleUpdatePseudo();
                case 7 -> groupGestion();
                case 8 -> handleListContacts();
                case 9 -> handleListGroup();
                case 10 -> handleListConversations();
                case 11 -> handleViewConversationHistory();
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Close resources and disconnect.
     */
    public void cleanup() {
        clientController.disconnect();
        scanner.close();
    }

    /**
     * Main entry point for the CLI client.
     */
    public static void main(String[] args) {
        CliClient cliClient = createClient();

        try {
            if (cliClient.connect()) {
                cliClient.run();
            } else {
                System.err.println("Failed to connect to server. Exiting.");
                System.exit(1);
            }
        } finally {
            cliClient.cleanup();
        }

        System.exit(0);
    }
}
