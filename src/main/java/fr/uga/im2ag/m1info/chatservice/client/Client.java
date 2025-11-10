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

import fr.uga.im2ag.m1info.chatservice.common.*;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
     * @return false if there is an existing connection or if the connection fails.
     */
    public boolean connect(String host, int port) {
        if (cnx!=null && cnx.isConnected()) return false;
        try {
            cnx = new Socket("localhost",1666);
            DataOutputStream dos = new DataOutputStream(cnx.getOutputStream());
            DataInputStream dis = new DataInputStream(cnx.getInputStream());
            dos.writeInt(clientId);
            dos.flush();
            // read the empty packet and use the recipient id
            clientId=Packet.readFrom(dis).to();

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
           // if (!cnx.isClosed()) e.printStackTrace();
           System.err.println("Il semblerait que l'identifiant ne permette pas de se connecter !");
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

    /** A bsic client in command line **/
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Votre id ? (0 pour en créer un nouveau)");
        int clientId =  sc.nextInt();


        Client c = new Client(clientId);
        c.setPacketProcessor(msg -> {
            if (msg.getMessageType() == MessageType.TEXT){

                TextMessage m = (TextMessage) msg;
                System.out.printf("Message reçu de %d : %s%n", m.getFrom(), m.getContent());
            } else if ( msg.getMessageType() == MessageType.MEDIA){
                MediaMessage m = (MediaMessage) msg;
                System.out.printf("Media reçu de %d : nom = %s%n", m.getFrom(), m.getMediaName());
            } else{
                System.out.println("Cote client, pas de handler ytrtouve pour" + msg.getMessageType().toString()); 
            }
        });

        if (c.connect("localhost",1666)) {

            clientId = c.getClientId();
            System.out.println("Vous êtes connecté avec l'id " + clientId);

            while (true) {
                System.out.println("A qui envoyer ? (0 pour quitter)");
                int to = sc.nextInt();sc.nextLine();
                if (to==0) break;
                System.out.println("Votre message :");
                String msg = sc.nextLine();

                if (msg.charAt(0) == '/'){
                    MediaMessage mediaMsg = (MediaMessage) MessageFactory.create(MessageType.MEDIA, clientId, to);
                    mediaMsg.generateNewMessageId(c.messageIdGenerator);
                    mediaMsg.setMediaName(msg.substring(1));
                    c.sendPacket(mediaMsg.toPacket());
                    // On est sur un media
                } else{
                    TextMessage textMsg = (TextMessage) MessageFactory.create(MessageType.TEXT, clientId, to);
                    textMsg.generateNewMessageId(c.messageIdGenerator);
                    textMsg.setContent(msg);
                    c.sendPacket(textMsg.toPacket());
                }
            }
            c.disconnect();
            System.exit(0);
        }

    }


}
