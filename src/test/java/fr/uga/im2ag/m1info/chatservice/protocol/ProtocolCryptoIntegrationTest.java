package fr.uga.im2ag.m1info.chatservice.protocol;
 
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.crypto.KeyExchange;
import fr.uga.im2ag.m1info.chatservice.crypto.SymmetricCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
 
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Integration tests combining protocol layer with crypto layer.
 * Tests the complete flow: encrypt → serialize → packet → deserialize → decrypt
 */
class ProtocolCryptoIntegrationTest {
 
    private KeyExchange keyExchange;
    private SymmetricCipher cipher;
 
    @BeforeEach
    void setUp() {
        keyExchange = new KeyExchange();
        cipher = new SymmetricCipher();
    }
 
    @Test
    @DisplayName("Complete message flow: Alice encrypts, packets, Bob decrypts")
    void testCompleteMessageFlow() throws Exception {
        // === Setup: Key Exchange ===
        KeyPair aliceKeys = keyExchange.generateKeyPair();
        KeyPair bobKeys = keyExchange.generateKeyPair();
 
        String conversationId = ProtocolUtils.createConversationId(1, 2);
 
        byte[] aliceShared = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), bobKeys.getPublic());
        byte[] bobShared = keyExchange.deriveSharedSecret(bobKeys.getPrivate(), aliceKeys.getPublic());
 
        byte[] sessionKey = keyExchange.deriveSessionKey(aliceShared, conversationId);
        SecretKeySpec key = new SecretKeySpec(sessionKey, "AES");
 
        // === Alice sends a message ===
        String plaintext = "Hello Bob! This is a secure message.";
        long sequence = 1;
        byte[] nonce = cipher.generateNonce();
 
        // 1. Encrypt the plaintext
        byte[] ciphertext = cipher.encrypt(
                plaintext.getBytes(StandardCharsets.UTF_8),
                key,
                nonce,
                null
        );
 
        // 2. Create EncryptedMessage
        EncryptedMessage encryptedMessage = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                sequence,
                nonce,
                ciphertext
        );
 
        // 3. Wrap in Packet
        Packet packet = ProtocolUtils.createPacketFromEncryptedMessage(1, 2, encryptedMessage);
 
        // === Server relays packet (cannot read it) ===
        // Server sees only: from=1, to=2, payload=gibberish
 
        // === Bob receives the packet ===
        // 1. Extract EncryptedMessage from Packet
        EncryptedMessage receivedMessage = ProtocolUtils.extractEncryptedMessageFromPacket(packet);
 
        // 2. Verify message type and sequence
        assertEquals(MessageType.ENCRYPTED_TEXT, receivedMessage.getType());
        assertEquals(sequence, receivedMessage.getSequenceNumber());
 
        // 3. Decrypt with Bob's session key
        SecretKeySpec bobKey = new SecretKeySpec(
                keyExchange.deriveSessionKey(bobShared, conversationId),
                "AES"
        );
 
        byte[] decrypted = cipher.decrypt(
                receivedMessage.getCiphertext(),
                bobKey,
                receivedMessage.getNonce(),
                null
        );
 
        String receivedPlaintext = new String(decrypted, StandardCharsets.UTF_8);
 
        // === Verify ===
        assertEquals(plaintext, receivedPlaintext);
    }
 
    @Test
    @DisplayName("Multiple messages with increasing sequence numbers")
    void testMultipleMessagesWithSequence() throws Exception {
        // Setup
        KeyPair aliceKeys = keyExchange.generateKeyPair();
        KeyPair bobKeys = keyExchange.generateKeyPair();
 
        String conversationId = ProtocolUtils.createConversationId(1, 2);
        byte[] sharedSecret = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), bobKeys.getPublic());
        byte[] sessionKey = keyExchange.deriveSessionKey(sharedSecret, conversationId);
        SecretKeySpec key = new SecretKeySpec(sessionKey, "AES");
 
        // Send 5 messages
        for (long seq = 1; seq <= 5; seq++) {
            String plaintext = "Message number " + seq;
            byte[] nonce = cipher.generateNonce();
 
            // Encrypt
            byte[] ciphertext = cipher.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key, nonce, null);
 
            // Create encrypted message
            EncryptedMessage encMsg = new EncryptedMessage(MessageType.ENCRYPTED_TEXT, seq, nonce, ciphertext);
 
            // Packet it
            Packet packet = ProtocolUtils.createPacketFromEncryptedMessage(1, 2, encMsg);
 
            // Receive and decrypt
            EncryptedMessage received = ProtocolUtils.extractEncryptedMessageFromPacket(packet);
            assertEquals(seq, received.getSequenceNumber());
 
            byte[] decrypted = cipher.decrypt(received.getCiphertext(), key, received.getNonce(), null);
            String decryptedText = new String(decrypted, StandardCharsets.UTF_8);
 
            assertEquals(plaintext, decryptedText);
        }
    }
 
    @Test
    @DisplayName("File chunk transfer simulation")
    void testFileChunkTransfer() throws Exception {
        // Setup
        KeyPair aliceKeys = keyExchange.generateKeyPair();
        KeyPair bobKeys = keyExchange.generateKeyPair();
 
        String conversationId = ProtocolUtils.createConversationId(1, 2);
        byte[] sharedSecret = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), bobKeys.getPublic());
        byte[] sessionKey = keyExchange.deriveSessionKey(sharedSecret, conversationId);
        SecretKeySpec key = new SecretKeySpec(sessionKey, "AES");
 
        // Simulate file chunks
        byte[] fileChunk = new byte[1024]; // 1KB chunk
        java.util.Arrays.fill(fileChunk, (byte) 0xAB);
 
        long sequence = 10;
        byte[] nonce = cipher.generateNonce();
 
        // Encrypt chunk
        byte[] encryptedChunk = cipher.encrypt(fileChunk, key, nonce, null);
 
        // Create message
        EncryptedMessage encMsg = new EncryptedMessage(
                MessageType.ENCRYPTED_FILE_CHUNK,
                sequence,
                nonce,
                encryptedChunk
        );
 
        // Packet
        Packet packet = ProtocolUtils.createPacketFromEncryptedMessage(1, 2, encMsg);
 
        // Receive
        EncryptedMessage received = ProtocolUtils.extractEncryptedMessageFromPacket(packet);
        assertEquals(MessageType.ENCRYPTED_FILE_CHUNK, received.getType());
 
        // Decrypt
        byte[] decryptedChunk = cipher.decrypt(received.getCiphertext(), key, received.getNonce(), null);
 
        assertArrayEquals(fileChunk, decryptedChunk);
    }
 
    @Test
    @DisplayName("Different conversations have isolated protocols")
    void testConversationIsolation() throws Exception {
        KeyPair aliceKeys = keyExchange.generateKeyPair();
        KeyPair bobKeys = keyExchange.generateKeyPair();
        KeyPair charlieKeys = keyExchange.generateKeyPair();
 
        // Alice-Bob conversation
        String convAliceBob = ProtocolUtils.createConversationId(1, 2);
        byte[] sharedAliceBob = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), bobKeys.getPublic());
        byte[] keyAliceBob = keyExchange.deriveSessionKey(sharedAliceBob, convAliceBob);
 
        // Alice-Charlie conversation
        String convAliceCharlie = ProtocolUtils.createConversationId(1, 3);
        byte[] sharedAliceCharlie = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), charlieKeys.getPublic());
        byte[] keyAliceCharlie = keyExchange.deriveSessionKey(sharedAliceCharlie, convAliceCharlie);
 
        // Keys should be different
        assertFalse(java.util.Arrays.equals(keyAliceBob, keyAliceCharlie));
 
        // Message for Bob
        String messageToBob = "Hi Bob!";
        byte[] nonce = cipher.generateNonce();
        byte[] cipherForBob = cipher.encrypt(
                messageToBob.getBytes(StandardCharsets.UTF_8),
                new SecretKeySpec(keyAliceBob, "AES"),
                nonce,
                null
        );
 
        // Charlie cannot decrypt Bob's message
        assertThrows(Exception.class, () -> {
            cipher.decrypt(
                    cipherForBob,
                    new SecretKeySpec(keyAliceCharlie, "AES"),
                    nonce,
                    null
            );
        });
    }
 
    @Test
    @DisplayName("Server cannot decrypt messages (E2EE verification)")
    void testServerCannotDecrypt() throws Exception {
        // Setup Alice and Bob
        KeyPair aliceKeys = keyExchange.generateKeyPair();
        KeyPair bobKeys = keyExchange.generateKeyPair();
 
        String conversationId = ProtocolUtils.createConversationId(1, 2);
        byte[] sharedSecret = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), bobKeys.getPublic());
        byte[] sessionKey = keyExchange.deriveSessionKey(sharedSecret, conversationId);
        SecretKeySpec key = new SecretKeySpec(sessionKey, "AES");
 
        // Alice encrypts
        String plaintext = "Secret message";
        byte[] nonce = cipher.generateNonce();
        byte[] ciphertext = cipher.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key, nonce, null);
 
        EncryptedMessage encMsg = new EncryptedMessage(MessageType.ENCRYPTED_TEXT, 1, nonce, ciphertext);
        Packet packet = ProtocolUtils.createPacketFromEncryptedMessage(1, 2, encMsg);
 
        // Server sees the packet but cannot decrypt
        // Server only has: public keys (useless), and encrypted packet
 
        // Simulate server trying to decrypt with wrong key
        byte[] wrongKey = new byte[32];
        SecretKeySpec serverKey = new SecretKeySpec(wrongKey, "AES");
 
        EncryptedMessage serverReceived = ProtocolUtils.extractEncryptedMessageFromPacket(packet);
 
        assertThrows(Exception.class, () -> {
            cipher.decrypt(serverReceived.getCiphertext(), serverKey, serverReceived.getNonce(), null);
        }, "Server with wrong key cannot decrypt");
    }
 
    @Test
    @DisplayName("Message tampering is detected at protocol level")
    void testTamperingDetection() throws Exception {
        // Setup
        KeyPair aliceKeys = keyExchange.generateKeyPair();
        KeyPair bobKeys = keyExchange.generateKeyPair();
 
        String conversationId = ProtocolUtils.createConversationId(1, 2);
        byte[] sharedSecret = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), bobKeys.getPublic());
        byte[] sessionKey = keyExchange.deriveSessionKey(sharedSecret, conversationId);
        SecretKeySpec key = new SecretKeySpec(sessionKey, "AES");
 
        // Alice encrypts
        String plaintext = "Transfer $10";
        byte[] nonce = cipher.generateNonce();
        byte[] ciphertext = cipher.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key, nonce, null);
 
        // Create message
        EncryptedMessage encMsg = new EncryptedMessage(MessageType.ENCRYPTED_TEXT, 1, nonce, ciphertext);
 
        // Attacker tampers with ciphertext
        byte[] tamperedCiphertext = ciphertext.clone();
        tamperedCiphertext[0] ^= 0x01; // Flip a bit
 
        EncryptedMessage tamperedMsg = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                1,
                nonce,
                tamperedCiphertext
        );
 
        // Bob tries to decrypt
        assertThrows(Exception.class, () -> {
            cipher.decrypt(tamperedMsg.getCiphertext(), key, tamperedMsg.getNonce(), null);
        }, "Tampering should be detected by GCM");
    }
 
    @Test
    @DisplayName("Large message through full protocol stack")
    void testLargeMessageFullStack() throws Exception {
        // Setup
        KeyPair aliceKeys = keyExchange.generateKeyPair();
        KeyPair bobKeys = keyExchange.generateKeyPair();
 
        String conversationId = ProtocolUtils.createConversationId(1, 2);
        byte[] sharedSecret = keyExchange.deriveSharedSecret(aliceKeys.getPrivate(), bobKeys.getPublic());
        byte[] sessionKey = keyExchange.deriveSessionKey(sharedSecret, conversationId);
        SecretKeySpec key = new SecretKeySpec(sessionKey, "AES");
 
        // Large message (64KB)
        byte[] largeData = new byte[64 * 1024];
        java.util.Arrays.fill(largeData, (byte) 0xFF);
 
        byte[] nonce = cipher.generateNonce();
        byte[] ciphertext = cipher.encrypt(largeData, key, nonce, null);
 
        EncryptedMessage encMsg = new EncryptedMessage(MessageType.ENCRYPTED_FILE_CHUNK, 1, nonce, ciphertext);
 
        // Serialize
        byte[] serialized = encMsg.serialize();
        assertTrue(serialized.length > 64 * 1024, "Serialized should include overhead");
 
        // Deserialize
        EncryptedMessage deserialized = EncryptedMessage.deserialize(serialized);
 
        // Decrypt
        byte[] decrypted = cipher.decrypt(deserialized.getCiphertext(), key, deserialized.getNonce(), null);
 
        assertArrayEquals(largeData, decrypted);
    }
 
    @Test
    @DisplayName("Protocol validation catches invalid messages")
    void testProtocolValidation() {
        // Invalid sequence (negative)
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateSequenceNumber(-1);
        });
 
        // Invalid nonce (wrong size)
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateNonce(new byte[8]);
        });
 
        // Invalid ciphertext (too small)
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateCiphertext(new byte[10]);
        });
    }
}