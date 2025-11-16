package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.handlers.*;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.KeyInMessage;
import fr.uga.im2ag.m1info.chatservice.common.ShaIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

import java.io.IOException;
import java.util.Scanner;

/**
 * Command-line interface for the TchatsApp client.
 * Provides a text-based menu to interact with the chat service.
 */
public class CliClient {
    private static final int SERVER_ID = 0;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1666;

    // TODO: client attributes should be removed in favor of context methods
    // when possible
    private final Client client;
    private final ClientContext context;
    private final Scanner scanner;

    /**
     * Creates a new CLI client.
     */
    CliClient(int clientId, Scanner scanner) {
        this.client = new Client(clientId);
        this.client.setMessageIdGenerator(new ShaIdGenerator());
        this.context = new ClientContext(client);
        this.scanner = scanner;
        initializeHandlers();
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
     * Initialize the client with packet handlers.
     */
    private void initializeHandlers() {
        ClientPaquetRouter router = new ClientPaquetRouter(context);
        router.addHandler(new AckConnectionHandler());
        router.addHandler(new TextMessageHandler());
        router.addHandler(new MediaMessageHandler());
        router.addHandler(new ErrorMessageHandler());
        router.addHandler(new ManagementMessageHandler());
        client.setPacketProcessor(router);
    }

    /**
     * Connect to the server with credentials.
     *
     * @return true if connection initiated successfully, false otherwise
     */
    public boolean connect() {
        String pseudo = "";
        if (client.getClientId() == 0) {
            System.out.println("Choose your username:");
            pseudo = scanner.nextLine().trim();
        }
        try {
            return client.connect(DEFAULT_HOST, DEFAULT_PORT, pseudo);
        } catch (IOException e) {
            System.err.println("[Client] Connection error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Display the main menu.
     */
    private void displayMenu() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("1. Send a message");
        System.out.println("2. Add a contact");
        System.out.println("3. Remove a contact");
        System.out.println("4. Change your username");
        System.out.println("5. Group gestion");
        System.out.println("0. Quit");
        System.out.print("Your choice: ");
    }

    /** 
     * Display the group menu.
     */
    private void displayGroupMenu() {
        System.out.println("\n=== Group Menu ===");
        System.out.println("1. Create a group");
        System.out.println("2. Leave a group");
        System.out.println("3. Add menber ( Admin only)");
        System.out.println("4. Remove menber ( Admin only)");
        System.out.println("0. Back to main menu");
        System.out.print("Your choice: ");
    }

    /**
     * Handle sending a message.
     */
    private void handleSendMessage() {
        System.out.print("Recipient ID: ");
        // System.out.println("You can send message tothe following users");  
        // for (ContactClient contact : context.getContactRepository().findAll()){
        //     System.out.print(contact.getPseudo() + "=(" + contact.getContactId() + ") ");
        // }
        // System.out.println();
        int to;
        try {
            to = readIntegerFromUser("Invalid ID.");
        } catch (Exception e) {
            return;
        }

        System.out.print("Your message (start with '/' for file path): ");
        String msg = scanner.nextLine();

        if (!msg.isEmpty() && msg.charAt(0) == '/') {
            client.sendMedia(msg, to);
        } else {
            TextMessage textMsg = (TextMessage) MessageFactory.create(MessageType.TEXT, context.getClientId(), to);
            textMsg.generateNewMessageId(client.getMessageIdGenerator());
            textMsg.setContent(msg);
            context.sendPacket(textMsg.toPacket());
        }
    }

    /**
     * Read an integer from user
     *
     * @param errorMessage Error message to display if invalid input 
     * @return The integer user provide 
     * @throws throw e; 
     */
    private int readIntegerFromUser(String errorMessage){
        int result;
        try {
            result = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid ID.");
            scanner.nextLine();
            throw e;
        }
        return result;
    }

    /**
     * Handle adding a contact.
     */
    private void handleAddContact() {
        System.out.print("Contact ID to add: ");
        int contactId;
        try {
            contactId = readIntegerFromUser("Invalid ID.");
        } catch (Exception e) {
            return;
        }

        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.ADD_CONTACT, context.getClientId(), SERVER_ID);
        mgmtMsg.addParam("contactId", Integer.toString(contactId));
        context.sendPacket(mgmtMsg.toPacket());
    }

    /**
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

        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.REMOVE_CONTACT, context.getClientId(), SERVER_ID);
        mgmtMsg.addParam("contactId", Integer.toString(contactId));
        context.sendPacket(mgmtMsg.toPacket());
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

        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.UPDATE_PSEUDO, context.getClientId(), SERVER_ID);
        mgmtMsg.addParam("newPseudo", newPseudo);
        context.sendPacket(mgmtMsg.toPacket());
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

        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.CREATE_GROUP, context.getClientId(), SERVER_ID);
        mgmtMsg.addParam(KeyInMessage.GROUP_NAME, groupName);
        context.sendPacket(mgmtMsg.toPacket());
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

        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.LEAVE_GROUP, context.getClientId(), groupId);
        mgmtMsg.addParam(KeyInMessage.GROUP_ID, groupId);
        context.sendPacket(mgmtMsg.toPacket());
    }

    /**
     * Handle add menber to a group 
     */
    private void handleAddMenberGroup() {
        System.out.print("Group id: ");
        int groupId;
        try {
            groupId= readIntegerFromUser("Invalid group ID.");
        } catch (Exception e) {
            return;
        }
        System.out.print("Menber id: ");
        int newMenber;
        try {
            newMenber = readIntegerFromUser("Invalid menber ID.");
        } catch (Exception e) {
            return;
        }


        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.ADD_GROUP_MEMBER, context.getClientId(), groupId);
        mgmtMsg.addParam(KeyInMessage.MENBER_ADD_ID, newMenber);
        context.sendPacket(mgmtMsg.toPacket());
    }

    /**
     * Handle remove menber to a group 
     */
    private void handleRemoveMenberGroup() {
        System.out.print("Group id: ");
        int groupId;
        try {
            groupId= readIntegerFromUser("Invalid group ID.");
        } catch (Exception e) {
            return;
        }
        System.out.print("Menber id: ");
        int deleteMenber;
        try {
            deleteMenber = readIntegerFromUser("Invalid menber ID.");
        } catch (Exception e) {
            return;
        }


        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.REMOVE_GROUP_MEMBER, context.getClientId(), groupId);
        mgmtMsg.addParam(KeyInMessage.MENBER_REMOVE_ID, deleteMenber);
        context.sendPacket(mgmtMsg.toPacket());
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
            case 3 -> handleAddMenberGroup();
            case 4 -> handleRemoveMenberGroup();
            default -> { 
                System.out.println("Invalid choice. Please try again.");
                groupGestion();
            }

        }

    }

    /**
     * Run the main interaction loop.
     */
    public void run() {
        while (true) {
            displayMenu();

            int action;
            try {
                action = readIntegerFromUser("Invalid input");
            } catch (Exception e) {
                continue;
            }

            if (action == 0) {
                System.out.println("Goodbye!");
                break;
            }

            switch (action) {
                case 1 -> handleSendMessage();
                case 2 -> handleAddContact();
                case 3 -> handleRemoveContact();
                case 4 -> handleUpdatePseudo();
                case 5 -> groupGestion();
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Close resources and disconnect.
     */
    public void cleanup() {
        context.disconnect();
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
