package fr.uga.im2ag.m1info.chatservice.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SymmetricCipher class.
 * Tests AES-GCM encryption, decryption, authentication, and tampering detection.
 */
class SymmetricCipherTest {

    private SymmetricCipher cipher;
    private SecretKey testKey;

    @BeforeEach
    void setUp() throws Exception {
        cipher = new SymmetricCipher();

        // Generate a test AES-256 key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        testKey = keyGen.generateKey();
    }

    @Test
    @DisplayName("Generate nonce should create 12-byte nonce")
    void testGenerateNonce() {
        byte[] nonce = cipher.generateNonce();

        assertNotNull(nonce, "Nonce should not be null");
        assertEquals(12, nonce.length, "Nonce should be 12 bytes for GCM");
    }

    @Test
    @DisplayName("Generated nonces should be unique")
    void testGenerateNonceUniqueness() {
        byte[] nonce1 = cipher.generateNonce();
        byte[] nonce2 = cipher.generateNonce();

        assertFalse(Arrays.equals(nonce1, nonce2),
                "Two generated nonces should be different");
    }

    @Test
    @DisplayName("Encrypt and decrypt should work correctly")
    void testEncryptDecryptRoundTrip() throws Exception {
        String plaintext = "Hello, World!";
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] nonce = cipher.generateNonce();

        // Encrypt
        byte[] ciphertext = cipher.encrypt(plaintextBytes, testKey, nonce, null);

        assertNotNull(ciphertext, "Ciphertext should not be null");
        assertTrue(ciphertext.length > plaintextBytes.length,
                "Ciphertext should be longer than plaintext (includes GCM tag)");

        // Decrypt
        byte[] decrypted = cipher.decrypt(ciphertext, testKey, nonce, null);

        assertArrayEquals(plaintextBytes, decrypted,
                "Decrypted text should match original plaintext");
        assertEquals(plaintext, new String(decrypted, StandardCharsets.UTF_8),
                "Decrypted string should match original string");
    }

    @Test
    @DisplayName("Encrypt with associated data should work")
    void testEncryptDecryptWithAssociatedData() throws Exception {
        byte[] plaintext = "Secret message".getBytes(StandardCharsets.UTF_8);
        byte[] associatedData = "header-info".getBytes(StandardCharsets.UTF_8);
        byte[] nonce = cipher.generateNonce();

        // Encrypt with associated data
        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce, associatedData);

        // Decrypt with same associated data
        byte[] decrypted = cipher.decrypt(ciphertext, testKey, nonce, associatedData);

        assertArrayEquals(plaintext, decrypted,
                "Decrypted text should match original when AAD matches");
    }

    @Test
    @DisplayName("Decrypt with wrong associated data should fail")
    void testDecryptWithWrongAssociatedData() throws Exception {
        byte[] plaintext = "Secret message".getBytes(StandardCharsets.UTF_8);
        byte[] associatedData1 = "header-1".getBytes(StandardCharsets.UTF_8);
        byte[] associatedData2 = "header-2".getBytes(StandardCharsets.UTF_8);
        byte[] nonce = cipher.generateNonce();

        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce, associatedData1);

        // Try to decrypt with different associated data
        assertThrows(GeneralSecurityException.class, () -> {
            cipher.decrypt(ciphertext, testKey, nonce, associatedData2);
        }, "Decryption with wrong AAD should fail");
    }

    @Test
    @DisplayName("Tampering with ciphertext should be detected")
    void testTamperingDetection() throws Exception {
        byte[] plaintext = "Important message".getBytes(StandardCharsets.UTF_8);
        byte[] nonce = cipher.generateNonce();

        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce, null);

        // Tamper with the ciphertext (flip a bit)
        ciphertext[0] ^= 0x01;

        // Decryption should fail due to authentication tag mismatch
        assertThrows(GeneralSecurityException.class, () -> {
            cipher.decrypt(ciphertext, testKey, nonce, null);
        }, "Tampering should be detected and cause decryption failure");
    }

    @Test
    @DisplayName("Tampering with authentication tag should be detected")
    void testAuthenticationTagTampering() throws Exception {
        byte[] plaintext = "Secure data".getBytes(StandardCharsets.UTF_8);
        byte[] nonce = cipher.generateNonce();

        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce, null);

        // Tamper with the last byte (part of GCM authentication tag)
        ciphertext[ciphertext.length - 1] ^= 0x01;

        assertThrows(GeneralSecurityException.class, () -> {
            cipher.decrypt(ciphertext, testKey, nonce, null);
        }, "Tampering with authentication tag should be detected");
    }

    @Test
    @DisplayName("Using wrong key for decryption should fail")
    void testWrongKeyDecryption() throws Exception {
        byte[] plaintext = "Message".getBytes(StandardCharsets.UTF_8);
        byte[] nonce = cipher.generateNonce();

        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce, null);

        // Generate a different key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey wrongKey = keyGen.generateKey();

        assertThrows(GeneralSecurityException.class, () -> {
            cipher.decrypt(ciphertext, wrongKey, nonce, null);
        }, "Decryption with wrong key should fail");
    }

    @Test
    @DisplayName("Using wrong nonce for decryption should fail")
    void testWrongNonceDecryption() throws Exception {
        byte[] plaintext = "Test message".getBytes(StandardCharsets.UTF_8);
        byte[] nonce1 = cipher.generateNonce();
        byte[] nonce2 = cipher.generateNonce();

        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce1, null);

        assertThrows(GeneralSecurityException.class, () -> {
            cipher.decrypt(ciphertext, testKey, nonce2, null);
        }, "Decryption with wrong nonce should fail");
    }

    @Test
    @DisplayName("Encrypt with invalid nonce length should throw exception")
    void testInvalidNonceLengthEncryption() {
        byte[] plaintext = "Test".getBytes(StandardCharsets.UTF_8);
        byte[] invalidNonce = new byte[16]; // Wrong length

        assertThrows(IllegalArgumentException.class, () -> {
            cipher.encrypt(plaintext, testKey, invalidNonce, null);
        }, "Encryption with invalid nonce length should throw exception");
    }

    @Test
    @DisplayName("Decrypt with invalid nonce length should throw exception")
    void testInvalidNonceLengthDecryption() {
        byte[] ciphertext = new byte[32];
        byte[] invalidNonce = new byte[8]; // Wrong length

        assertThrows(IllegalArgumentException.class, () -> {
            cipher.decrypt(ciphertext, testKey, invalidNonce, null);
        }, "Decryption with invalid nonce length should throw exception");
    }

    @Test
    @DisplayName("Encrypt with null nonce should throw exception")
    void testNullNonceEncryption() {
        byte[] plaintext = "Test".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> {
            cipher.encrypt(plaintext, testKey, null, null);
        }, "Encryption with null nonce should throw exception");
    }

    @Test
    @DisplayName("Encrypt empty message should work")
    void testEncryptEmptyMessage() throws Exception {
        byte[] plaintext = new byte[0];
        byte[] nonce = cipher.generateNonce();

        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce, null);
        assertNotNull(ciphertext, "Ciphertext of empty message should not be null");

        byte[] decrypted = cipher.decrypt(ciphertext, testKey, nonce, null);
        assertEquals(0, decrypted.length, "Decrypted empty message should be empty");
    }

    @Test
    @DisplayName("Encrypt large message should work")
    void testEncryptLargeMessage() throws Exception {
        // Create a 1MB message
        byte[] plaintext = new byte[1024 * 1024];
        Arrays.fill(plaintext, (byte) 'A');
        byte[] nonce = cipher.generateNonce();

        byte[] ciphertext = cipher.encrypt(plaintext, testKey, nonce, null);
        byte[] decrypted = cipher.decrypt(ciphertext, testKey, nonce, null);

        assertArrayEquals(plaintext, decrypted,
                "Large message should encrypt and decrypt correctly");
    }

    @Test
    @DisplayName("Same plaintext with different nonces should produce different ciphertexts")
    void testNonceUniquenessEffect() throws Exception {
        byte[] plaintext = "Same message".getBytes(StandardCharsets.UTF_8);
        byte[] nonce1 = cipher.generateNonce();
        byte[] nonce2 = cipher.generateNonce();

        byte[] ciphertext1 = cipher.encrypt(plaintext, testKey, nonce1, null);
        byte[] ciphertext2 = cipher.encrypt(plaintext, testKey, nonce2, null);

        assertFalse(Arrays.equals(ciphertext1, ciphertext2),
                "Same plaintext with different nonces should produce different ciphertexts");
    }

    @Test
    @DisplayName("Encryption should be deterministic with same inputs")
    void testEncryptionDeterministic() throws Exception {
        byte[] plaintext = "Deterministic test".getBytes(StandardCharsets.UTF_8);
        byte[] nonce = cipher.generateNonce();

        byte[] ciphertext1 = cipher.encrypt(plaintext, testKey, nonce, null);
        byte[] ciphertext2 = cipher.encrypt(plaintext, testKey, nonce, null);

        assertArrayEquals(ciphertext1, ciphertext2,
                "Encryption with same inputs should produce same output");
    }

    @Test
    @DisplayName("Get nonce length should return 12")
    void testGetNonceLength() {
        assertEquals(12, cipher.getNonceLength(),
                "Nonce length should be 12 bytes");
    }

    @Test
    @DisplayName("Get tag length should return 128")
    void testGetTagLength() {
        assertEquals(128, cipher.getTagLength(),
                "GCM tag length should be 128 bits");
    }
}