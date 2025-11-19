package fr.uga.im2ag.m1info.chatservice.gui;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import fr.uga.im2ag.m1info.chatservice.client.Client;
import fr.uga.im2ag.m1info.chatservice.client.ClientContext;

import java.awt.*;
import java.awt.event.ActionListener;

/**
 * A reusable login panel for COO TchatApp.
 *
 * Responsibilities:
 *  - Renders username + password fields and a "Login" button
 *  - Exposes getters for fields
 *  - No logic inside
 */
public class LoginPanel extends JPanel {

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("Se connecter");
    private final JLabel errorLabel = new JLabel(" ");
    
    public LoginPanel() {

        super(new GridBagLayout());
        setBorder(new EmptyBorder(24, 24, 24, 24)); //Padding

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8); //marge autour des composants
        c.fill = GridBagConstraints.HORIZONTAL; //Composants s'étirent horizontalement
        c.weightx = 1.0;

        JLabel title = new JLabel("Connexion");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f)); //police plus grande plus gras
        c.gridx = 0; 
        c.gridy = 0; 
        c.gridwidth = 2; 
        c.fill = GridBagConstraints.NONE; //pas besoin d'étirer les comp au max
        c.anchor = GridBagConstraints.CENTER;
        add(title, c);

        c.gridy++; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel("id ? 0 pour créer un nouveau compte"), c);

        c.gridy++;
        usernameField.setColumns(18);
        add(usernameField, c);

        c.gridy++;
        add(new JLabel("Mot de passe"), c);

        c.gridy++;
        passwordField.setColumns(18);
        add(passwordField, c);

        c.gridy++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        loginButton.setPreferredSize(new Dimension(180, 36));
        add(loginButton, c);

        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        errorLabel.setForeground(new Color(160, 20, 20));
        add(errorLabel, c);
    }

    // Public API

    public String getUsername() { return usernameField.getText(); }

    public char[] getPassword() { return passwordField.getPassword(); }

    public JButton getLoginButton() { return loginButton; }
    
    public JLabel getErrorLabel() { return errorLabel; }

    public void showError(String msg) {
        errorLabel.setText(msg);
    }

    public void clearError() {
        errorLabel.setText(" ");
    }

    public void addSubmitListener(ActionListener l) {
        loginButton.addActionListener(l);
        passwordField.addActionListener(l); // Enter dans le champ mdp
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            JFrame loginFrame = new JFrame("Connexion");
            LoginPanel loginPanel = new LoginPanel();

            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loginFrame.setContentPane(loginPanel);
            loginFrame.pack();
            loginFrame.setLocationRelativeTo(null);
            loginFrame.setVisible(true);
        });

    }
}
