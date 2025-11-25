package fr.uga.im2ag.m1info.chatservice.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import javafx.scene.layout.Border;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        private final String messageId;
        private final String replyToMessageId;

        public MessageItem(boolean mine, String author, String text, String messageId, String replyToMessageId) {
            this.mine = mine;
            this.author = author;
            this.text = text;
            this.messageId = messageId;
            this.replyToMessageId = replyToMessageId;
        }

        public MessageItem(boolean mine, String author, String text) {
            this(mine, author, text, null, null);
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

        public String getMessageId() {
            return messageId;
        }

        public String getReplyToMessageId() {
            return replyToMessageId;
        }
    }

    /**
     * Callback interface for message sending.
     */
    @FunctionalInterface
    public interface OnSendListener {
        void onSend(String text, String replyToMessageId);
    }

    /**
     * Callback interface for replying to a message.
     */
    @FunctionalInterface
    public interface OnReplyListener {
        void onReply(MessageItem message);
    }

     /**
     * Callback interface for options in conv.
     */
    @FunctionalInterface
    public interface OnOptionListener {
        void onOption();
    }

    private final JButton backButton;
    private final JLabel titleLabel;
    private final DefaultListModel<MessageItem> messageModel;
    private final JList<MessageItem> messageList;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JPanel replyPreviewPanel;
    private final JLabel replyPreviewLabel;
    private final JButton cancelReplyButton;
    private final JButton optionsButton;

    private ActionListener onBack;
    private OnSendListener onSend;
    private OnReplyListener onReply;
    private OnOptionListener onOption;

    private MessageItem replyingTo = null;
    private final Map<String, MessageItem> messageCache = new HashMap<>();

    private int hoveredIndex = -1;

    public ConversationPanel() {
        super(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        this.backButton = new JButton("< Retour");
        this.titleLabel = new JLabel("Conversation");
        this.messageModel = new DefaultListModel<>();
        this.messageList = new JList<>(messageModel);
        this.inputField = new JTextField();
        this.sendButton = new JButton("Envoyer");
        this.replyPreviewPanel = new JPanel(new BorderLayout(8, 0));
        this.replyPreviewLabel = new JLabel();
        this.cancelReplyButton = new JButton("✕");
        this.optionsButton = new JButton("⋮");
        setupLayout();
        setupListeners();
        setupContextMenu();
        setupHoverEffect();
    }

    private void setupLayout() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        backButton.setFocusable(false);
        optionsButton.setFocusable(false);
        headerPanel.add(backButton, BorderLayout.WEST);
        headerPanel.add(optionsButton, BorderLayout.EAST);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Message list
        messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageList.setCellRenderer(new MessageRenderer());
        JScrollPane scrollPane = new JScrollPane(messageList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom container with reply preview and input
        JPanel bottomContainer = new JPanel(new BorderLayout(0, 0));

        // Reply preview panel (hidden by default)
        replyPreviewPanel.setBorder(new EmptyBorder(8, 8, 4, 8));
        replyPreviewPanel.setBackground(new Color(240, 240, 240));
        replyPreviewPanel.setVisible(false);

        JPanel replyContent = new JPanel(new BorderLayout(8, 0));
        replyContent.setOpaque(false);

        JLabel replyIcon = new JLabel("↩");
        replyIcon.setFont(replyIcon.getFont().deriveFont(Font.BOLD, 14f));
        replyIcon.setForeground(new Color(100, 100, 100));
        replyContent.add(replyIcon, BorderLayout.WEST);

        replyPreviewLabel.setFont(replyPreviewLabel.getFont().deriveFont(Font.PLAIN, 12f));
        replyPreviewLabel.setForeground(new Color(80, 80, 80));
        replyContent.add(replyPreviewLabel, BorderLayout.CENTER);

        replyPreviewPanel.add(replyContent, BorderLayout.CENTER);

        cancelReplyButton.setFocusable(false);
        cancelReplyButton.setMargin(new Insets(2, 6, 2, 6));
        cancelReplyButton.setFont(cancelReplyButton.getFont().deriveFont(Font.BOLD, 12f));
        replyPreviewPanel.add(cancelReplyButton, BorderLayout.EAST);

        bottomContainer.add(replyPreviewPanel, BorderLayout.NORTH);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        sendButton.setFocusable(false);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        bottomContainer.add(inputPanel, BorderLayout.SOUTH);
        add(bottomContainer, BorderLayout.SOUTH);
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
                    String replyId = replyingTo != null ? replyingTo.getMessageId() : null;
                    onSend.onSend(text.trim(), replyId);
                }
                inputField.setText("");
                clearReplyPreview();
            }
        };

        ActionListener openActionMenu = e -> {


        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
        optionsButton.addActionListener(openActionMenu);

        cancelReplyButton.addActionListener(e -> clearReplyPreview());
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem replyItem = new JMenuItem("Répondre");
        replyItem.addActionListener(e -> {
            int index = messageList.getSelectedIndex();
            if (index >= 0) {
                MessageItem message = messageModel.getElementAt(index);
                showReplyPreview(message);
                if (onReply != null) {
                    onReply.onReply(message);
                }
            }
        });
        contextMenu.add(replyItem);

        JMenuItem copyItem = new JMenuItem("Copier le texte");
        copyItem.addActionListener(e -> {
            int index = messageList.getSelectedIndex();
            if (index >= 0) {
                MessageItem message = messageModel.getElementAt(index);
                if (message.getText() != null) {
                    StringSelection selection = new StringSelection(message.getText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                }
            }
        });
        contextMenu.add(copyItem);

        messageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseEvent(e);
            }

            private void handleMouseEvent(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = messageList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Rectangle bounds = messageList.getCellBounds(index, index);
                        if (bounds != null && bounds.contains(e.getPoint())) {
                            messageList.setSelectedIndex(index);
                            contextMenu.show(messageList, e.getX(), e.getY());
                        }
                    }
                }
            }
        });
    }

    private void setupHoverEffect() {
        messageList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = messageList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    Rectangle bounds = messageList.getCellBounds(index, index);
                    if (bounds != null && bounds.contains(e.getPoint())) {
                        if (hoveredIndex != index) {
                            hoveredIndex = index;
                            messageList.repaint(bounds);
                        }
                        return;
                    }
                }
                suppressHover();
            }
        });

        messageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                suppressHover();
            }
        });
    }

    private void suppressHover() {
        if (hoveredIndex != -1) {
            int oldIndex = hoveredIndex;
            hoveredIndex = -1;
            if (oldIndex < messageModel.getSize()) {
                Rectangle bounds = messageList.getCellBounds(oldIndex, oldIndex);
                if (bounds != null) {
                    messageList.repaint(bounds);
                }
            }
        }
    }

    private void showReplyPreview(MessageItem message) {
        replyingTo = message;
        String previewText = message.getText();
        if (previewText != null && previewText.length() > 50) {
            previewText = previewText.substring(0, 50) + "...";
        }
        String author = message.isMine() ? "Vous" : (message.getAuthor() != null ? message.getAuthor() : "??");
        replyPreviewLabel.setText("Répondre à " + author + ": " + previewText);
        replyPreviewPanel.setVisible(true);
        inputField.requestFocus();
    }

    private void clearReplyPreview() {
        replyingTo = null;
        replyPreviewPanel.setVisible(false);
    }

    private void ensureLastVisible() {
        int size = messageModel.getSize();
        if (size > 0) {
            messageList.ensureIndexIsVisible(size - 1);
        }
    }

    private void rebuildMessageCache() {
        messageCache.clear();
        for (int i = 0; i < messageModel.getSize(); i++) {
            MessageItem item = messageModel.getElementAt(i);
            if (item.getMessageId() != null) {
                messageCache.put(item.getMessageId(), item);
            }
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
        rebuildMessageCache();
        ensureLastVisible();
    }

    /**
     * Append a message to the conversation.
     *
     * @param message the message to append
     */
    public void appendMessage(MessageItem message) {
        messageModel.addElement(message);
        if (message.getMessageId() != null) {
            messageCache.put(message.getMessageId(), message);
        }
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
     * Set the callback for replying to a message.
     *
     * @param listener the reply listener
     */
    public void setOnReply(OnReplyListener listener) {
        this.onReply = listener;
    }

    public void setOnOptions(OnOptionListener listener) {
        this.onOption = listener;
    }

    /**
     * Get the input text field.
     *
     * @return the input field
     */
    public JTextField getInputField() {
        return inputField;
    }

    /**
     * Clear the reply preview (cancels reply mode).
     */
    public void cancelReply() {
        clearReplyPreview();
    }

    // ----------------------- Cell Renderer -----------------------

    /**
     * Custom renderer for message items with bubble style, reply references, and hover effect.
     */
    private final class MessageRenderer extends JPanel implements ListCellRenderer<MessageItem> {
        private static final Color COLOR_MY_MESSAGE = new Color(225, 248, 238);
        private static final Color COLOR_OTHER_MESSAGE = new Color(240, 240, 240);
        private static final Color COLOR_MY_MESSAGE_HOVER = new Color(210, 243, 228);
        private static final Color COLOR_OTHER_MESSAGE_HOVER = new Color(230, 230, 230);
        private static final Color COLOR_REPLY_REF = new Color(200, 200, 200);

        private final JLabel headerLabel;
        private final JTextArea bubbleArea;
        private final JPanel replyRefPanel;
        private final JLabel replyRefLabel;

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

            // Reply reference panel
            replyRefPanel = new JPanel(new BorderLayout(4, 0));
            replyRefPanel.setBorder(new EmptyBorder(4, 12, 4, 12));
            replyRefPanel.setOpaque(true);

            JLabel replyIcon = new JLabel("↩");
            replyIcon.setFont(replyIcon.getFont().deriveFont(Font.PLAIN, 10f));
            replyIcon.setForeground(new Color(100, 100, 100));
            replyRefPanel.add(replyIcon, BorderLayout.WEST);

            replyRefLabel = new JLabel();
            replyRefLabel.setFont(replyRefLabel.getFont().deriveFont(Font.ITALIC, 11f));
            replyRefLabel.setForeground(new Color(80, 80, 80));
            replyRefPanel.add(replyRefLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MessageItem> list,
                                                      MessageItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            removeAll();

            boolean isMine = value.isMine();
            boolean isHovered = (index == hoveredIndex);
            String author = isMine ? "Vous" : ((value.getAuthor() == null) ? "??" : value.getAuthor());

            headerLabel.setText(author);
            bubbleArea.setText((value.getText() == null) ? "" : value.getText());

            Color bgColor;
            if (isHovered) {
                bgColor = isMine ? COLOR_MY_MESSAGE_HOVER : COLOR_OTHER_MESSAGE_HOVER;
            } else {
                bgColor = isMine ? COLOR_MY_MESSAGE : COLOR_OTHER_MESSAGE;
            }
            bubbleArea.setBackground(bgColor);
            bubbleArea.setForeground(Color.DARK_GRAY);
            bubbleArea.setOpaque(true);

            // Build message box
            JPanel messageBox = new JPanel();
            messageBox.setLayout(new BoxLayout(messageBox, BoxLayout.Y_AXIS));
            messageBox.setOpaque(false);
            messageBox.add(headerLabel);
            messageBox.add(Box.createVerticalStrut(2));

            // Add reply reference if present
            if (value.getReplyToMessageId() != null) {
                MessageItem referencedMessage = messageCache.get(value.getReplyToMessageId());
                if (referencedMessage != null) {
                    String refText = referencedMessage.getText();
                    if (refText != null && refText.length() > 40) {
                        refText = refText.substring(0, 40) + "...";
                    }
                    String refAuthor = referencedMessage.isMine() ? "Vous" :
                            (referencedMessage.getAuthor() != null ? referencedMessage.getAuthor() : "??");
                    replyRefLabel.setText(refAuthor + ": " + refText);

                    // Couleur de référence adaptée au hover
                    Color refBgColor;
                    if (isHovered) {
                        refBgColor = isMine ? COLOR_MY_MESSAGE_HOVER.darker() : COLOR_OTHER_MESSAGE_HOVER.darker();
                    } else {
                        refBgColor = isMine ? COLOR_MY_MESSAGE.darker() : COLOR_OTHER_MESSAGE.darker();
                    }
                    replyRefPanel.setBackground(refBgColor);
                    messageBox.add(replyRefPanel);
                    messageBox.add(Box.createVerticalStrut(2));
                }
            }

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