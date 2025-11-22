package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.ClientController;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.KeyExchangeMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.KeyExchangeResponseMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.KeyExchangeException;
import fr.uga.im2ag.m1info.chatservice.crypto.keyexchange.KeyExchangeManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side handler for KEY_EXCHANGE and KEY_EXCHANGE_RESPONSE messages.
 * <p>
 * This handler delegates to the {@link KeyExchangeManager} for processing
 * incoming key exchange messages from peers.
 * <p>
 * Processing flow:
 * <ul>
 *   <li>KEY_EXCHANGE: A peer is initiating a key exchange with us</li>
 *   <li>KEY_EXCHANGE_RESPONSE: A peer is responding to our key exchange request</li>
 * </ul>
 *
 * @see KeyExchangeManager
 * @see KeyExchangeMessage
 * @see KeyExchangeResponseMessage
 */
public class KeyExchangeHandler extends ClientPacketHandler {
    
    private static final Logger LOG = Logger.getLogger(KeyExchangeHandler.class.getName());
    
    private KeyExchangeManager keyExchangeManager;
    
    // ========================= Initialization =========================
    
    @Override
    public void initialize(ClientHandlerContext context) {
        // No-op
    }
    
    /**
     * Sets the KeyExchangeManager instance.
     * <p>
     * This must be called after the ClientController is initialized
     * and before any key exchange messages are processed.
     *
     * @param manager the KeyExchangeManager instance
     */
    public void setKeyExchangeManager(KeyExchangeManager manager) {
        this.keyExchangeManager = manager;
    }
    
    // ========================= Message Handling =========================
    
    @Override
    public void handle(ProtocolMessage message, ClientController context) {
        if (keyExchangeManager == null) {
            LOG.severe("KeyExchangeManager not initialized! Cannot process key exchange message.");
            return;
        }
        
        try {
            switch (message.getMessageType()) {
                case KEY_EXCHANGE -> handleKeyExchangeRequest(message, context);
                case KEY_EXCHANGE_RESPONSE -> handleKeyExchangeResponse(message, context);
                default -> LOG.warning("Unexpected message type in KeyExchangeHandler: " + message.getMessageType());
            }
        } catch (KeyExchangeException e) {
            LOG.log(Level.SEVERE, "Key exchange failed with peer " + message.getFrom(), e);
            // TODO: Publish an event for UI notification
        }
    }
    
    /**
     * Handles an incoming KEY_EXCHANGE request from a peer.
     */
    private void handleKeyExchangeRequest(ProtocolMessage message, ClientController context) throws KeyExchangeException {
        if (!(message instanceof KeyExchangeMessage keyExMsg)) {
            throw new IllegalArgumentException("Expected KeyExchangeMessage but got " + message.getClass().getSimpleName());
        }
        
        int peerId = keyExMsg.getFrom();
        byte[] publicKey = keyExMsg.getPublicKey();
        
        LOG.info("Received KEY_EXCHANGE from peer " + peerId);
        
        if (publicKey == null || publicKey.length == 0) {
            LOG.warning("Received KEY_EXCHANGE with empty public key from peer " + peerId);
            throw new KeyExchangeException(
                "Empty public key received",
                KeyExchangeException.ErrorCode.INVALID_PUBLIC_KEY,
                peerId
            );
        }
        
        // Delegate to KeyExchangeManager
        keyExchangeManager.handleKeyExchangeRequest(peerId, publicKey);
    }
    
    /**
     * Handles an incoming KEY_EXCHANGE_RESPONSE from a peer.
     */
    private void handleKeyExchangeResponse(ProtocolMessage message, ClientController context) throws KeyExchangeException {
        if (!(message instanceof KeyExchangeResponseMessage keyExRespMsg)) {
            throw new IllegalArgumentException("Expected KeyExchangeResponseMessage but got " + message.getClass().getSimpleName());
        }
        
        int peerId = keyExRespMsg.getFrom();
        byte[] publicKey = keyExRespMsg.getPublicKey();
        
        LOG.info("Received KEY_EXCHANGE_RESPONSE from peer " + peerId);
        
        if (publicKey == null || publicKey.length == 0) {
            LOG.warning("Received KEY_EXCHANGE_RESPONSE with empty public key from peer " + peerId);
            throw new KeyExchangeException(
                "Empty public key received",
                KeyExchangeException.ErrorCode.INVALID_PUBLIC_KEY,
                peerId
            );
        }
        
        // Delegate to KeyExchangeManager
        keyExchangeManager.handleKeyExchangeResponse(peerId, publicKey);
    }
    
    // ========================= Handler Registration =========================
    
    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.KEY_EXCHANGE || 
               messageType == MessageType.KEY_EXCHANGE_RESPONSE;
    }
}
