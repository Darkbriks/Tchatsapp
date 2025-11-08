package fr.uga.im2ag.m1info.chatservice.crypto;
 
import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
 
/**
 * Manages session keys and sequence numbers for conversations.
 * Provides replay protection and key rotation.
 */
public class SessionKeyManager {
 
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
 
    /**
     * Stores information about an encryption session.
     */
    private static class SessionInfo {
        SecretKey key;
        AtomicLong sendSequence;
        AtomicLong receiveSequence;
 
        SessionInfo(SecretKey key) {
            this.key = key;
            this.sendSequence = new AtomicLong(0);
            this.receiveSequence = new AtomicLong(0);
        }
    }
 
    /**
     * Stores a session key for a conversation.
     * @param conversationId The conversation identifier
     * @param key The session key
     */
    public void storeSessionKey(String conversationId, SecretKey key) {
        sessions.put(conversationId, new SessionInfo(key));
    }
 
    /**
     * Retrieves the session key for a conversation.
     * @param conversationId The conversation identifier
     * @return The session key or null if not found
     */
    public SecretKey getSessionKey(String conversationId) {
        SessionInfo info = sessions.get(conversationId);
        return info != null ? info.key : null;
    }
 
    /**
     * Gets and increments the send sequence number for a conversation.
     * @param conversationId The conversation identifier
     * @return The next sequence number to use
     */
    public long getNextSendSequence(String conversationId) {
        SessionInfo info = sessions.get(conversationId);
        if (info == null) {
            throw new IllegalStateException("No session for conversation: " + conversationId);
        }
        return info.sendSequence.getAndIncrement();
    }
 
    /**
     * Validates and updates the receive sequence number.
     * Prevents replay attacks by ensuring sequence numbers are increasing.
     * @param conversationId The conversation identifier
     * @param sequence The received sequence number
     * @return true if sequence is valid, false if it's a replay
     */
    public boolean validateReceiveSequence(String conversationId, long sequence) {
        SessionInfo info = sessions.get(conversationId);
        if (info == null) {
            throw new IllegalStateException("No session for conversation: " + conversationId);
        }
 
        long expected = info.receiveSequence.get();
        if (sequence <= expected) {
            return false; // Replay or out-of-order
        }
 
        info.receiveSequence.set(sequence);
        return true;
    }
 
    /**
     * Checks if a session exists for a conversation.
     * @param conversationId The conversation identifier
     * @return true if session exists
     */
    public boolean hasSession(String conversationId) {
        return sessions.containsKey(conversationId);
    }
 
    /**
     * Removes a session (useful for key rotation).
     * @param conversationId The conversation identifier
     */
    public void removeSession(String conversationId) {
        sessions.remove(conversationId);
    }
}