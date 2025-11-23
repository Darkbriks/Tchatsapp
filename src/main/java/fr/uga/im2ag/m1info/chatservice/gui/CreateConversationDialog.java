package fr.uga.im2ag.m1info.chatservice.gui;

import fr.uga.im2ag.m1info.chatservice.client.repository.ContactClientRepository;
import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateConversationDialog extends JDialog {

    public static final class Result {
        private final String conversationName;
        private final Set<Integer> participantIds;
        private final boolean isGroup;

        public Result(String conversationName, Set<Integer> participantIds, boolean isGroup) {
            this.conversationName = conversationName;
            this.participantIds = participantIds;
            this.isGroup = isGroup;
        }

        public String getConversationName() { return conversationName; }
        public Set<Integer> getParticipantIds() { return participantIds; }
        public boolean isGroup() { return isGroup; }
    }

    // Pojo to add contacts in JLsit
    private static final class ContactItem {
        final int id;
        final String pseudo;
        ContactItem(int id, String pseudo) {
            this.id = id; this.pseudo = pseudo;
        }
        
        public int getId() { return id; }
        public String getPseudo() { return pseudo; }

        @Override
        public String toString() { return pseudo + ", id = " + id; }
    }

    private final JRadioButton rbPrivate = new JRadioButton("Conversation privée", true);
    private final JRadioButton rbGroup   = new JRadioButton("Conversation de groupe");
    private final JTextField nameField   = new JTextField(); // Name of the group (auto for priavte)
    private final JList<ContactItem> contactList = new JList<>();
    private final JButton btnOk = new JButton("Créer");
    private final JButton btnCancel = new JButton("Annuler");

    private Result result;

    /** Create a conversation dialog which will allow to create a new conversation from the selected participants
     * 
     * @param owner the main Frame
     * @param contactRepo contact repository of the client
     * @param currentUserId the id of the client
     */

    public CreateConversationDialog(Frame owner,
                                    ContactClientRepository contactRepo,
                                    int currentUserId) {
        super(owner, "Nouvelle conversation", true);



        setLayout(new BorderLayout(8, 8));
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        // Conversation type
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbPrivate);
        bg.add(rbGroup);
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typePanel.add(rbPrivate);
        typePanel.add(rbGroup);

        c.gridx = 0; 
        c.gridy = 0;
        c.gridwidth = 2;
        content.add(new JLabel("Conversation type :"), c);
        c.gridy++;
        content.add(typePanel, c);

        // Name of the conv
        c.gridy++;
        content.add(new JLabel("Name of the conv, optional for private ones :"), c);
        c.gridy++;
        nameField.setColumns(20);
        content.add(nameField, c);

        // contact list
        c.gridy++;
        content.add(new JLabel("Choisissez les participants :"), c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;

        // load contacts from repo
        List<ContactItem> items = new ArrayList<>();
        for (ContactClient contact : contactRepo.findAll()) {
            if (contact.getContactId() == currentUserId) {
                continue; // we don't show the client itself if it has been added by mistake
            }
            items.add(new ContactItem(contact.getContactId(), contact.getPseudo()));
        }
        contactList.setListData(items.toArray(new ContactItem[0]));
        contactList.setVisibleRowCount(8);
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(contactList);
        content.add(scroll, c);

        // 4) boottom panel
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnCancel);
        buttons.add(btnOk);

        add(content, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        // private vs group conv
        rbPrivate.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }
        });
        rbGroup.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                contactList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }
        });

        btnCancel.addActionListener(e -> {
            result = null;
            dispose();
        });

        btnOk.addActionListener(e -> onValidate(currentUserId));

        pack();
        setLocationRelativeTo(owner);
    }

    private void onValidate(int currentUserId) {
        boolean isGroup = rbGroup.isSelected();
        List<ContactItem> selected = contactList.getSelectedValuesList();

        if (!isGroup) {
            // private conv
            if (selected.size() != 1) {
                JOptionPane.showMessageDialog(this,
                        "Please select only one ocntact for private conv",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            // group conv
            if (selected.size() < 2) {
                JOptionPane.showMessageDialog(this,
                        "Please select at least 2 contacts to create group conv",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            if (!isGroup) {
                // default name of priv conv = pseudo of contact
                name = selected.get(0).getPseudo();
            } else {
                // Group : if no name provided, some pseudos, and "..." if > 3
                StringBuilder sb = new StringBuilder("Group : ");
                int count = 0;
                for (ContactItem ci : selected) {
                    if (count > 0) sb.append(", ");
                    sb.append(ci.pseudo);
                    count++;
                    if (count >= 3) {
                        sb.append("...");
                        break;
                    }
                }
                name = sb.toString();
                }
        }

        // Participants = selected contacts
        Set<Integer> ids = new HashSet<>();
        if (selected.size()==1) ids.add(selected.get(0).getId());
        else { 
            for (ContactItem ci : selected) {
                ids.add(ci.id);
            }
        }
        result = new Result(name, ids, isGroup);
        dispose();
    }

    public Result showDialog() {
        setVisible(true); // block until dispose
        return result;
    }
}
