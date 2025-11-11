/*
 * Copyright (c) 2025.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of TchatsApp.
 *
 * TchatsApp is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * TchatsApp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TchatsApp. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.client.handlers.ErrorMessageHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.ManagementMessageHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.MediaMessageHandler;
import fr.uga.im2ag.m1info.chatservice.client.handlers.TextMessageHandler;
import fr.uga.im2ag.m1info.chatservice.common.*;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.*;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * A basic client for Tchatsapp.
 * It allows to connect to a server, send and receive packets.
 */
public class Client {

    private int clientId;
    private Socket cnx;
    private PacketProcessor processor;
    private MessageIdGenerator messageIdGenerator;

    public Client() {
        this(0);
    }
    public Client(int clientId) {
        this.clientId=clientId;
        this.messageIdGenerator = new ShaIdGenerator();
    }

    /**
     * Attemps to connect to a given server.
     * @param host
     * @param port
     * @param username
     * @return false if there is an existing connection or if the connection fails.
     */
    public boolean connect(String host, int port, String username) {
        if (cnx!=null && cnx.isConnected()) return false;
        try {
            cnx = new Socket("localhost",1666);
            DataOutputStream dos = new DataOutputStream(cnx.getOutputStream());
            DataInputStream dis = new DataInputStream(cnx.getInputStream());

            Packet connectionPacket;
            if (clientId == 0) {
                ManagementMessage createMsg = (ManagementMessage) MessageFactory.create(MessageType.CREATE_USER, 0, 0);
                createMsg.addParam("pseudo", username);
                connectionPacket = createMsg.toPacket();
            } else {
                ManagementMessage connectMsg = (ManagementMessage) MessageFactory.create(MessageType.CONNECT_USER, clientId, 0);
                connectionPacket = connectMsg.toPacket();
            }

            connectionPacket.writeTo(dos);

            Packet responsePacket = Packet.readFrom(dis);
            ProtocolMessage responseMsg = MessageFactory.fromPacket(responsePacket);

            if (responseMsg.getMessageType() == MessageType.ERROR) {
                ErrorMessage errorMsg = (ErrorMessage) responseMsg;
                System.err.println("Connection failed:");
                System.err.println("\tLevel: " + errorMsg.getErrorLevel());
                System.err.println("\tType: " + errorMsg.getErrorType());
                System.err.println("\tMessage: " + errorMsg.getErrorMessage());
                cnx.close();
                cnx = null;
                return false;
            }

            if (responseMsg.getMessageType() == MessageType.CREATE_USER || responseMsg.getMessageType() == MessageType.CONNECT_USER) {
                ManagementMessage mgmtMsg = (ManagementMessage) responseMsg;
                clientId = responsePacket.to();
                System.out.println("Connected with client ID: " + clientId);
                if (mgmtMsg.getParam("pseudo") != null) {
                    System.out.println("Pseudo: " + mgmtMsg.getParam("pseudo"));
                }
            }

            // reception thread
            new Thread(() ->{
                try {
                    while (cnx!=null && !cnx.isInputShutdown()) {
                        Packet m = Packet.readFrom(dis);
                        processReceivedPacket(m);
                    }
                } catch (IOException e) {
                    if (cnx==null || !cnx.isConnected()) return;
                    e.printStackTrace();
                }

            }).start();
            return cnx.isConnected();
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public int getClientId() {
        return clientId;
    }

    public void setMessageIdGenerator(MessageIdGenerator generator) {
        this.messageIdGenerator=generator;
    }

    /**
     * Set the packet processor to be called when packet are received by the client
     * @param p
     */
    public void setPacketProcessor(PacketProcessor p ) {
        processor=p;
    }

    private void processReceivedPacket(Packet m) {
        if (processor!=null) processor.process(MessageFactory.fromPacket(m));
    }

    public boolean isConnected() {
        return cnx!=null && cnx.isConnected();
    }

    public void disconnect() {
        try {
            if (cnx != null) cnx.close();
            cnx = null;
        } catch (IOException e) {/* ignored */}
    }

    public boolean sendPacket(Packet m) {
        //if (m.from()!=clientId) throw new RuntimeException("Message from field must be equals to clientId");
        System.out.println(m);
        try {
            DataOutputStream dos = new DataOutputStream(cnx.getOutputStream());
            m.writeTo(dos);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendMedia(String msg, int to){
        try {
            String fileName = msg.substring(1);
            InputStream fileStream = new FileInputStream(new File(fileName));
            int count = 0;
            byte[] buffer = new byte[8192]; // or 4096, or more
            while ((count = fileStream.read(buffer)) > 0)
            {
                MediaMessage mediaMsg = (MediaMessage) MessageFactory.create(MessageType.MEDIA, clientId, to);
                mediaMsg.generateNewMessageId(messageIdGenerator);
                mediaMsg.setMediaName(fileName);
                mediaMsg.setContent(buffer);
                mediaMsg.setSizeContent(count);
                sendPacket(mediaMsg.toPacket());
            }
            fileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** A basic client in command line **/
    public static void main(String[] args) throws IOException, InterruptedException {
        final int serverId = 0;
        Scanner sc = new Scanner(System.in);
        System.out.println("Votre id ? (0 pour en créer un nouveau)");
        int clientId =  sc.nextInt();
        sc.nextLine();
        String pseudo = "";
        if (clientId == 0) {
            System.out.println("Choisissez votre pseudo :");
            pseudo = sc.nextLine();
        }

        Client c = new Client(clientId);

        ClientPaquetRouter router = new ClientPaquetRouter();
        router.addHandler(new TextMessageHandler());
        router.addHandler(new MediaMessageHandler());
        router.addHandler(new ErrorMessageHandler());
        router.addHandler(new ManagementMessageHandler());
        c.setPacketProcessor(router);

        if (c.connect("localhost",1666, pseudo)) {

            clientId = c.getClientId();

            while (true) {
                System.out.println("Quelle action voulez-vous faire ?" +
                        "\n\t1. Envoyer un message" +
                        "\n\t2. Ajouter un contact" +
                        "\n\t3. Supprimer un contact" +
                        "\n\t4. Changer de pseudo" +
                        "\n\t0. Quitter");
                int action = sc.nextInt(); sc.nextLine();
                if (action == 0) break;
                switch (action) {
                    case 1 -> {
                        System.out.println("A qui envoyer ? (id)");
                        int to = sc.nextInt();
                        sc.nextLine();
                        System.out.println("Votre message :");
                        String msg = sc.nextLine();
                        if (msg.charAt(0) == '/'){
                            c.sendMedia(msg, to);
                        } else{
                            TextMessage textMsg = (TextMessage) MessageFactory.create(MessageType.TEXT, clientId, to);
                            textMsg.generateNewMessageId(c.messageIdGenerator);
                            textMsg.setContent(msg);
                            c.sendPacket(textMsg.toPacket());
                        }
                    }
                    case 2 -> {
                        System.out.println("Quel est l'id du contact à ajouter ?");
                        int contactId = sc.nextInt();
                        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(MessageType.ADD_CONTACT, clientId, serverId);
                        mgmtMsg.addParam("contactId", Integer.toString(contactId));
                        c.sendPacket(mgmtMsg.toPacket());
                    }
                    case 3 -> {
                        System.out.println("Quel est l'id du contact à supprimer ?");
                        int contactId = sc.nextInt();
                        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(MessageType.REMOVE_CONTACT, clientId, serverId);
                        mgmtMsg.addParam("contactId", Integer.toString(contactId));
                        c.sendPacket(mgmtMsg.toPacket());
                    }
                    case 4 -> {
                        System.out.println("Quel est votre nouveau pseudo ?");
                        String newPseudo = sc.nextLine();
                        ManagementMessage mgmtMsg = (ManagementMessage) MessageFactory.create(MessageType.UPDATE_PSEUDO, clientId, serverId);
                        mgmtMsg.addParam("newPseudo", newPseudo);
                        c.sendPacket(mgmtMsg.toPacket());
                    }
                    default -> System.out.println("Action non reconnue.");
                }
            }
            c.disconnect();
            System.exit(0);
        }
    }
}
