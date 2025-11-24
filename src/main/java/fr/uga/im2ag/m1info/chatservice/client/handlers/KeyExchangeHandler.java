package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.client.encryption.ClientEncryptionService;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.KeyExchangeMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.KeyExchangeResponseMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side handler for KEY_EXCHANGE and KEY_EXCHANGE_RESPONSE messages.
 * <p>
 * This handler delegates to the {@link ClientEncryptionService} for processing
 * incoming key exchange messages from peers.
 * <p>
 * Processing flow:
 * <ul>
 *   <li>KEY_EXCHANGE: A peer is initiating a key exchange with us</li>
 *   <li>KEY_EXCHANGE_RESPONSE: A peer is responding to our key exchange request</li>
 * </ul>
 * <p>
 * This version uses the ClientEncryptionService facade for cleaner integration.
 *
 * @see ClientEncryptionService
 * @see KeyExchangeMessage
 * @see KeyExchangeResponseMessage
 */
public class KeyExchangeHandler extends ClientPacketHandler {

    private static final Logger LOG = Logger.getLogger(KeyExchangeHandler.class.getName());

    private ClientEncryptionService encryptionService;

    // ========================= Initialization =========================

    @Override
    public void initialize(ClientHandlerContext context) {
        // No-op - encryption service is set separately
    }

    /**
     * Sets the ClientEncryptionService instance.
     * <p>
     * This must be called after the ClientController is initialized
     * and before any key exchange messages are processed.
     *
     * @param service the ClientEncryptionService instance
     */
    public void setEncryptionService(ClientEncryptionService service) {
        this.encryptionService = service;
    }

    // ========================= Handler Configuration =========================

    @Override
    public boolean canHandle(MessageType type) {
        return type == MessageType.KEY_EXCHANGE || type == MessageType.KEY_EXCHANGE_RESPONSE;
    }

    // ========================= Message Handling =========================

    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (encryptionService == null) {
            LOG.severe("ClientEncryptionService not initialized! Cannot process key exchange message.");
            return;
        }

        switch (message.getMessageType()) {
            case KEY_EXCHANGE -> handleKeyExchangeRequest(message);
            case KEY_EXCHANGE_RESPONSE -> handleKeyExchangeResponse(message);
            default -> LOG.warning("Unexpected message type in EncryptionKeyExchangeHandler: " +
                    message.getMessageType());
        }
    }

    /**
     * Handles an incoming KEY_EXCHANGE request from a peer.
     * <p>
     * The peer is initiating a key exchange. We need to:
     * <ol>
     *   <li>Extract the peer's public key</li>
     *   <li>Generate our keypair and compute shared secret</li>
     *   <li>Send our public key back as KEY_EXCHANGE_RESPONSE</li>
     * </ol>
     *
     * @param message the KEY_EXCHANGE message
     */
    private void handleKeyExchangeRequest(ProtocolMessage message) {
        if (!(message instanceof KeyExchangeMessage keyExMsg)) {
            LOG.severe("Expected KeyExchangeMessage but got: " + message.getClass().getName());
            return;
        }

        int peerId = keyExMsg.getFrom();
        byte[] publicKeyBytes = keyExMsg.getPublicKey();

        LOG.info("Received KEY_EXCHANGE request from peer " + peerId);

        try {
            encryptionService.handleKeyExchangeRequest(peerId, publicKeyBytes);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process key exchange request from peer " + peerId, e);
            // Could publish an error event here for UI notification
        }
    }

    /**
     * Handles an incoming KEY_EXCHANGE_RESPONSE from a peer.
     * <p>
     * The peer is responding to our key exchange request. We need to:
     * <ol>
     *   <li>Extract the peer's public key</li>
     *   <li>Compute the shared secret using our stored private key</li>
     *   <li>Derive and store the session key</li>
     * </ol>
     *
     * @param message the KEY_EXCHANGE_RESPONSE message
     */
    private void handleKeyExchangeResponse(ProtocolMessage message) {
        if (!(message instanceof KeyExchangeResponseMessage keyExRespMsg)) {
            LOG.severe("Expected KeyExchangeResponseMessage but got: " + message.getClass().getName());
            return;
        }

        int peerId = keyExRespMsg.getFrom();
        byte[] publicKeyBytes = keyExRespMsg.getPublicKey();

        LOG.info("Received KEY_EXCHANGE_RESPONSE from peer " + peerId);

        try {
            encryptionService.handleKeyExchangeResponse(peerId, publicKeyBytes);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process key exchange response from peer " + peerId, e);
            // Could publish an error event here for UI notification
        }
    }
}
