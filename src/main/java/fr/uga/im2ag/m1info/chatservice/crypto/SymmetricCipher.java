package fr.uga.im2ag.m1info.chatservice.crypto;
 
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
 
/**
 * Provides AES-256-GCM authenticated encryption for message payloads.
 * GCM mode provides both confidentiality and authenticity.
 */
public class SymmetricCipher {
 
    /**
     * Encrypts plaintext using AES-256-GCM.
     * @param plaintext The data to encrypt
     * @param key The AES secret key
     * @param nonce The nonce (must be unique per encryption)
     * @param associatedData Additional authenticated data (can be null)
     * @return The ciphertext with authentication tag appended
     * @throws GeneralSecurityException if encryption fails
     */
    public byte[] encrypt(byte[] plaintext, SecretKey key, byte[] nonce, byte[] associatedData)
            throws GeneralSecurityException {
        // TODO: Implement AES-GCM encryption
        throw new UnsupportedOperationException("Not implemented yet");
    }
 
    /**
     * Decrypts ciphertext using AES-256-GCM and verifies authentication tag.
     * @param ciphertext The encrypted data with authentication tag
     * @param key The AES secret key
     * @param nonce The nonce used during encryption
     * @param associatedData Additional authenticated data (must match encryption)
     * @return The decrypted plaintext
     * @throws GeneralSecurityException if decryption or authentication fails
     */
    public byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] nonce, byte[] associatedData)
            throws GeneralSecurityException {
        // TODO: Implement AES-GCM decryption with authentication
        throw new UnsupportedOperationException("Not implemented yet");
    }
 
    /**
     * Generates a secure random nonce for GCM mode.
     * @return A 12-byte random nonce
     */
    public byte[] generateNonce() {
        // TODO: Implement secure nonce generation
        throw new UnsupportedOperationException("Not implemented yet");
    }
}