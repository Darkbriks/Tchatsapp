package fr.uga.im2ag.m1info.chatservice.client.encryption;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ServerEncryptedMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ServerKeyExchangeMessage;
import fr.uga.im2ag.m1info.chatservice.crypto.KeyExchange;
import fr.uga.im2ag.m1info.chatservice.crypto.SymmetricCipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side encryption service for secure client-server communication.
 * <p>
 * This service handles:
 * <ul>
 *   <li>ECDH key exchange with the server</li>
 *   <li>Session key management for server communication</li>
 *   <li>Encryption of messages destined for the server (to=0)</li>
 *   <li>Decryption of messages from the server (from=0)</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe.
 */
public class ClientServerEncryptionService {

    private static final Logger LOG = Logger.getLogger(ClientServerEncryptionService.class.getName());

    /** Default timeout for key exchange in seconds */
    public static final long DEFAULT_KEY_EXCHANGE_TIMEOUT_SECONDS = 5;

    /** Conversation ID prefix for server sessions */
    private static final String SERVER_CONVERSATION_PREFIX = "server_session_";

    /** X25519 algorithm identifier */
    private static final String KEY_ALGORITHM = "X25519";

    /** Crypto provider */
    private static final String CRYPTO_PROVIDER = "BC";

    // ========================= Dependencies =========================

    private final KeyExchange keyExchange;
    private final SymmetricCipher cipher;

    // ========================= State =========================

    /** Client's ephemeral keypair for server communication */
    private volatile KeyPair clientKeyPair;

    /** Session key for server communication */
    private volatile SecretKey serverSessionKey;

    /** Future that completes when session is established */
    private final CompletableFuture<Boolean> sessionEstablished;

    /** Unique identifier for this client's server session */
    private volatile String sessionId;

    // ========================= Constructor =========================

    /**
     * Creates a new ClientServerEncryptionService.
     */
    public ClientServerEncryptionService() {
        this.keyExchange = new KeyExchange();
        this.cipher = new SymmetricCipher();
        this.sessionEstablished = new CompletableFuture<>();
    }

    // ========================= Key Exchange =========================

    /**
     * Handles the SERVER_KEY_EXCHANGE message from the server.
     * <p>
     * This method:
     * <ol>
     *   <li>Generates the client's ephemeral keypair</li>
     *   <li>Computes the shared secret</li>
     *   <li>Derives the session key</li>
     *   <li>Creates the response packet</li>
     * </ol>
     *
     * @param serverKeyExchangePacket the SERVER_KEY_EXCHANGE packet from the server
     * @return the SERVER_KEY_EXCHANGE_RESPONSE packet to send back
     * @throws GeneralSecurityException if key exchange fails
     */
    public Packet handleServerKeyExchange(Packet serverKeyExchangePacket) throws GeneralSecurityException {
        if (serverKeyExchangePacket.messageType() != MessageType.SERVER_KEY_EXCHANGE) {
            throw new IllegalArgumentException("Expected SERVER_KEY_EXCHANGE, got: " +
                    serverKeyExchangePacket.messageType());
        }

        // Parse server's public key
        ServerKeyExchangeMessage serverMsg = new ServerKeyExchangeMessage();
        serverMsg.fromPacket(serverKeyExchangePacket);
        byte[] serverPublicKeyBytes = serverMsg.getPublicKey();

        // Reconstruct server's public key
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, CRYPTO_PROVIDER);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(serverPublicKeyBytes);
        PublicKey serverPublicKey = keyFactory.generatePublic(keySpec);

        // Generate client's ephemeral keypair
        this.clientKeyPair = keyExchange.generateKeyPair();

        // Compute shared secret
        byte[] sharedSecret = keyExchange.deriveSharedSecret(
                clientKeyPair.getPrivate(),
                serverPublicKey
        );

        // Derive session key using a deterministic conversation ID
        // Both client and server must use the same ID, so we derive it from the public keys
        this.sessionId = deriveConversationId(serverPublicKeyBytes, clientKeyPair.getPublic().getEncoded());
        byte[] sessionKeyBytes = keyExchange.deriveSessionKey(sharedSecret, sessionId);
        this.serverSessionKey = new SecretKeySpec(sessionKeyBytes, "AES");

        // Mark session as established
        sessionEstablished.complete(true);

        LOG.info("Server key exchange completed, session established");

        // Create response
        ServerKeyExchangeMessage response = ServerKeyExchangeMessage.createClientResponse(
                0, // Client ID not yet known
                clientKeyPair.getPublic()
        );

        return response.toPacket();
    }

    /**
     * Waits for the secure session to be established.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit
     * @return true if session was established, false if timeout occurred
     * @throws InterruptedException if the wait is interrupted
     */
    public boolean waitForSession(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            return sessionEstablished.get(timeout, unit);
        } catch (TimeoutException e) {
            LOG.warning("Timeout waiting for server session");
            return false;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error waiting for server session", e);
            return false;
        }
    }

    /**
     * Checks if the secure session has been established.
     *
     * @return true if the session key is available
     */
    public boolean isSessionEstablished() {
        return serverSessionKey != null;
    }

    // ========================= Encryption/Decryption =========================

    /**
     * Encrypts a packet destined for the server.
     * <p>
     * Only encrypts if:
     * <ul>
     *   <li>The destination is the server (to=0)</li>
     *   <li>The message type requires encryption</li>
     *   <li>The session is established</li>
     * </ul>
     *
     * @param packet the packet to encrypt
     * @return the encrypted packet, or the original if encryption is not needed/possible
     */
    public Packet encryptForServer(Packet packet) {
        // Only encrypt messages to the server
        if (packet.to() != 0) {
            return packet;
        }

        // Don't encrypt key exchange messages
        if (packet.messageType().isServerKeyExchange()) {
            return packet;
        }

        // Don't encrypt if session not established
        if (serverSessionKey == null) {
            LOG.fine("Session not established, sending unencrypted");
            return packet;
        }

        // Don't encrypt messages that don't require it
        if (!packet.messageType().requiresServerEncryption()) {
            return packet;
        }

        try {
            return encryptPacket(packet);
        } catch (GeneralSecurityException e) {
            LOG.log(Level.WARNING, "Failed to encrypt packet for server", e);
            return packet;
        }
    }

    /**
     * Decrypts a packet received from the server.
     * <p>
     * If the packet is SERVER_ENCRYPTED, it is decrypted and the original
     * packet is reconstructed. Otherwise, the packet is returned as-is.
     *
     * @param packet the received packet
     * @return the decrypted packet, or the original if not encrypted
     * @throws GeneralSecurityException if decryption fails
     */
    public Packet decryptFromServer(Packet packet) throws GeneralSecurityException {
        // If not encrypted, return as-is
        if (packet.messageType() != MessageType.SERVER_ENCRYPTED) {
            return packet;
        }

        if (serverSessionKey == null) {
            throw new GeneralSecurityException("No server session key available");
        }

        return decryptPacket(packet);
    }

    /**
     * Checks if a packet from the server needs decryption.
     *
     * @param packet the packet to check
     * @return true if the packet is encrypted
     */
    public boolean needsDecryption(Packet packet) {
        return packet.messageType() == MessageType.SERVER_ENCRYPTED;
    }

    /**
     * Checks if a packet should be encrypted before sending to the server.
     *
     * @param packet the packet to check
     * @return true if the packet should be encrypted
     */
    public boolean shouldEncryptForServer(Packet packet) {
        return packet.to() == 0 &&
                !packet.messageType().isServerKeyExchange() &&
                packet.messageType().requiresServerEncryption() &&
                serverSessionKey != null;
    }

    // ========================= Lifecycle =========================

    /**
     * Resets the encryption state.
     * <p>
     * Call this when reconnecting to the server.
     */
    public void reset() {
        this.clientKeyPair = null;
        this.serverSessionKey = null;
        this.sessionId = null;
        LOG.info("Client server encryption service reset");
    }

    // ========================= Private Helpers =========================

    private Packet encryptPacket(Packet packet) throws GeneralSecurityException {
        // Get the payload to encrypt
        ByteBuffer payloadBuffer = packet.getPayload();
        byte[] plaintext = new byte[payloadBuffer.remaining()];
        payloadBuffer.get(plaintext);

        // Generate nonce and encrypt
        byte[] nonce = cipher.generateNonce();
        byte[] ciphertext = cipher.encrypt(plaintext, serverSessionKey, nonce, null);

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

    private Packet decryptPacket(Packet packet) throws GeneralSecurityException {
        // Parse encrypted message
        ServerEncryptedMessage encryptedMsg = new ServerEncryptedMessage();
        encryptedMsg.fromPacket(packet);

        // Decrypt
        byte[] plaintext = cipher.decrypt(
                encryptedMsg.getCiphertext(),
                serverSessionKey,
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
            throw new RuntimeException("SHA-256 not available", e);
        }
        digest.update(serverPublicKey);
        digest.update(clientPublicKey);
        byte[] hash = digest.digest();

        StringBuilder sb = new StringBuilder(SERVER_CONVERSATION_PREFIX);
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%02x", hash[i]));
        }
        return sb.toString();
    }
}