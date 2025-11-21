package fr.uga.im2ag.m1info.chatservice.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class HomePanel extends JPanel {
   
    public static final class ConversationItem {
        private final String id;
        private final String title;       // nom du contact ou du groupe
        private final String lastMessage; // aperçu
        public ConversationItem(String id, String title, String lastMessage) {
            this.id = id; 
            this.title = title; 
            this.lastMessage = lastMessage;
        }
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getLastMessage() { return lastMessage; }
        @Override public String toString() { return title; }
    }

    private final JTextField searchField = new JTextField();
    private final DefaultListModel<ConversationItem> listModel = new DefaultListModel<>();
    private final JList<ConversationItem> conversationList = new JList<>(listModel);
    private final JButton newButton = new JButton("Nouveau +");

    private final List<ConversationItem> master = new ArrayList<>();

    private ActionListener onNewConversation;

    public HomePanel() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Headre
        JPanel header = new JPanel(new BorderLayout(6, 6));
        JLabel title = new JLabel("Conversations");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f)); //plus gros/gras
        header.add(title, BorderLayout.NORTH);
        searchField.setToolTipText("Rechercher une conversation"); //c'est pratique ça en vrai !
        header.add(searchField, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);


        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setCellRenderer(new ConversationRenderer());
        JScrollPane sp = new JScrollPane(conversationList);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.add(Box.createHorizontalGlue()); //composant spécial qui prend tout l’espace horizontal disponible et pousse les composants placés après à dorite
        newButton.setFocusable(false); //pas de petites bordure bleue dégueu et pas sélectionnable via tab
        south.add(newButton);
        add(south, BorderLayout.SOUTH);

        installSearchFilter();
        newButton.addActionListener(e -> { 
            if (onNewConversation != null) 
                onNewConversation.actionPerformed(e); 
        });
    }


    public void setConversations(List<ConversationItem> conversations) {
        master.clear();
        if (conversations != null) 
            master.addAll(conversations);
        applyFilter(searchField.getText());
    }

    public void setConversationsFromStrings(List<String> titles) {
        master.clear();
        if (titles != null) {
            int i = 0;
            for (String t : titles) {
                master.add(new ConversationItem("c" + (i++), Objects.toString(t, ""), ""));
            }
        }
        applyFilter(searchField.getText());
    }

    public ConversationItem getSelectedConversation() { return conversationList.getSelectedValue(); }

    public void addConversationSelectionListener(ListSelectionListener l) { conversationList.addListSelectionListener(l); }

    public void setOnNewConversation(ActionListener l) { this.onNewConversation = l; }

    public JTextField getSearchField() { return searchField; }
    public JList<ConversationItem> getConversationList() { return conversationList; }
    public JButton getNewButton() { return newButton; }

    private void installSearchFilter() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { applyFilter(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { applyFilter(searchField.getText()); }
        });
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        listModel.clear();
        for (ConversationItem it : master) {
            if (q.isEmpty() || it.getTitle().toLowerCase().contains(q))
                listModel.addElement(it);
        }
        if (!listModel.isEmpty() && conversationList.getSelectedIndex() == -1) 
            conversationList.setSelectedIndex(0);
    }

    // Le renderer de la page. Pas réussi à faire de fondu/dégradé, je m'y remettrai à la fin du proj histoire si on a le temps maisc 'est chiant
    private static final class ConversationRenderer extends JPanel implements ListCellRenderer<ConversationItem> {
        private final JLabel title = new JLabel();
        private final JLabel preview = new JLabel();
        private static final int MAX_PREVIEW_CHARS = 77; //modifiable, mais ça rappelle le 80 des écrans encor eplus vieux que moi

        ConversationRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10,10,10,10));
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            preview.setForeground(new Color(0,0,0,160));

            JPanel center = new JPanel(new BorderLayout());
            center.setOpaque(false);
            center.add(title, BorderLayout.NORTH);
            center.add(preview, BorderLayout.SOUTH);
            add(center, BorderLayout.CENTER);
        }


        @Override
        public Component getListCellRendererComponent(JList<? extends ConversationItem> list, 
                        ConversationItem value, int index, boolean isSelected, boolean cellHasFocus) {
            title.setText(value.getTitle());
            String msg = value.getLastMessage() == null ? "" : value.getLastMessage();
            if (msg.length() > MAX_PREVIEW_CHARS)
                msg = msg.substring(0, MAX_PREVIEW_CHARS - 3) + "...";
            preview.setText(msg);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } 
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setOpaque(true);
            return this;
        }
    }
}