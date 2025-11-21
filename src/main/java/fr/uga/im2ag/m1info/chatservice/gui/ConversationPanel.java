package fr.uga.im2ag.m1info.chatservice.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ConversationPanel extends JPanel {

    public static final class MessageItem {
        private final boolean mine;
        private final String author; //ignoré si mine=true
        private final String text;
        public MessageItem(boolean mine, String author, String text) {
            this.mine = mine; 
            this.author = author; 
            this.text = text;
        }
        public boolean isMine() { return mine; }
        public String getAuthor() { return author; }
        public String getText() { return text; }
    }


    public interface OnSendListener { void onSend(String text); }
    private ActionListener onBack;
    private OnSendListener onSend;


    private final JButton backButton = new JButton("< Retour");
    private final JLabel titleLabel = new JLabel("Conversation");

    private final DefaultListModel<MessageItem> msgModel = new DefaultListModel<>();
    private final JList<MessageItem> msgList = new JList<>(msgModel);

    private final JTextField input = new JTextField();
    private final JButton sendBtn = new JButton("Envoyer >");

    public ConversationPanel() {
        super(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0,0,0,0));

        // Header
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(new EmptyBorder(8, 8, 8, 8));
        backButton.setFocusable(false);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        top.add(backButton, BorderLayout.WEST);
        top.add(titleLabel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Liste des messages
        msgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        msgList.setCellRenderer(new MessageRenderer());
        JScrollPane sp = new JScrollPane(msgList);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);

        // Panneau du bas pour nouveau msg
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBorder(new EmptyBorder(8, 8, 8, 8));
        sendBtn.setFocusable(false);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        backButton.addActionListener(e -> { 
            if (onBack != null) onBack.actionPerformed(e); 
        });

        ActionListener doSend = e -> {
            String t = input.getText();
            if (t != null) 
                t = t.trim();
            if (t == null || t.isEmpty()) 
                return;
            if (onSend != null) 
                onSend.onSend(t);
            input.setText("");
        };
        sendBtn.addActionListener(doSend);
        input.addActionListener(doSend);
    }

    // nom du contact/groupe
    public void setConversationTitle(String title) { titleLabel.setText(title == null ? "" : title); }

    // Tous les msgs
    public void setMessages(List<MessageItem> items) {
        msgModel.clear();
        if (items != null) {
            for (MessageItem it : items) {
                msgModel.addElement(it);
            }
        }
        ensureLastVisible(); //ça va faire scroll vers le bas
    }

    // Ajoute un message, scroll vers bas.
    public void appendMessage(MessageItem item) {
        msgModel.addElement(item);
        ensureLastVisible();
    }

    public void setOnBack(ActionListener l) { this.onBack = l; }

    public void setOnSend(OnSendListener l) { this.onSend = l; }

    // Donne acès aux composants si besoin
    public JTextField getInputField() { return input; }
    public JButton getSendButton() { return sendBtn; }
    public JList<MessageItem> getMessageList() { return msgList; }

    private void ensureLastVisible() {
        int n = msgModel.getSize();
        if (n > 0) 
            msgList.ensureIndexIsVisible(n - 1);
    }

    private static final class MessageRenderer extends JPanel implements ListCellRenderer<MessageItem> {
        private final JLabel header = new JLabel();
        private final JTextArea bubble = new JTextArea();

        MessageRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 8, 6, 8));

            header.setFont(header.getFont().deriveFont(Font.PLAIN, 11f));
            header.setForeground(new Color(0,0,0,130));

            bubble.setLineWrap(true); //retour à la ligne auto pour pas couper
            bubble.setWrapStyleWord(true); //false = coupe n'importe où, true = coupe entre mots
            bubble.setEditable(false);
            bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

            JPanel v = new JPanel(); //header au dessus de la bubulle
            v.setOpaque(false);
            v.setLayout(new BoxLayout(v, BoxLayout.Y_AXIS));
            v.add(header);
            v.add(Box.createVerticalStrut(2)); 
            v.add(bubble);

            add(v, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MessageItem> list, MessageItem value,
                        int index, boolean isSelected, boolean cellHasFocus) {
            
            removeAll();
            boolean mine = value.isMine();
            String who = mine ? "vous" : (value.getAuthor() == null ? "??" : value.getAuthor());
            header.setText(who);
            bubble.setText(value.getText() == null ? "" : value.getText());

            // Couleurs des bubulles (bubulle ? c'est MA bubuuuuuuulle!!!!!)
            if (mine) {
                bubble.setBackground(new Color(225, 248, 238)); // vert clair
            } else {
                bubble.setBackground(new Color(240, 240, 240)); // gris clair
            }
            bubble.setForeground(Color.DARK_GRAY);
            bubble.setOpaque(true);

            JPanel messageBox = new JPanel();
            messageBox.setLayout(new BoxLayout(messageBox, BoxLayout.Y_AXIS));
            messageBox.setOpaque(false);
            messageBox.add(header);
            messageBox.add(Box.createVerticalStrut(2)); //padding de pixels entre le nom du gonz qui envoie le message et le msg
            messageBox.add(bubble);

            JPanel line = new JPanel(new BorderLayout());
            line.setOpaque(false);

            //gauche droite comme dans touuuuutes les messageries actuelles
            if (mine) {
                line.add(messageBox, BorderLayout.EAST);
            } else {
                line.add(messageBox, BorderLayout.WEST);
            }

            setOpaque(true);

            add(line, BorderLayout.CENTER);

            return this;
        }
    }
}
