package fr.uga.im2ag.m1info.chatservice.storage;
 
import javax.crypto.SecretKey;
import java.io.IOException;
 
/**
 * Handles secure storage of cryptographic keys.
 * Keys are encrypted at rest using a master key derived from the user's password.
 */
public class KeyStore {
 
    /**
     * Saves a session key to persistent storage.
     * @param conversationId The conversation identifier
     * @param key The session key to store
     * @throws IOException if storage fails
     */
    public void saveSessionKey(String conversationId, SecretKey key) throws IOException {
        // TODO: Implement encrypted key storage
        throw new UnsupportedOperationException("Not implemented yet");
    }
 
    /**
     * Loads a session key from persistent storage.
     * @param conversationId The conversation identifier
     * @return The session key or null if not found
     * @throws IOException if loading fails
     */
    public SecretKey loadSessionKey(String conversationId) throws IOException {
        // TODO: Implement key loading
        throw new UnsupportedOperationException("Not implemented yet");
    }
 
    /**
     * Deletes a session key from storage.
     * @param conversationId The conversation identifier
     * @throws IOException if deletion fails
     */
    public void deleteSessionKey(String conversationId) throws IOException {
        // TODO: Implement key deletion
        throw new UnsupportedOperationException("Not implemented yet");
    }
}