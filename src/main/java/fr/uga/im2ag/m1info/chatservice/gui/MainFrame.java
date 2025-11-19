package fr.uga.im2ag.m1info.chatservice.gui;

import fr.uga.im2ag.m1info.chatservice.client.Client;
import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.ClientPaquetRouter;
import fr.uga.im2ag.m1info.chatservice.client.handlers.*;
import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;
import fr.uga.im2ag.m1info.chatservice.client.repository.ContactClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.repository.ConversationClientRepository;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;
import fr.uga.im2ag.m1info.chatservice.gui.ConversationPanel.MessageItem;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainFrame extends JFrame {
    private static final int SERVER_ID = 0;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1666;


    private Client client;
    private ClientController controller;
    
    private final JPanel cards = new JPanel(new CardLayout());
    private final LoginPanel loginPanel = new LoginPanel();
    private final HomePanel homePanel = new HomePanel();
    private final ConversationPanel conversationPanel = new ConversationPanel();

    public MainFrame() {
        super("TchatApp");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cards.add(loginPanel, "login");
        cards.add(homePanel, "home");
        cards.add(conversationPanel, "conversation");
        setContentPane(cards);

        loginPanel.setPreferredSize(new Dimension(420, 360));
        pack();
        setLocationRelativeTo(null);

        loginPanel.addSubmitListener(e -> handleLogin());
        homePanel.addConversationSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                HomePanel.ConversationItem selected = homePanel.getSelectedConversation();
                if (selected != null) {
                    handleConversationSelection(selected);
                }
            }
        });
    }

    private void showHome() {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, "home");
        setSize(900, 600);
        setLocationRelativeTo(null);
        homePanel.setOnNewConversation(ev -> {
        System.out.println("Créer une nouvelle conversatio");
                // Call backend
            });

    }

    private void showErrorLogin(String msg){
        loginPanel.showError(msg);
    }

    public void clearErrorLogin() {
        loginPanel.clearError();
    }

    private void handleLogin(){
        clearErrorLogin();

        String raw = loginPanel.getUsername().trim();
        if (raw.isEmpty()) {
            showErrorLogin("Veuillez entrer un identifiant numérique.");
            return;
        }
        int idClient;
        try { 
            idClient = Integer.parseInt(raw);
        }
        catch (NumberFormatException exc) {
            showErrorLogin("Identifiant invalide : veuillez entrer un nombre.");
            return;
        }

        this.client = new Client(idClient);
        this.controller = new ClientController(client);
        initializeHandlers();
        String pseudo = "";
        boolean connected = false;
        if (client.getClientId() == 0) {
            pseudo = askPseudo();
            if (Objects.equals(pseudo, "") || pseudo == null) {return;}
        }
        try {
            connected = client.connect(DEFAULT_HOST, DEFAULT_PORT, pseudo);
        } catch (IOException e) {
            showErrorLogin("[Client] Connection error: " + e.getMessage());
        }
        if (connected) { 
            showHome();
            refreshHomeConversations();
        }
    }

    private void initializeHandlers() {
        ClientPaquetRouter router = new ClientPaquetRouter(controller);
        router.addHandler(new AckConnectionHandler());
        router.addHandler(new TextMessageHandler());
        router.addHandler(new MediaMessageHandler());
        router.addHandler(new ErrorMessageHandler());
        router.addHandler(new ManagementMessageHandler());
        client.setPacketProcessor(router);
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
                // Annulé, on abandonne le login
                return null;
            }
            pseudo = pseudo.trim();
        } while (pseudo.isEmpty());
        return pseudo;
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

    public void handleConversationSelection(HomePanel.ConversationItem conversation) {
        CardLayout cl = (CardLayout) cards.getLayout();
        ConversationClient conv = controller.getConversationRepository()
                                         .findById(conversation.getId());
        conversationPanel.setConversationTitle(conv.getConversationName());
        List<ConversationPanel.MessageItem> messageItems = loadMessages(conv);
        conversationPanel.setMessages(messageItems);
        conversationPanel.setOnSend(text -> {                   
                        TextMessage textMsg = (TextMessage) MessageFactory.create(MessageType.TEXT, controller.getClientId(), 0);
                        textMsg.setContent(text);
                        controller.sendPacket(textMsg.toPacket());
                        Message msg = new Message (textMsg.getMessageId(),
                                           controller.getClientId(),
                                           Integer.parseInt(conv.getConversationId()), 
                                           text, 
                                           textMsg.getTimestamp(),
                                           text);                              
                        conversationPanel.appendMessage(new MessageItem(true, null, text));
                        
        });
        conversationPanel.setOnBack(e -> cl.show(cards, "home"));
        cl.show(cards, "conversation");
        setLocationRelativeTo(null);
    }

    public List<ConversationPanel.MessageItem> loadMessages(ConversationClient conversation){
        List<Message> messagesFromConv = conversation.getMessagesFrom(null, -1, true, true);
        ContactClientRepository contactClientRepository = controller.getContactRepository();
        List<ConversationPanel.MessageItem> messageItems = new ArrayList<ConversationPanel.MessageItem>();
        for (Message message : messagesFromConv) {
            messageItems.add(new ConversationPanel.MessageItem (
                             message.getFromUserId() == client.getClientId(),
                             contactClientRepository.findById(message.getFromUserId())
                                                    .getPseudo(),
                             message.getContent()));
        }
        return messageItems;
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
