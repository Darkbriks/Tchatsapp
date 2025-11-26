package fr.uga.im2ag.m1info.chatservice.gui;

import fr.uga.im2ag.m1info.chatservice.client.model.Media;
import fr.uga.im2ag.m1info.chatservice.client.model.VirtualMedia;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
        private final Media attachedMedia;

        public MessageItem(boolean mine, String author, String text, String messageId, String replyToMessageId, Media attachedMedia) {
            this.mine = mine;
            this.author = author;
            this.text = text;
            this.messageId = messageId;
            this.replyToMessageId = replyToMessageId;
            this.attachedMedia = attachedMedia;
        }

        public MessageItem(boolean mine, String author, String text, String messageId, String replyToMessageId) {
            this(mine, author, text, messageId, replyToMessageId, null);
        }

        public MessageItem(boolean mine, String author, String text) {
            this(mine, author, text, null, null, null);
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

        public Media getAttachedMedia() {
            return attachedMedia;
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
    private final JButton attachFileButton;
    private final JPanel replyPreviewPanel;
    private final JLabel replyPreviewLabel;
    private final JButton cancelReplyButton;
    private final JButton optionsButton;

    // Callbacks
    private ActionListener onBack;
    private OnSendListener onSend;
    private OnReplyListener onReply;
    private OnOptionListener onOption;
    private Consumer<File> onFileSend;
    private Consumer<String> onFileDownload;
    private Consumer<String> onFileOpen;

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
        this.attachFileButton = new JButton("Joindre");
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
        MessageRenderer renderer = new MessageRenderer();
        renderer.setFileActionCallbacks(
                mediaId -> {
                    if (onFileDownload != null) {
                        onFileDownload.accept(mediaId);
                    }
                },
                mediaId -> {
                    if (onFileOpen != null) {
                        onFileOpen.accept(mediaId);
                    }
                }
        );
        messageList.setCellRenderer(renderer);

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
        JPanel inputPanel = new JPanel(new BorderLayout(4, 4));
        inputPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Left side: attach button
        attachFileButton.setToolTipText("Joindre un fichier");
        attachFileButton.setFocusable(false);
        inputPanel.add(attachFileButton, BorderLayout.WEST);

        // Center: text input
        inputPanel.add(inputField, BorderLayout.CENTER);

        // Right side: send button
        sendButton.setFocusable(false);
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
            if (onOption != null) {
                onOption.onOption();
            }
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
        optionsButton.addActionListener(openActionMenu);

        cancelReplyButton.addActionListener(e -> clearReplyPreview());

        attachFileButton.addActionListener(e -> handleAttachFile());
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

    private void handleAttachFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Sélectionner un fichier à envoyer");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (onFileSend != null) {
                onFileSend.accept(selectedFile);
            }
        }
    }

    private void showReplyPreview(MessageItem message) {
        replyingTo = message;
        String preview = message.getAuthor() != null
                ? message.getAuthor() + ": " + message.getText()
                : message.getText();
        if (preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }
        replyPreviewLabel.setText(preview);
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
        messageCache.clear();
        if (messages != null) {
            for (MessageItem item : messages) {
                messageModel.addElement(item);
                if (item.getMessageId() != null) {
                    messageCache.put(item.getMessageId(), item);
                }
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

    public void setOnOption(OnOptionListener listener) {
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

    /**
     * Set the callback for file attachment.
     */
    public void setOnFileSend(Consumer<File> callback) {
        this.onFileSend = callback;
    }

    /**
     * Set the callback for file download.
     */
    public void setOnFileDownload(Consumer<String> callback) {
        this.onFileDownload = callback;
    }

    /**
     * Set the callback for file opening.
     */
    public void setOnFileOpen(Consumer<String> callback) {
        this.onFileOpen = callback;
    }

    // ----------------------- Cell Renderer -----------------------

    /**
     * Custom renderer for message items with bubble style, reply references, and hover effect.
     */
    private class MessageRenderer extends JPanel implements ListCellRenderer<MessageItem> {
        private final JLabel authorLabel;
        private final JTextArea contentArea;
        private final JPanel replyIndicator;
        private final JLabel replyText;
        private final JPanel filePanel;
        private final JLabel fileIconLabel;
        private final JLabel fileNameLabel;
        private final JLabel fileSizeLabel;
        private final JButton downloadButton;
        private final JButton openButton;

        private Consumer<String> onFileDownloadCallback;
        private Consumer<String> onFileOpenCallback;

        public MessageRenderer() {
            super(new BorderLayout(8, 4));
            setBorder(new EmptyBorder(8, 8, 8, 8));

            // Author label
            authorLabel = new JLabel();
            authorLabel.setFont(authorLabel.getFont().deriveFont(Font.BOLD, 11f));
            add(authorLabel, BorderLayout.NORTH);

            // Center panel for reply indicator + content + file
            JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
            centerPanel.setOpaque(false);

            // Reply indicator (hidden by default)
            replyIndicator = new JPanel(new BorderLayout(6, 0));
            replyIndicator.setOpaque(false);
            replyIndicator.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(100, 149, 237)),
                    new EmptyBorder(4, 6, 4, 0)
            ));
            replyText = new JLabel();
            replyText.setFont(replyText.getFont().deriveFont(Font.ITALIC, 10f));
            replyText.setForeground(new Color(100, 100, 100));
            replyIndicator.add(replyText, BorderLayout.CENTER);
            replyIndicator.setVisible(false);
            centerPanel.add(replyIndicator, BorderLayout.NORTH);

            // Content area (text)
            contentArea = new JTextArea();
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setEditable(false);
            contentArea.setOpaque(false);
            contentArea.setFont(contentArea.getFont().deriveFont(13f));
            centerPanel.add(contentArea, BorderLayout.CENTER);

            // File panel (for attached files)
            filePanel = new JPanel(new BorderLayout(8, 4));
            filePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    new EmptyBorder(8, 8, 8, 8)
            ));
            filePanel.setBackground(new Color(245, 245, 250));

            // File icon and info
            JPanel fileInfoPanel = new JPanel(new BorderLayout(8, 2));
            fileInfoPanel.setOpaque(false);

            fileIconLabel = new JLabel();
            fileIconLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
            fileInfoPanel.add(fileIconLabel, BorderLayout.WEST);

            JPanel fileDetailsPanel = new JPanel(new GridLayout(2, 1, 0, 2));
            fileDetailsPanel.setOpaque(false);
            fileNameLabel = new JLabel();
            fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 12f));
            fileSizeLabel = new JLabel();
            fileSizeLabel.setFont(fileSizeLabel.getFont().deriveFont(10f));
            fileSizeLabel.setForeground(Color.GRAY);
            fileDetailsPanel.add(fileNameLabel);
            fileDetailsPanel.add(fileSizeLabel);
            fileInfoPanel.add(fileDetailsPanel, BorderLayout.CENTER);

            filePanel.add(fileInfoPanel, BorderLayout.CENTER);

            // Action buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            buttonPanel.setOpaque(false);

            downloadButton = new JButton("Télécharger");
            downloadButton.setFont(downloadButton.getFont().deriveFont(11f));
            downloadButton.setFocusPainted(false);

            openButton = new JButton("Ouvrir");
            openButton.setFont(openButton.getFont().deriveFont(11f));
            openButton.setFocusPainted(false);

            buttonPanel.add(downloadButton);
            buttonPanel.add(openButton);
            filePanel.add(buttonPanel, BorderLayout.SOUTH);

            centerPanel.add(filePanel, BorderLayout.SOUTH);
            filePanel.setVisible(false); // Hidden by default

            add(centerPanel, BorderLayout.CENTER);
        }

        public void setFileActionCallbacks(Consumer<String> onDownload, Consumer<String> onOpen) {
            this.onFileDownloadCallback = onDownload;
            this.onFileOpenCallback = onOpen;
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends MessageItem> list,
                MessageItem item,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            // Author
            if (item.isMine()) {
                authorLabel.setText("Vous");
                authorLabel.setForeground(new Color(0, 100, 200));
            } else {
                String author = item.getAuthor() != null ? item.getAuthor() : "Unknown";
                authorLabel.setText(author);
                authorLabel.setForeground(new Color(100, 100, 100));
            }

            // Reply indicator
            if (item.getReplyToMessageId() != null) {
                MessageItem repliedTo = messageCache.get(item.getReplyToMessageId());
                if (repliedTo != null) {
                    String replyAuthor = repliedTo.isMine() ? "Vous" :
                            (repliedTo.getAuthor() != null ? repliedTo.getAuthor() : "Unknown");
                    String replyContent = repliedTo.getText();
                    if (replyContent.length() > 30) {
                        replyContent = replyContent.substring(0, 30) + "...";
                    }
                    replyText.setText("↩ " + replyAuthor + ": " + replyContent);
                    replyIndicator.setVisible(true);
                } else {
                    replyIndicator.setVisible(false);
                }
            } else {
                replyIndicator.setVisible(false);
            }

            // Content (text)
            contentArea.setText(item.getText());

            // File attachment
            Media media = item.getAttachedMedia();
            if (media instanceof VirtualMedia virtualMedia) {
                filePanel.setVisible(true);

                // File icon
                fileIconLabel.setText(virtualMedia.getIcon());

                // File name and size
                fileNameLabel.setText(virtualMedia.getFileName());
                fileSizeLabel.setText(virtualMedia.getFormattedFileSize());

                // Button actions
                String mediaId = virtualMedia.getMediaId();

                // Remove old listeners
                for (ActionListener al : downloadButton.getActionListeners()) {
                    downloadButton.removeActionListener(al);
                }
                for (ActionListener al : openButton.getActionListeners()) {
                    openButton.removeActionListener(al);
                }

                // Add new listeners
                downloadButton.addActionListener(e -> {
                    if (onFileDownloadCallback != null) {
                        onFileDownloadCallback.accept(mediaId);
                    }
                });

                openButton.addActionListener(e -> {
                    if (onFileOpenCallback != null) {
                        onFileOpenCallback.accept(mediaId);
                    }
                });

            } else {
                filePanel.setVisible(false);
            }

            // Background color
            Color bgColor;
            if (isSelected) {
                bgColor = list.getSelectionBackground();
            } else if (index == hoveredIndex) {
                bgColor = item.isMine() ? new Color(200, 230, 255) : new Color(245, 245, 245);
            } else {
                bgColor = item.isMine() ? new Color(220, 240, 255) : Color.WHITE;
            }
            setBackground(bgColor);

            return this;
        }
    }
}