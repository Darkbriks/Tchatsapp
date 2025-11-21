package fr.uga.im2ag.m1info.chatservice.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Panel displaying a conversation with messages.
 * Provides message input and send functionality.
 * Pure UI component with no business logic.
 */
public class ConversationPanel extends JPanel {

    /**
     * Data class representing a message item in the conversation.
     */
    public static final class MessageItem {
        private final boolean mine;
        private final String author;
        private final String text;

        public MessageItem(boolean mine, String author, String text) {
            this.mine = mine;
            this.author = author;
            this.text = text;
        }

        public boolean isMine() {
            return mine;
        }

        public String getAuthor() {
            return author;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Callback interface for message sending.
     */
    @FunctionalInterface
    public interface OnSendListener {
        void onSend(String text);
    }

    private final JButton backButton;
    private final JLabel titleLabel;
    private final DefaultListModel<MessageItem> messageModel;
    private final JList<MessageItem> messageList;
    private final JTextField inputField;
    private final JButton sendButton;

    private ActionListener onBack;
    private OnSendListener onSend;

    public ConversationPanel() {
        super(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        this.backButton = new JButton("< Retour");
        this.titleLabel = new JLabel("Conversation");
        this.messageModel = new DefaultListModel<>();
        this.messageList = new JList<>(messageModel);
        this.inputField = new JTextField();
        this.sendButton = new JButton("Envoyer");

        setupLayout();
        setupListeners();
    }

    private void setupLayout() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        backButton.setFocusable(false);
        headerPanel.add(backButton, BorderLayout.WEST);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Message list
        messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageList.setCellRenderer(new MessageRenderer());
        JScrollPane scrollPane = new JScrollPane(messageList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        sendButton.setFocusable(false);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        backButton.addActionListener(e -> {
            if (onBack != null) {
                onBack.actionPerformed(e);
            }
        });

        ActionListener sendAction = e -> {
            String text = inputField.getText();
            if (text != null && !text.trim().isEmpty()) {
                if (onSend != null) {
                    onSend.onSend(text.trim());
                }
                inputField.setText("");
            }
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
    }

    private void ensureLastVisible() {
        int size = messageModel.getSize();
        if (size > 0) {
            messageList.ensureIndexIsVisible(size - 1);
        }
    }

    // ----------------------- Public API -----------------------

    /**
     * Set the conversation title.
     *
     * @param title the title to display
     */
    public void setConversationTitle(String title) {
        titleLabel.setText((title == null) ? "" : title);
    }

    /**
     * Set all messages in the conversation.
     *
     * @param messages the messages to display
     */
    public void setMessages(List<MessageItem> messages) {
        messageModel.clear();
        if (messages != null) {
            for (MessageItem item : messages) {
                messageModel.addElement(item);
            }
        }
        ensureLastVisible();
    }

    /**
     * Append a message to the conversation.
     *
     * @param message the message to append
     */
    public void appendMessage(MessageItem message) {
        messageModel.addElement(message);
        ensureLastVisible();
    }

    /**
     * Set the callback for the back button.
     *
     * @param listener the action listener
     */
    public void setOnBack(ActionListener listener) {
        this.onBack = listener;
    }

    /**
     * Set the callback for message sending.
     *
     * @param listener the send listener
     */
    public void setOnSend(OnSendListener listener) {
        this.onSend = listener;
    }

    /**
     * Get the input text field.
     *
     * @return the input field
     */
    public JTextField getInputField() {
        return inputField;
    }

    // ----------------------- Cell Renderer -----------------------

    /**
     * Custom renderer for message items with bubble style.
     */
    private static final class MessageRenderer extends JPanel implements ListCellRenderer<MessageItem> {
        private static final Color COLOR_MY_MESSAGE = new Color(225, 248, 238);
        private static final Color COLOR_OTHER_MESSAGE = new Color(240, 240, 240);

        private final JLabel headerLabel;
        private final JTextArea bubbleArea;

        MessageRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 8, 6, 8));

            headerLabel = new JLabel();
            headerLabel.setFont(headerLabel.getFont().deriveFont(Font.PLAIN, 11f));
            headerLabel.setForeground(new Color(0, 0, 0, 130));

            bubbleArea = new JTextArea();
            bubbleArea.setLineWrap(true);
            bubbleArea.setWrapStyleWord(true);
            bubbleArea.setEditable(false);
            bubbleArea.setBorder(new EmptyBorder(8, 12, 8, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MessageItem> list,
                                                      MessageItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            removeAll();

            boolean isMine = value.isMine();
            String author = isMine ? "Vous" : ((value.getAuthor() == null) ? "??" : value.getAuthor());

            headerLabel.setText(author);
            bubbleArea.setText((value.getText() == null) ? "" : value.getText());
            bubbleArea.setBackground(isMine ? COLOR_MY_MESSAGE : COLOR_OTHER_MESSAGE);
            bubbleArea.setForeground(Color.DARK_GRAY);
            bubbleArea.setOpaque(true);

            // Build message box
            JPanel messageBox = new JPanel();
            messageBox.setLayout(new BoxLayout(messageBox, BoxLayout.Y_AXIS));
            messageBox.setOpaque(false);
            messageBox.add(headerLabel);
            messageBox.add(Box.createVerticalStrut(2));
            messageBox.add(bubbleArea);

            // Position based on sender
            JPanel linePanel = new JPanel(new BorderLayout());
            linePanel.setOpaque(false);

            if (isMine) {
                linePanel.add(messageBox, BorderLayout.EAST);
            } else {
                linePanel.add(messageBox, BorderLayout.WEST);
            }

            setOpaque(true);
            add(linePanel, BorderLayout.CENTER);

            return this;
        }
    }
}