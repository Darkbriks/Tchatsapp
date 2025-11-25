package fr.uga.im2ag.m1info.chatservice.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Home panel displaying the list of conversations.
 * Provides search functionality and new conversation button.
 * Pure UI component with no business logic.
 */
public class HomePanel extends JPanel {

    /**
     * Data class representing a conversation item in the list.
     */
    public static final class ConversationItem {
        private final String id;
        private final String title;
        private final String lastMessage;

        public ConversationItem(String id, String title, String lastMessage) {
            this.id = id;
            this.title = title;
            this.lastMessage = lastMessage;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private final JTextField searchField;
    private final DefaultListModel<ConversationItem> listModel;
    private final JList<ConversationItem> conversationList;
    private final JButton newConversationButton;
    private final JButton newContactButton;
    private final JButton pendingContactRequestButton;
    private final JButton viewContactsButton;


    private final List<ConversationItem> masterList;
    private ActionListener onNewConversation;
    private ActionListener onNewContact;
    private ActionListener onViewContacts;
    private ActionListener onShowPendingRequest;

    public HomePanel() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        this.searchField = new JTextField();
        this.listModel = new DefaultListModel<>();
        this.conversationList = new JList<>(listModel);
        this.newConversationButton = new JButton("New +");
        this.newContactButton = new JButton("Add a contact");
        this.pendingContactRequestButton = new JButton("Pending contact requests");
        this.viewContactsButton = new JButton("Show contacts");
        this.masterList = new ArrayList<>();

        setupLayout();
        setupListeners();
    }

    private void setupLayout() {
        // Header with title and search
        JPanel header = new JPanel(new BorderLayout(6, 6));
        JLabel title = new JLabel("Conversations");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        header.add(title, BorderLayout.NORTH);

        searchField.setToolTipText("Search for a conversation");
        header.add(searchField, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Conversation list
        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setCellRenderer(new ConversationRenderer());
        JScrollPane scrollPane = new JScrollPane(conversationList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with new contact button & new conversation button
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(newContactButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(viewContactsButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(pendingContactRequestButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(newConversationButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        // Search filter
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter(searchField.getText());
            }
        });

        // New conversation button
        newConversationButton.addActionListener(e -> {
            if (onNewConversation != null) {
                onNewConversation.actionPerformed(e);
            }
        });

        // New contact button
        newContactButton.addActionListener(e -> {
            if (onNewContact != null) {
                onNewContact.actionPerformed(e);
            }
        });

        viewContactsButton.addActionListener(e -> {
            if (onViewContacts != null) {
                onViewContacts.actionPerformed(e);
            }
        });

        // Pending contact requests button
        pendingContactRequestButton.addActionListener(e -> {
            if (onShowPendingRequest != null) {
                onShowPendingRequest.actionPerformed(e);
            }
        });
    }

    private void applyFilter(String query) {
        String q = (query == null) ? "" : query.trim().toLowerCase();
        listModel.clear();

        for (ConversationItem item : masterList) {
            if (q.isEmpty() || item.getTitle().toLowerCase().contains(q)) {
                listModel.addElement(item);
            }
        }
    }

    // ----------------------- Public API -----------------------

    /**
     * Set the list of conversations to display.
     *
     * @param conversations the conversations to display
     */
    public void setConversations(List<ConversationItem> conversations) {
        masterList.clear();
        if (conversations != null) {
            masterList.addAll(conversations);
        }
        applyFilter(searchField.getText());
    }

    /**
     * Get the currently selected conversation.
     *
     * @return the selected conversation, or null if none selected
     */
    public ConversationItem getSelectedConversation() {
        return conversationList.getSelectedValue();
    }

    /**
     * Add a listener for conversation selection changes.
     *
     * @param listener the list selection listener
     */
    public void addConversationSelectionListener(ListSelectionListener listener) {
        conversationList.addListSelectionListener(listener);
    }

    /**
     * Set the callback for the new conversation button.
     *
     * @param listener the action listener
     */
    public void setOnNewConversation(ActionListener listener) {
        this.onNewConversation = listener;
    }

    /**
     * Set the callback for the new contact button.
     *
     * @param listener the action listener
     */
    public void setOnNewContact(ActionListener listener) {
        this.onNewContact = listener;
    }

    /**
     * Set the callback for the contact view button.
     *
     * @param listener the action listener
     */
    public void setOnViewContacts(ActionListener listener) {
        this.onViewContacts = listener;
    }

    public void setOnShowPendingRequest(ActionListener l) {
        this.onShowPendingRequest = l;
    }


    // ----------------------- Cell Renderer -----------------------

    /**
     * Custom renderer for conversation items.
     */
    private static final class ConversationRenderer extends JPanel implements ListCellRenderer<ConversationItem> {
        private static final int MAX_PREVIEW_CHARS = 77;

        private final JLabel titleLabel;
        private final JLabel previewLabel;

        ConversationRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 10, 10, 10));

            titleLabel = new JLabel();
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

            previewLabel = new JLabel();
            previewLabel.setForeground(new Color(0, 0, 0, 160));

            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.setOpaque(false);
            centerPanel.add(titleLabel, BorderLayout.NORTH);
            centerPanel.add(previewLabel, BorderLayout.SOUTH);
            add(centerPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ConversationItem> list,
                                                      ConversationItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            titleLabel.setText(value.getTitle());

            String preview = (value.getLastMessage() == null) ? "" : value.getLastMessage();
            if (preview.length() > MAX_PREVIEW_CHARS) {
                preview = preview.substring(0, MAX_PREVIEW_CHARS - 3) + "...";
            }
            previewLabel.setText(preview);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setOpaque(true);

            return this;
        }
    }
}