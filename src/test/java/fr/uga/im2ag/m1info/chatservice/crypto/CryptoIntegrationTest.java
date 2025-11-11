package fr.uga.im2ag.m1info.chatservice.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete E2EE flow.
 * Tests the interaction between KeyExchange and SymmetricCipher.
 */
class CryptoIntegrationTest {

    private KeyExchange keyExchange;
    private SymmetricCipher cipher;

    @BeforeEach
    void setUp() {
        keyExchange = new KeyExchange();
        cipher = new SymmetricCipher();
    }

    @Test
    @DisplayName("Complete E2EE flow: Alice and Bob exchange messages")
    void testCompleteE2EEFlow() throws Exception {
        // === PHASE 1: Key Exchange ===

        // Alice generates her keypair
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();

        // Bob generates his keypair
        KeyPair bobKeyPair = keyExchange.generateKeyPair();

        // Alice and Bob exchange public keys (simulated - in real app, sent via server)
        // Alice computes shared secret
        byte[] aliceSharedSecret = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );

        // Bob computes shared secret
        byte[] bobSharedSecret = keyExchange.deriveSharedSecret(
                bobKeyPair.getPrivate(),
                aliceKeyPair.getPublic()
        );

        // Both derive session key with conversation ID
        String conversationId = "alice-bob-chat-123";
        byte[] aliceSessionKey = keyExchange.deriveSessionKey(aliceSharedSecret, conversationId);
        byte[] bobSessionKey = keyExchange.deriveSessionKey(bobSharedSecret, conversationId);

        // Verify both have the same session key
        assertArrayEquals(aliceSessionKey, bobSessionKey,
                "Alice and Bob should have the same session key");

        // === PHASE 2: Encrypted Communication ===

        // Convert to SecretKey for AES
        SecretKeySpec aliceKey = new SecretKeySpec(aliceSessionKey, "AES");
        SecretKeySpec bobKey = new SecretKeySpec(bobSessionKey, "AES");

        // Alice sends a message to Bob
        String aliceMessage = "Hello Bob! This is a secret message.";
        byte[] aliceNonce = cipher.generateNonce();
        byte[] encryptedMessage = cipher.encrypt(
                aliceMessage.getBytes(StandardCharsets.UTF_8),
                aliceKey,
                aliceNonce,
                null
        );

        // Server forwards encrypted message to Bob (cannot read it)

        // Bob decrypts the message
        byte[] decryptedMessage = cipher.decrypt(encryptedMessage, bobKey, aliceNonce, null);
        String bobReceivedMessage = new String(decryptedMessage, StandardCharsets.UTF_8);

        assertEquals(aliceMessage, bobReceivedMessage,
                "Bob should receive Alice's original message");

        // === PHASE 3: Bob replies ===

        String bobMessage = "Hi Alice! I received your message securely.";
        byte[] bobNonce = cipher.generateNonce();
        byte[] bobEncryptedMessage = cipher.encrypt(
                bobMessage.getBytes(StandardCharsets.UTF_8),
                bobKey,
                bobNonce,
                null
        );

        // Alice decrypts Bob's reply
        byte[] aliceDecryptedReply = cipher.decrypt(bobEncryptedMessage, aliceKey, bobNonce, null);
        String aliceReceivedReply = new String(aliceDecryptedReply, StandardCharsets.UTF_8);

        assertEquals(bobMessage, aliceReceivedReply,
                "Alice should receive Bob's reply message");
    }

    @Test
    @DisplayName("Server cannot decrypt messages (E2EE verification)")
    void testServerCannotDecrypt() throws Exception {
        // Alice and Bob perform key exchange
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();
        KeyPair bobKeyPair = keyExchange.generateKeyPair();

        byte[] aliceSharedSecret = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );

        byte[] aliceSessionKey = keyExchange.deriveSessionKey(aliceSharedSecret, "conversation");
        SecretKeySpec aliceKey = new SecretKeySpec(aliceSessionKey, "AES");

        // Alice encrypts a message
        String message = "Secret information";
        byte[] nonce = cipher.generateNonce();
        byte[] encryptedMessage = cipher.encrypt(
                message.getBytes(StandardCharsets.UTF_8),
                aliceKey,
                nonce,
                null
        );

        // Server sees the encrypted message but has no way to decrypt it
        // Server only sees Alice's and Bob's public keys (useless without private keys)

        // Server tries to decrypt with a random key (simulating attack)
        byte[] randomKey = new byte[32];
        SecretKeySpec serverKey = new SecretKeySpec(randomKey, "AES");

        assertThrows(Exception.class, () -> {
            cipher.decrypt(encryptedMessage, serverKey, nonce, null);
        }, "Server with wrong key should not be able to decrypt");
    }

    @Test
    @DisplayName("Multiple conversations should have isolated keys")
    void testMultipleConversationsIsolation() throws Exception {
        // Alice's keypair
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();

        // Bob's keypair
        KeyPair bobKeyPair = keyExchange.generateKeyPair();

        // Charlie's keypair
        KeyPair charlieKeyPair = keyExchange.generateKeyPair();

        // Alice-Bob conversation
        byte[] aliceBobShared = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );
        byte[] aliceBobKey = keyExchange.deriveSessionKey(aliceBobShared, "alice-bob");

        // Alice-Charlie conversation
        byte[] aliceCharlieShared = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                charlieKeyPair.getPublic()
        );
        byte[] aliceCharlieKey = keyExchange.deriveSessionKey(aliceCharlieShared, "alice-charlie");

        // Keys should be different
        assertFalse(java.util.Arrays.equals(aliceBobKey, aliceCharlieKey),
                "Different conversations should have different keys");

        // Message encrypted for Bob should not be decryptable with Charlie's key
        String message = "For Bob only";
        byte[] nonce = cipher.generateNonce();

        SecretKeySpec bobKey = new SecretKeySpec(aliceBobKey, "AES");
        SecretKeySpec charlieKey = new SecretKeySpec(aliceCharlieKey, "AES");

        byte[] encryptedForBob = cipher.encrypt(
                message.getBytes(StandardCharsets.UTF_8),
                bobKey,
                nonce,
                null
        );

        // Charlie cannot decrypt message intended for Bob
        assertThrows(Exception.class, () -> {
            cipher.decrypt(encryptedForBob, charlieKey, nonce, null);
        }, "Charlie should not be able to decrypt message encrypted for Bob");
    }
}