package fr.uga.im2ag.m1info.chatservice.gui;

import fr.uga.im2ag.m1info.chatservice.client.Client;
import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.ClientPaquetRouter;
import fr.uga.im2ag.m1info.chatservice.client.handlers.*;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1666;

    private static final String CARD_LOGIN = "login";
    private static final String CARD_HOME = "home";
    private static final String CARD_CONVERSATION = "conversation";

    private ClientController controller;
    private GuiEventHandler eventHandler;

    // UI components
    private final JPanel cards;
    private final CardLayout cardLayout;
    private final LoginPanel loginPanel;
    private final HomePanel homePanel;
    private final ConversationPanel conversationPanel;

    // State
    private String currentConversationId;
    private volatile boolean awaitingConnection;

    public MainFrame() {
        super("TchatApp");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleApplicationClose();
            }
        });

        // Initialize UI components
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        this.loginPanel = new LoginPanel();
        this.homePanel = new HomePanel();
        this.conversationPanel = new ConversationPanel();

        // Setup card layout
        cards.add(loginPanel, CARD_LOGIN);
        cards.add(homePanel, CARD_HOME);
        cards.add(conversationPanel, CARD_CONVERSATION);
        setContentPane(cards);

        // Initial size for login
        loginPanel.setPreferredSize(new Dimension(420, 360));
        pack();
        setLocationRelativeTo(null);

        // Setup UI event listeners
        setupUIListeners();
    }

    // ----------------------- UI Setup -----------------------

    private void setupUIListeners() {
        loginPanel.addSubmitListener(e -> handleLogin());
    }

    private void setupEventHandlerCallbacks() {
        eventHandler.setOnConnectionEstablished(event -> {
            if (awaitingConnection) {
                awaitingConnection = false;
                onConnectionSuccess(event.getClientId(), event.getPseudo(), event.isNewUser());
            }
        });

        eventHandler.setOnError(event -> {
            if (awaitingConnection && event.getErrorLevel() == ErrorMessage.ErrorLevel.CRITICAL) {
                awaitingConnection = false;
                onConnectionError(event.getErrorMessage());
            } else {
                showErrorDialog(event.getErrorLevel().toString(), event.getErrorMessage());
            }
        });
    }

    // ----------------------- Login Flow -----------------------

    // TODO: Better gestion for invalid id
    private void handleLogin() {
        loginPanel.clearError();

        String raw = loginPanel.getUsername().trim();
        if (raw.isEmpty()) {
            loginPanel.showError("Veuillez entrer un identifiant numérique.");
            return;
        }

        int clientId;
        try {
            clientId = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            loginPanel.showError("Identifiant invalide : veuillez entrer un nombre.");
            return;
        }

        // Ask for pseudo if creating new account
        String pseudo = "";
        if (clientId == 0) {
            pseudo = askPseudo();
            if (pseudo == null) {
                return; // User cancelled
            }
        }

        // Initialize client and controller
        initializeClient(clientId);

        // Start connection
        awaitingConnection = true;
        loginPanel.getLoginButton().setEnabled(false);
        loginPanel.showError("Connexion en cours...");

        final String finalPseudo = pseudo;
        new Thread(() -> {
            try {
                boolean socketConnected = controller.connect(DEFAULT_HOST, DEFAULT_PORT, finalPseudo);
                if (!socketConnected) {
                    SwingUtilities.invokeLater(() -> {
                        awaitingConnection = false;
                        onConnectionError("Impossible d'établir la connexion au serveur.");
                    });
                }
                // If socket connected, wait for ConnectionEstablishedEvent or ErrorEvent
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    awaitingConnection = false;
                    onConnectionError("Erreur de connexion : " + e.getMessage());
                });
            }
        }, "Connection-Thread").start();
    }

    private void initializeClient(int clientId) {
        Client client = new Client(clientId);
        this.controller = new ClientController(client);

        // Initialize packet handlers
        ClientPaquetRouter router = new ClientPaquetRouter(controller);
        router.addHandler(new AckConnectionHandler());
        router.addHandler(new AckMessageHandler(client.getCommandManager()));
        router.addHandler(new TextMessageHandler());
        router.addHandler(new MediaMessageHandler());
        router.addHandler(new ErrorMessageHandler());
        router.addHandler(new ManagementMessageHandler());
        router.addHandler(new ContactRequestHandler());
        client.setPacketProcessor(router);

        // Initialize event handler
        this.eventHandler = new GuiEventHandler(controller);
        setupEventHandlerCallbacks();
        eventHandler.registerSubscriptions();
    }

    private void onConnectionSuccess(int clientId, String pseudo, boolean isNewUser) {
        loginPanel.clearError();
        loginPanel.getLoginButton().setEnabled(true);

        if (isNewUser) {
            JOptionPane.showMessageDialog(
                    this,
                    "Compte créé avec succès !\nVotre identifiant : " + clientId,
                    "Bienvenue",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        setTitle("TchatApp - " + pseudo);
        showHome();
    }

    private void onConnectionError(String errorMessage) {
        loginPanel.showError(errorMessage);
        loginPanel.getLoginButton().setEnabled(true);

        // Cleanup if client was initialized
        if (eventHandler != null) {
            eventHandler.unsubscribeAll();
            eventHandler = null;
        }
        if (controller != null) {
            controller.disconnect();
            controller = null;
        }
    }

    private String askPseudo() {
        String pseudo;
        do {
            pseudo = JOptionPane.showInputDialog(
                    this,
                    "Choisissez votre pseudo :",
                    "Nouveau compte",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (pseudo == null) {
                return null; // User cancelled
            }
            pseudo = pseudo.trim();
        } while (pseudo.isEmpty());
        return pseudo;
    }

    // ----------------------- Navigation -----------------------

    private void showLogin() {
        cardLayout.show(cards, CARD_LOGIN);
        setSize(420, 360);
        setLocationRelativeTo(null);
    }

    private void showHome() {
        cardLayout.show(cards, CARD_HOME);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private void showConversation() {
        cardLayout.show(cards, CARD_CONVERSATION);
    }

    // ----------------------- Home Panel Actions -----------------------
    // TODO

    // ----------------------- Conversation Panel Actions -----------------------
    // TODO

    // ----------------------- Application Lifecycle -----------------------

    private void handleApplicationClose() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Voulez-vous vraiment quitter ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            cleanup();
            dispose();
            System.exit(0);
        }
    }

    private void cleanup() {
        if (eventHandler != null) {
            eventHandler.unsubscribeAll();
        }
        if (controller != null) {
            controller.disconnect();
        }
    }

    // ----------------------- Utility -----------------------

    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // ----------------------- Entry Point -----------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}