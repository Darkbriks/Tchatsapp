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

import fr.uga.im2ag.m1info.chatservice.client.handlers.*;
import fr.uga.im2ag.m1info.chatservice.common.*;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * A basic client for Tchatsapp.
 * It allows to connect to a server, send and receive packets.
 */
public class Client {

    private static final int MAX_SIZE_CHUNK_FILE = 8192;
    private int clientId;
    private Socket cnx;
    private PacketProcessor processor;
    private Thread receptionThread;

    /**
     * Creates a new Client with clientId 0 (for user creation).
     */
    public Client() {
        this(0);
    }

    /**
     * Creates a new Client with the specified clientId.
     *
     * @param clientId the client ID (0 for new user creation)
     */
    public Client(int clientId) {
        this.clientId = clientId;
    }

    /**
     * Attempts to connect to the server.
     *
     * @param host the server hostname or IP address
     * @param port the server port
     * @param username the username to use for the connection (only used if clientId is 0)
     * @return true if the connection socket was established, false otherwise
     * @throws IOException if a network error occurs
     */
    public boolean connect(String host, int port, String username) throws IOException {
        if (cnx != null && cnx.isConnected()) {
            return false;
        }

        cnx = new Socket(host, port);
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

        startReceptionThread(dis);

        return cnx.isConnected();
    }

    /**
     * Start the reception thread for handling incoming packets.
     *
     * @param dis the DataInputStream to read from
     */
    private void startReceptionThread(DataInputStream dis) {
        receptionThread = new Thread(() -> {
            try {
                while (cnx != null && !cnx.isInputShutdown()) {
                    Packet packet = Packet.readFrom(dis);
                    processReceivedPacket(packet);
                }
            } catch (IOException e) {
                if (cnx == null || !cnx.isConnected()) {
                    return;
                }
                System.err.println("[Client] Reception thread error: " + e.getMessage());
            }
        }, "Client-Reception-Thread");
        receptionThread.setDaemon(true);
        receptionThread.start();
    }

    /**
     * Get the client ID.
     *
     * @return the client ID
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Update the client ID (used after successful user creation).
     *
     * @param clientId the new client ID
     */
    void updateClientId(int clientId) {
        this.clientId = clientId;
    }

    /**
     * Set the packet processor to be called when packets are received by the client.
     *
     * @param p the PacketProcessor to use
     */
    public void setPacketProcessor(PacketProcessor p) {
        processor = p;
    }

    /**
     * Process a received packet by delegating to the registered processor.
     *
     * @param m the received packet
     */
    private void processReceivedPacket(Packet m) {
        if (processor != null) { processor.process(MessageFactory.fromPacket(m)); }
    }

    /**
     * Check if the client is connected to the server.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return cnx != null && cnx.isConnected();
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        try {
            if (cnx != null) { cnx.close(); }
            cnx = null;
            if (receptionThread != null && receptionThread.isAlive()) {
                receptionThread.interrupt();
            }
        } catch (IOException e) {
            /* ignored */
        }
    }

    /**
     * Send a packet to the server.
     *
     * @param m the packet to send
     * @return true if the packet was sent successfully, false otherwise
     */
    public boolean sendPacket(Packet m) {
        System.out.println("[Client] Sending: " + m);
        try {
            DataOutputStream dos = new DataOutputStream(cnx.getOutputStream());
            m.writeTo(dos);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Failed to send packet: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a media file to a recipient.
     * The file is split into chunks and sent as multiple MediaMessage packets.
     *
     * @param msg the filename path (should start with '/')
     * @param to the recipient ID
     */
    public void sendMedia(String msg, int to) {
        try {
            String fileName = msg.substring(1);
            InputStream fileStream = new FileInputStream(new File(fileName));
            int count = 0;
            byte[] buffer = new byte[MAX_SIZE_CHUNK_FILE];
            while ((count = fileStream.read(buffer)) > 0) {
                MediaMessage mediaMsg = (MediaMessage) MessageFactory.create(MessageType.MEDIA, clientId, to);
                mediaMsg.setMediaName(fileName);
                mediaMsg.setContent(buffer);
                mediaMsg.setSizeContent(count);
                sendPacket(mediaMsg.toPacket());
            }
            fileStream.close();
        } catch (Exception e) {
            System.err.println("[Client] Failed to send media: " + e.getMessage());
        }
    }
}
