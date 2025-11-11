package fr.uga.im2ag.m1info.chatservice.common;

/** Utility class for generating unique message IDs,
 * using SHA-256, user ID and timestamp.
 * Security is not a goal here, just uniqueness.
 * <p>
 * Message ID can be useful to have responses to specific messages,
 * or reactions, for example.
 */
public interface MessageIdGenerator {
    String generateId(int userId, long timestamp);
}
