package fr.uga.im2ag.m1info.chatservice.gui;

import fr.uga.im2ag.m1info.chatservice.client.Client;
import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactRequest;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;
import fr.uga.im2ag.m1info.chatservice.client.repository.ContactClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.ConversationClientRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
    private HashMap <String, Set<Integer>> pendingGroupParticipants;

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
        homePanel.addConversationSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                HomePanel.ConversationItem selected = homePanel.getSelectedConversation();
                if (selected != null) {
                    handleConversationSelection(selected);
                }
            }
        });
        homePanel.setOnNewConversation(e -> handleNewConversationRequest());
        homePanel.setOnNewContact(e -> handleNewContactRequest());
        homePanel.setOnViewContacts(e -> generateContactView());
        homePanel.setOnShowPendingRequest(e -> showPendingContactRequests());
    }

    

    private void setupEventHandlerCallbacks() {
        eventHandler.setOnConnectionEstablished(event -> {
            if (awaitingConnection) {
                awaitingConnection = false;
                onConnectionSuccess(event.getClientId(), event.getPseudo(), event.isNewUser());
            }
        });

        eventHandler.setOnGroupCreated(event -> {
            int groupId = event.getGroupInfo().getGroupId();
            String groupName = event.getGroupInfo().getGroupName();
            if (pendingGroupParticipants.containsKey(groupName)) {
                for (Integer participantId : pendingGroupParticipants.get(groupName)){
                    controller.addMemberToGroup(groupId, participantId);
                }
                controller.getOrCreateGroupConversation(groupId);
                pendingGroupParticipants.remove(groupName);
            }
            refreshHomeConversations();
        });

        eventHandler.setOnGroupMemberChanged(event -> {
            int groupId = event.getGroupId();
            if (controller.getGroupRepository().findById(groupId) != null) 
                controller.getGroupRepository().findById(groupId).addMember(event.getMemberId());
        });

        eventHandler.setOnContactRequestResponse(event -> {
            showContactRequestResponseDialog(event.isAccepted(), event.getOtherUserId());
        });

        eventHandler.setOnContactRequestReceived(event -> {
            boolean choice = showContactRequestReceivedDialog(event.getSenderId());
            controller.respondToContactRequest(event.getSenderId(), choice);   
        });

        eventHandler.setOnUserPseudoUpdated(event -> {
            String newPseudo = event.getNewPseudo();
            setTitle("TchatApp - " + newPseudo);
            homePanel.repaint();

        });

        eventHandler.setOnContactUpdated(event -> {
            refreshHomeConversations();
            homePanel.repaint();
        });

        eventHandler.setOnTextMessageReceived(event -> {
            refreshHomeConversations();
            refreshMessages(controller.getConversationRepository().findById(event.getConversationId()));
            conversationPanel.repaint();
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
        this.controller = new ClientController(new Client(clientId));
        //this.controller.initializeEncryption();
        this.controller.initializeHandlers();

        // Initialize event handler
        this.eventHandler = new GuiEventHandler(controller);
        setupEventHandlerCallbacks();
        eventHandler.registerSubscriptions();

        // Initialize group member pending for a group created on the request of the user
        this.pendingGroupParticipants = new HashMap<>();
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
    }

    private void showHome() {
        cardLayout.show(cards, CARD_HOME);
        setSize(900, 600);
    }

    private void showConversation() {
        cardLayout.show(cards, CARD_CONVERSATION);
    }

    // ----------------------- Home Panel Actions -----------------------

    private void handleNewConversationRequest() {

        ContactClientRepository contactRepo = controller.getContactRepository();
        if (contactRepo == null) {
            JOptionPane.showMessageDialog(this,
                    "No contact repo available",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int currentUserId = controller.getClientId();

        CreateConversationDialog dialog = new CreateConversationDialog(this, contactRepo, currentUserId);
        CreateConversationDialog.Result res = dialog.showDialog();
        if (res == null) {
            return;
        }

        if (res.isGroup()) {
            res.getParticipantIds().add(currentUserId);
            controller.createGroup(res.getConversationName());

            pendingGroupParticipants.put(res.getConversationName(), res.getParticipantIds());
        }
        else {
            int otherParticipantId = res.getParticipantIds().iterator().next();
            ConversationClient conv = controller.getOrCreatePrivateConversation(otherParticipantId);
            ContactClient contact = controller.getContactRepository().findById(otherParticipantId);
            conv.setConversationName(contact.getPseudo());
        }
        // MaJ HP
        refreshHomeConversations();
    }

    private void refreshHomeConversations() {
        ConversationClientRepository convoRepo = controller.getConversationRepository();

        Set<ConversationClient> allConv = convoRepo.findAll();

        ArrayList<HomePanel.ConversationItem> items = new ArrayList<>();

        for (ConversationClient conv : allConv) {
            Message last = conv.getLastMessage();
            String preview = "";
            if (last != null) {preview = last.getContent();}

            HomePanel.ConversationItem item =
                    new HomePanel.ConversationItem(
                            conv.getConversationId(),
                            conv.getConversationName(),
                            preview
                    );

            items.add(item);
        }

        items.sort((a, b) -> {
            ConversationClient ca = convoRepo.findById(a.getId());
            ConversationClient cb = convoRepo.findById(b.getId());
            Message ma = ca.getLastMessage();
            Message mb = cb.getLastMessage();
            Instant ta = (ma == null ? Instant.MIN : ma.getTimestamp());
            Instant tb = (mb == null ? Instant.MIN : mb.getTimestamp());
            return tb.compareTo(ta); // plus récent d’abord
        });

        homePanel.setConversations(items);
    }

    private void handleNewContactRequest() {
        String idToAdd;
        try {
            do {
                idToAdd = JOptionPane.showInputDialog(this, "Envoyer une demande de contact à :", "Nouveau contact", JOptionPane.QUESTION_MESSAGE);
                if (idToAdd == null) { return; }
                idToAdd = idToAdd.trim();
            } while (idToAdd.isEmpty());

            int otherUserId = Integer.parseInt(idToAdd);
            controller.sendContactRequest(otherUserId);
        } catch (NumberFormatException e) {
            showErrorDialog("Identifiant invalide", "Veuillez entrer un nombre valide.");
        }
    }

    private void generateContactView() {
        JDialog dialog = new JDialog(this, "Your contacts", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Set<ContactClient> contacts = controller.getContactRepository().findAll();

        if (contacts.isEmpty()) {
            listPanel.add(new JLabel("You have no contacts."));
        } else {
            for (ContactClient contact : contacts) {
                String label = "- " + contact.getPseudo()
                            + " (" + contact.getContactId() + ")";
                listPanel.add(new JLabel(label));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setPreferredSize(new Dimension(300, 200));

        dialog.add(scroll, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeBtn);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showPendingContactRequests() {
        if (controller == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Pas encore connecté, aucune demande de contact.",
                    "Demandes de contact",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        ContactClientRepository repo = controller.getContactRepository();
        repo.cleanupExpiredRequests();

        Set<ContactRequest> received = repo.getPendingReceivedRequests();
        Set<ContactRequest> sent = repo.getPendingSentRequests();

        if (received.isEmpty() && sent.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "no pending contact requests",
                    "pending contact requests",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        StringBuilder sb = new StringBuilder();

        if (!received.isEmpty()) {
            sb.append("received :\n");
            for (ContactRequest r : received) {
                sb.append(" - from ")
                .append(r.getSenderId())
                .append(" (request id : ")
                .append(r.getRequestId())
                .append(")\n");
            }
        }

        if (!sent.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("sent :\n");
            for (ContactRequest r : sent) {
                sb.append(" - to ")
                .append(r.getReceiverId())
                .append(" (request id : ")
                .append(r.getRequestId())
                .append(")\n");
            }
        }

        JOptionPane.showMessageDialog(
                this,
                sb.toString(),
                "pending contact request",
                JOptionPane.INFORMATION_MESSAGE
        );
    }




    // ----------------------- Conversation Panel Actions -----------------------


    public void handleConversationSelection(HomePanel.ConversationItem conversation) {
        CardLayout cl = (CardLayout) cards.getLayout();
        ConversationClient conv = controller.getConversationRepository()
                                         .findById(conversation.getId());

        currentConversationId = conv.getConversationId();
        if (conv.isGroupConversation()) {
            conversationPanel.setConversationTitle(conv.getConversationName());
        }

        refreshMessages(conv);

        conversationPanel.setOnSend((text, replyId) -> {
            if (text == null || text.trim().isEmpty()) {
                return;
            }
            String trimmed = text.trim();

            int toUserId = conv.getPeerId();
            controller.sendTextMessage(trimmed, toUserId);
            refreshMessages(conv);
        });

        conversationPanel.setOnOption(() -> {
            if (conv.isGroupConversation()) {
                showGroupOptions(conv);
            }
        });
        conversationPanel.setOnBack(e -> cl.show(cards, "home"));
        cl.show(cards, "conversation");
    }

    private void showGroupOptions(ConversationClient conv) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem viewMembers = new JMenuItem("Voir les membres");
        viewMembers.addActionListener(e -> showGroupMembersDialog(conv));
        menu.add(viewMembers);
        menu.show(conversationPanel, conversationPanel.getWidth() - 10, 35);
    }

    private void showGroupMembersDialog(ConversationClient conv) {
        ContactClientRepository contacts = controller.getContactRepository();

        StringBuilder sb = new StringBuilder();
        for (Integer memberId : controller.getGroupRepository().findById(conv.getPeerId()).getMembers()) {

            String pseudo = "User #" + memberId;
            ContactClient c = contacts.findById(memberId);

            if (c != null) {
                pseudo = c.getPseudo();
            }

            sb.append("- ").append(pseudo)
            .append(" (").append(memberId).append(")")
            .append("\n");
        }

        JOptionPane.showMessageDialog(
                this,
                sb.toString(),
                "Membres du groupe : " + conv.getConversationName(),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public List<ConversationPanel.MessageItem> loadMessages(ConversationClient conversation){
        List<Message> messagesFromConv = conversation.getMessagesFrom(null, -1, true, true);
        ContactClientRepository contactClientRepository = controller.getContactRepository();
        List<ConversationPanel.MessageItem> messageItems = new ArrayList<>();
        for (Message message : messagesFromConv) {
            boolean isOwnMessage = message.getFromUserId() == controller.getClientId();
            String pseudo;
            if (isOwnMessage) {
                pseudo = "Vous";
            } else {
                ContactClient contact = contactClientRepository.findById(message.getFromUserId());
                pseudo = (contact != null) ? contact.getPseudo() : "User " + message.getFromUserId();
            }
            messageItems.add(new ConversationPanel.MessageItem(isOwnMessage, pseudo, message.getContent(), message.getMessageId(), message.getReplyToMessageId()));
        }
        return messageItems;
    }

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

    private void showContactRequestResponseDialog (boolean isAccepted, int otherUserId) {

        String user = "User " + otherUserId;
        String message = isAccepted
                ? user + " has accepted your contact request"
                : user + " has denied your contact request";

        String title = isAccepted
                ? "Contact request accepted!"
                : "Contact request denied";

        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );

    }

    private boolean showContactRequestReceivedDialog (int senderID) {
        String [] options = {"Add user as contact", "Deny contact request"};
        int selection = JOptionPane.showOptionDialog(this,
                                                    "Contact request from " + senderID + "received",
                                                    "Contact Request received",
                                                    JOptionPane.DEFAULT_OPTION,
                                                    JOptionPane.INFORMATION_MESSAGE,
                                                    null,
                                                    options,
                                                    options[0]
        );
        return (selection == 0);
    }

    private void refreshMessages(ConversationClient conv) {
        List<ConversationPanel.MessageItem> messageItems = loadMessages(conv);
        conversationPanel.setMessages(messageItems);
    }

    // ----------------------- Entry Point -----------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}