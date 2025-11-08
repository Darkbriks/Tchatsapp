package fr.uga.im2ag.m1info.chatservice.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Utility class for generating unique message IDs,
 * using SHA-256, user ID and timestamp.
 * Security is not a goal here, just uniqueness.
 * <p>
 * Message ID can be useful to have responses to specific messages,
 * or reactions, for example.
 */
public class MessageIdGenerator {
    /**
     * Generate a unique message ID based on user ID and current timestamp.
     *
     * @param userId    the ID of the user sending the message
     * @param timestamp the timestamp of the message
     * @return a unique message ID as a hexadecimal string
     */
    public static String generateMessageId(int userId, long timestamp) {
        try {
            String input = userId + "-" + timestamp;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating message ID", e);
        }
    }
}
