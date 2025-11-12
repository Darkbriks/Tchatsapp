package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.event.types.TextMessageReceivedEvent;
import fr.uga.im2ag.m1info.chatservice.client.handlers.*;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.ShaIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;

import java.util.Scanner;

/**
 * Command-line interface for the TchatsApp client.
 * Provides a text-based menu to interact with the chat service.
 */
public class CliClient {
    private static final int SERVER_ID = 0;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1666;

    private final ClientController clientController;
    private final Scanner scanner;

    /**
     * Creates a new CLI client.
     */
    CliClient(int clientId, Scanner scanner) {
        Client client = new Client(clientId);
        client.setMessageIdGenerator(new ShaIdGenerator());
        this.clientController = new ClientController(client);
        this.scanner = scanner;
        initializeHandlers(client);
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
     * Initialize the client with packet handlers.
     */
    private void initializeHandlers(Client client) {
        ClientPaquetRouter router = new ClientPaquetRouter(clientController);
        router.addHandler(new AckConnectionHandler());
        router.addHandler(new TextMessageHandler());
        router.addHandler(new MediaMessageHandler());
        router.addHandler(new ErrorMessageHandler());
        router.addHandler(new ManagementMessageHandler());
        client.setPacketProcessor(router);
    }

    private void registerEventListeners() {
        clientController.subscribeToEvent(TextMessageReceivedEvent.class, this::textMessageEventCallback);
    }

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

    /**
     * Display the main menu.
     */
    private void displayMenu() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("1. Send a message");
        System.out.println("2. Add a contact");
        System.out.println("3. Remove a contact");
        System.out.println("4. Change your username");
        System.out.println("0. Quit");
        System.out.print("Your choice: ");
    }

    /**
     * Handle sending a message.
     */
    private void handleSendMessage() {
        System.out.print("Recipient ID: ");
        int to;
        try {
            to = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid ID.");
            scanner.nextLine();
            return;
        }

        System.out.print("Your message (start with '/' for file path): ");
        String msg = scanner.nextLine();

        if (!msg.isEmpty() && msg.charAt(0) == '/') {
            clientController.sendMedia(msg, to);
        } else {
            TextMessage textMsg = (TextMessage) MessageFactory.create(MessageType.TEXT, clientController.getClientId(), to);
            textMsg.generateNewMessageId(clientController.getMessageIdGenerator());
            textMsg.setContent(msg);
            clientController.sendPacket(textMsg.toPacket());
        }
    }

    /**
     * Handle adding a contact.
     */
    private void handleAddContact() {
        System.out.print("Contact ID to add: ");
        int contactId;
        try {
            contactId = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid ID.");
            scanner.nextLine();
            return;
        }

        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.ADD_CONTACT, clientController.getClientId(), SERVER_ID);
        mgmtMsg.addParam("contactId", Integer.toString(contactId));
        clientController.sendPacket(mgmtMsg.toPacket());
    }

    /**
     * Handle removing a contact.
     */
    private void handleRemoveContact() {
        System.out.print("Contact ID to remove: ");
        int contactId;
        try {
            contactId = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Invalid ID.");
            scanner.nextLine();
            return;
        }

        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(
                MessageType.REMOVE_CONTACT, clientController.getClientId(), SERVER_ID);
        mgmtMsg.addParam("contactId", Integer.toString(contactId));
        clientController.sendPacket(mgmtMsg.toPacket());
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
                MessageType.UPDATE_PSEUDO, clientController.getClientId(), SERVER_ID);
        mgmtMsg.addParam("newPseudo", newPseudo);
        clientController.sendPacket(mgmtMsg.toPacket());
    }

    /**
     * Run the main interaction loop.
     */
    public void run() {
        while (true) {
            displayMenu();

            int action;
            try {
                action = scanner.nextInt();
                scanner.nextLine();
            } catch (Exception e) {
                System.err.println("Invalid input.");
                scanner.nextLine();
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

    private void textMessageEventCallback(TextMessageReceivedEvent event) {
        System.out.println("\n[New message received in conversation " + event.getConversationId() + "]");
        System.out.println("\tFrom: " + event.getMessage().getFromUserId());
        System.out.println("\tTo: " + event.getMessage().getToUserId());
        System.out.println("\tMessageId: " + event.getMessage().getMessageId());
        System.out.println("\tTimestamp: " + event.getMessage().getTimestamp());
        if (event.getMessage().getReplyToMessageId() != null) {
            System.out.println("\tReply to: " + event.getMessage().getReplyToMessageId());
        }
        System.out.println("\tContent: " + event.getMessage().getContent());
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