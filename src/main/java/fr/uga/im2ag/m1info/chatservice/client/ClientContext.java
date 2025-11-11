package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.common.Packet;

/**
 * Context providing access to client functionalities for packet handlers.
 * This class encapsulates the client and provides controlled access to its operations.
 */
// TODO: Add access to repositories
public class ClientContext {
    private final Client client;
    private volatile boolean connectionEstablished;
    private volatile String lastErrorMessage;

    /**
     * Creates a new ClientContext.
     *
     * @param client the client instance to wrap
     */
    public ClientContext(Client client) {
        this.client = client;
        this.connectionEstablished = false;
        this.lastErrorMessage = null;
    }

    /**
     * Get the client ID.
     *
     * @return the client ID
     */
    public int getClientId() {
        return client.getClientId();
    }

    /**
     * Send a packet to the server.
     *
     * @param packet the packet to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendPacket(Packet packet) {
        return client.sendPacket(packet);
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        client.disconnect();
    }

    /**
     * Check if the client is connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Mark the connection as established.
     */
    public void markConnectionEstablished() {
        this.connectionEstablished = true;
    }

    /**
     * Check if the connection handshake is complete.
     *
     * @return true if connection is fully established, false otherwise
     */
    public boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    /**
     * Set the last error message.
     *
     * @param errorMessage the error message
     */
    public void setLastError(String errorMessage) {
        this.lastErrorMessage = errorMessage;
    }

    /**
     * Get the last error message.
     *
     * @return the last error message, or null if none
     */
    public String getLastError() {
        return lastErrorMessage;
    }

    /**
     * Clear the last error message.
     */
    public void clearLastError() {
        this.lastErrorMessage = null;
    }

    /**
     * Update the client ID.
     *
     * @param clientId the new client ID
     */
    public void updateClientId(int clientId) {
        client.updateClientId(clientId);
    }
}