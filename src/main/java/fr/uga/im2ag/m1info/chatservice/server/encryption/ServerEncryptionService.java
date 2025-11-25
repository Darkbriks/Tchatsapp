package fr.uga.im2ag.m1info.chatservice.server.encryption;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ServerEncryptedMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ServerKeyExchangeMessage;
import fr.uga.im2ag.m1info.chatservice.crypto.KeyExchange;
import fr.uga.im2ag.m1info.chatservice.crypto.SymmetricCipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side encryption service for secure client-server communication.
 * <p>
 * This service manages:
 * <ul>
 *   <li>ECDH key exchange with each connecting client</li>
 *   <li>Per-connection session keys</li>
 *   <li>Encryption of server-originated messages</li>
 *   <li>Decryption of client messages destined for the server</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe.
 *
 * @see KeyExchange
 * @see SymmetricCipher
 */
public class ServerEncryptionService {

    private static final Logger LOG = Logger.getLogger(ServerEncryptionService.class.getName());

    /** Conversation ID used for deriving server session keys */
    private static final String SERVER_CONVERSATION_PREFIX = "server_session_";

    /** X25519 algorithm identifier */
    private static final String KEY_ALGORITHM = "X25519";

    /** Crypto provider */
    private static final String CRYPTO_PROVIDER = "BC";

    // ========================= Dependencies =========================

    private final KeyExchange keyExchange;
    private final SymmetricCipher cipher;

    // ========================= State =========================

    /**
     * Pending key exchanges, keyed by SocketChannel.
     * Contains the server's ephemeral keypair waiting for client response.
     */
    private final Map<SocketChannel, PendingServerKeyExchange> pendingExchanges;

    /**
     * Established session keys, keyed by SocketChannel.
     */
    private final Map<SocketChannel, SecretKey> sessionKeys;

    // ========================= Constructor =========================

    /**
     * Creates a new ServerEncryptionService.
     */
    public ServerEncryptionService() {
        this.keyExchange = new KeyExchange();
        this.cipher = new SymmetricCipher();
        this.pendingExchanges = new ConcurrentHashMap<>();
        this.sessionKeys = new ConcurrentHashMap<>();
    }

    // ========================= Key Exchange =========================

    /**
     * Initiates a key exchange with a newly connected client.
     * <p>
     * This should be called immediately when a new connection is accepted,
     * before any other communication.
     *
     * @param channel the client's socket channel
     * @return the SERVER_KEY_EXCHANGE packet to send to the client
     * @throws GeneralSecurityException if key generation fails
     */
    public Packet initiateKeyExchange(SocketChannel channel) throws GeneralSecurityException {
        // Generate ephemeral keypair for this connection
        KeyPair serverKeyPair = keyExchange.generateKeyPair();

        // Store pending exchange
        PendingServerKeyExchange pending = new PendingServerKeyExchange(serverKeyPair);
        pendingExchanges.put(channel, pending);

        // Create and return the key exchange message
        ServerKeyExchangeMessage msg = ServerKeyExchangeMessage.createServerHello(
                0, // Client ID unknown at this point
                serverKeyPair.getPublic()
        );

        LOG.fine("Initiated key exchange for channel: " + channel);
        return msg.toPacket();
    }

    /**
     * Handles the client's key exchange response.
     * <p>
     * This completes the ECDH exchange and establishes the session key.
     *
     * @param channel the client's socket channel
     * @param packet  the SERVER_KEY_EXCHANGE_RESPONSE packet from the client
     * @return true if the key exchange completed successfully
     */
    public boolean handleKeyExchangeResponse(SocketChannel channel, Packet packet) {
        PendingServerKeyExchange pending = pendingExchanges.remove(channel);
        if (pending == null) {
            LOG.warning("Received key exchange response for unknown channel: " + channel);
            return false;
        }

        try {
            // Parse client's public key
            ServerKeyExchangeMessage response = new ServerKeyExchangeMessage();
            response.fromPacket(packet);
            byte[] clientPublicKeyBytes = response.getPublicKey();

            // Reconstruct client's public key
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, CRYPTO_PROVIDER);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(clientPublicKeyBytes);
            PublicKey clientPublicKey = keyFactory.generatePublic(keySpec);

            // Compute shared secret
            byte[] sharedSecret = keyExchange.deriveSharedSecret(
                    pending.getKeyPair().getPrivate(),
                    clientPublicKey
            );

            // Derive session key using deterministic conversation ID from both public keys
            byte[] serverPublicKeyBytes = pending.getKeyPair().getPublic().getEncoded();
            String conversationId = deriveConversationId(serverPublicKeyBytes, clientPublicKeyBytes);
            byte[] sessionKeyBytes = keyExchange.deriveSessionKey(sharedSecret, conversationId);
            SecretKey sessionKey = new SecretKeySpec(sessionKeyBytes, "AES");

            // Store session key
            sessionKeys.put(channel, sessionKey);

            LOG.info("Key exchange completed for channel: " + channel);
            return true;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Key exchange failed for channel: " + channel, e);
            return false;
        }
    }

    /**
     * Checks if a key exchange is pending for a channel.
     *
     * @param channel the socket channel
     * @return true if a key exchange is in progress
     */
    public boolean isKeyExchangePending(SocketChannel channel) {
        return pendingExchanges.containsKey(channel);
    }

    /**
     * Checks if a secure session has been established for a channel.
     *
     * @param channel the socket channel
     * @return true if the session key is established
     */
    public boolean hasSecureSession(SocketChannel channel) {
        return sessionKeys.containsKey(channel);
    }

    // ========================= Encryption/Decryption =========================

    /**
     * Encrypts a packet for sending to a client.
     * <p>
     * Only encrypts packets that are:
     * <ul>
     *   <li>Created by the server (not relayed)</li>
     *   <li>Of a type that requires encryption</li>
     * </ul>
     *
     * @param channel the target client's channel
     * @param packet  the packet to encrypt
     * @return the encrypted packet, or the original packet if encryption is not needed/possible
     */
    public Packet encryptOutgoing(SocketChannel channel, Packet packet) {
        // Don't encrypt key exchange messages
        if (packet.messageType().isServerKeyExchange()) {
            return packet;
        }

        // Don't encrypt if no session established
        SecretKey sessionKey = sessionKeys.get(channel);
        if (sessionKey == null) {
            LOG.fine("No session key for channel, sending unencrypted: " + channel);
            return packet;
        }

        // Don't encrypt messages that don't require it
        if (!packet.messageType().requiresServerEncryption()) {
            return packet;
        }

        try {
            return encryptPacket(packet, sessionKey);
        } catch (GeneralSecurityException e) {
            LOG.log(Level.WARNING, "Failed to encrypt packet for channel: " + channel, e);
            return packet; // Fall back to unencrypted
        }
    }

    /**
     * Decrypts a packet received from a client.
     * <p>
     * If the packet is of type SERVER_ENCRYPTED, it is decrypted and the
     * original packet is reconstructed. Otherwise, the packet is returned as-is.
     *
     * @param channel the source client's channel
     * @param packet  the received packet
     * @return the decrypted packet, or the original packet if not encrypted
     * @throws GeneralSecurityException if decryption fails
     */
    public Packet decryptIncoming(SocketChannel channel, Packet packet) throws GeneralSecurityException {
        // Handle key exchange response specially
        if (packet.messageType() == MessageType.SERVER_KEY_EXCHANGE_RESPONSE) {
            return packet; // Will be handled by handleKeyExchangeResponse
        }

        // If not encrypted, return as-is
        if (packet.messageType() != MessageType.SERVER_ENCRYPTED) {
            return packet;
        }

        SecretKey sessionKey = sessionKeys.get(channel);
        if (sessionKey == null) {
            throw new GeneralSecurityException("No session key for channel: " + channel);
        }

        return decryptPacket(packet, sessionKey);
    }

    /**
     * Checks if an incoming packet needs decryption.
     *
     * @param packet the packet to check
     * @return true if the packet is encrypted and needs decryption
     */
    public boolean needsDecryption(Packet packet) {
        return packet.messageType() == MessageType.SERVER_ENCRYPTED;
    }

    /**
     * Checks if an outgoing packet should be encrypted.
     * <p>
     * A packet should be encrypted if:
     * <ul>
     *   <li>It's destined for a specific client (to != 0)</li>
     *   <li>It's a server-originated message (from == 0)</li>
     *   <li>It requires encryption based on message type</li>
     * </ul>
     *
     * @param packet the packet to check
     * @return true if the packet should be encrypted
     */
    public boolean shouldEncrypt(Packet packet) {
        // Only encrypt server-originated messages to clients
        if (packet.from() != 0) {
            return false; // Relayed message, don't encrypt
        }
        return packet.messageType().requiresServerEncryption();
    }

    // ========================= Connection Lifecycle =========================

    /**
     * Cleans up resources when a connection is closed.
     *
     * @param channel the closed channel
     */
    public void onConnectionClosed(SocketChannel channel) {
        pendingExchanges.remove(channel);
        sessionKeys.remove(channel);
        LOG.fine("Cleaned up encryption state for channel: " + channel);
    }

    // ========================= Private Helpers =========================

    private Packet encryptPacket(Packet packet, SecretKey sessionKey) throws GeneralSecurityException {
        // Get the payload to encrypt
        ByteBuffer payloadBuffer = packet.getPayload();
        byte[] plaintext = new byte[payloadBuffer.remaining()];
        payloadBuffer.get(plaintext);

        // Generate nonce and encrypt
        byte[] nonce = cipher.generateNonce();
        byte[] ciphertext = cipher.encrypt(plaintext, sessionKey, nonce, null);

        // Create encrypted message
        ServerEncryptedMessage encryptedMsg = new ServerEncryptedMessage(
                packet.from(),
                packet.to(),
                packet.messageType(),
                nonce,
                ciphertext
        );

        return encryptedMsg.toPacket();
    }

    private Packet decryptPacket(Packet packet, SecretKey sessionKey) throws GeneralSecurityException {
        // Parse encrypted message
        ServerEncryptedMessage encryptedMsg = new ServerEncryptedMessage();
        encryptedMsg.fromPacket(packet);

        // Decrypt
        byte[] plaintext = cipher.decrypt(
                encryptedMsg.getCiphertext(),
                sessionKey,
                encryptedMsg.getNonce(),
                null
        );

        // Reconstruct original packet
        return new Packet.PacketBuilder(plaintext.length)
                .setMessageType(encryptedMsg.getOriginalType())
                .setFrom(packet.from())
                .setTo(packet.to())
                .setPayload(plaintext)
                .build();
    }

    /**
     * Derives a deterministic conversation ID from both public keys.
     * <p>
     * This ensures that both client and server derive the same session key,
     * as long as they use the same public keys in the same order.
     *
     * @param serverPublicKey the server's public key bytes
     * @param clientPublicKey the client's public key bytes
     * @return a deterministic conversation ID
     */
    private String deriveConversationId(byte[] serverPublicKey, byte[] clientPublicKey) {
        // Use a simple hash of both keys concatenated
        // Server key first for consistency (server always initiates)
        java.security.MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 is always available
            throw new RuntimeException("SHA-256 not available", e);
        }
        digest.update(serverPublicKey);
        digest.update(clientPublicKey);
        byte[] hash = digest.digest();

        // Convert first 8 bytes to hex for a readable ID
        StringBuilder sb = new StringBuilder(SERVER_CONVERSATION_PREFIX);
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%02x", hash[i]));
        }
        return sb.toString();
    }

    // ========================= Inner Classes =========================

    /**
     * Holds state for a pending key exchange.
     */
    private static class PendingServerKeyExchange {
        private final KeyPair keyPair;
        private final long createdAt;

        PendingServerKeyExchange(KeyPair keyPair) {
            this.keyPair = keyPair;
            this.createdAt = System.currentTimeMillis();
        }

        KeyPair getKeyPair() {
            return keyPair;
        }

        long getCreatedAt() {
            return createdAt;
        }

        boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - createdAt > timeoutMs;
        }
    }
}