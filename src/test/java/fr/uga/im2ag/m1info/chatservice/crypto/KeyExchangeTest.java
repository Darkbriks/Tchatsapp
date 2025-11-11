package fr.uga.im2ag.m1info.chatservice.crypto;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
 
import java.security.KeyPair;
import java.util.Arrays;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Unit tests for KeyExchange class.
 * Tests ECDH key generation, shared secret derivation, and HKDF key derivation.
 */
class KeyExchangeTest {
 
    private KeyExchange keyExchange;
 
    @BeforeEach
    void setUp() {
        keyExchange = new KeyExchange();
    }
 
    @Test
    @DisplayName("Generate keypair should create valid keys")
    void testGenerateKeyPair() throws Exception {
        KeyPair keyPair = keyExchange.generateKeyPair();
 
        assertNotNull(keyPair, "KeyPair should not be null");
        assertNotNull(keyPair.getPrivate(), "Private key should not be null");
        assertNotNull(keyPair.getPublic(), "Public key should not be null");
        assertEquals("X25519", keyPair.getPrivate().getAlgorithm());
        assertEquals("X25519", keyPair.getPublic().getAlgorithm());
    }
 
    @Test
    @DisplayName("Generate multiple keypairs should produce different keys")
    void testGenerateKeyPairUniqueness() throws Exception {
        KeyPair keyPair1 = keyExchange.generateKeyPair();
        KeyPair keyPair2 = keyExchange.generateKeyPair();
 
        assertFalse(Arrays.equals(keyPair1.getPublic().getEncoded(), keyPair2.getPublic().getEncoded()),
                "Two generated public keys should be different");
        assertFalse(Arrays.equals(keyPair1.getPrivate().getEncoded(), keyPair2.getPrivate().getEncoded()),
                "Two generated private keys should be different");
    }
 
    @Test
    @DisplayName("Both parties should derive the same shared secret")
    void testDeriveSharedSecretSymmetry() throws Exception {
        /*
         * The following test made also sure that:
         * - Session keys should be the same for both parties with same conversation ID
         * - Derive session key should produce 32-byte key
         */
        // Alice and Bob each generate keypairs
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();
        KeyPair bobKeyPair = keyExchange.generateKeyPair();
 
        // Alice computes shared secret using her private key and Bob's public key
        byte[] aliceSharedSecret = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );
 
        // Bob computes shared secret using his private key and Alice's public key
        byte[] bobSharedSecret = keyExchange.deriveSharedSecret(
                bobKeyPair.getPrivate(),
                aliceKeyPair.getPublic()
        );
 
        // Both should get the same shared secret
        assertNotNull(aliceSharedSecret, "Alice's shared secret should not be null");
        assertNotNull(bobSharedSecret, "Bob's shared secret should not be null");
        assertEquals(32, aliceSharedSecret.length, "X25519 shared secret should be 32 bytes");
        assertArrayEquals(aliceSharedSecret, bobSharedSecret,
                "Alice and Bob should derive the same shared secret");
    }
 
    @Test
    @DisplayName("Shared secret should be different for different key pairs")
    void testDeriveSharedSecretUniqueness() throws Exception {
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();
        KeyPair bobKeyPair1 = keyExchange.generateKeyPair();
        KeyPair bobKeyPair2 = keyExchange.generateKeyPair();
 
        byte[] sharedSecret1 = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair1.getPublic()
        );
 
        byte[] sharedSecret2 = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair2.getPublic()
        );
 
        assertFalse(Arrays.equals(sharedSecret1, sharedSecret2),
                "Shared secrets with different key pairs should be different");
    }
 
 
    @Test
    @DisplayName("Different conversation IDs should produce different session keys")
    void testDeriveSessionKeyConversationBinding() throws Exception {
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();
        KeyPair bobKeyPair = keyExchange.generateKeyPair();
 
        byte[] sharedSecret = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );
 
        byte[] sessionKey1 = keyExchange.deriveSessionKey(sharedSecret, "conversation-1");
        byte[] sessionKey2 = keyExchange.deriveSessionKey(sharedSecret, "conversation-2");
 
        assertFalse(Arrays.equals(sessionKey1, sessionKey2),
                "Different conversation IDs should produce different session keys");
    }
 
    @Test
    @DisplayName("HKDF should handle empty conversation ID")
    void testDeriveSessionKeyEmptyConversation() throws Exception {
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();
        KeyPair bobKeyPair = keyExchange.generateKeyPair();
 
        byte[] sharedSecret = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );
 
        byte[] sessionKey = keyExchange.deriveSessionKey(sharedSecret, "");
 
        assertNotNull(sessionKey, "Session key with empty conversation ID should not be null");
        assertEquals(32, sessionKey.length, "Session key should still be 32 bytes");
    }
 
    @Test
    @DisplayName("Session key derivation should be deterministic")
    void testDeriveSessionKeyDeterministic() throws Exception {
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();
        KeyPair bobKeyPair = keyExchange.generateKeyPair();
 
        byte[] sharedSecret = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );
 
        String conversationId = "test-conversation";
 
        // Derive the same session key twice
        byte[] sessionKey1 = keyExchange.deriveSessionKey(sharedSecret, conversationId);
        byte[] sessionKey2 = keyExchange.deriveSessionKey(sharedSecret, conversationId);
 
        assertArrayEquals(sessionKey1, sessionKey2,
                "Deriving session key twice with same input should produce same result");
    }
 
    @Test
    @DisplayName("Null conversation ID should throw exception")
    void testDeriveSessionKeyNullConversation() throws Exception {
        KeyPair aliceKeyPair = keyExchange.generateKeyPair();
        KeyPair bobKeyPair = keyExchange.generateKeyPair();
 
        byte[] sharedSecret = keyExchange.deriveSharedSecret(
                aliceKeyPair.getPrivate(),
                bobKeyPair.getPublic()
        );
 
        assertThrows(NullPointerException.class, () -> {
            keyExchange.deriveSessionKey(sharedSecret, null);
        }, "Deriving session key with null conversation ID should throw exception");
    }
}