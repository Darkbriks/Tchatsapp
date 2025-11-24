package fr.uga.im2ag.m1info.chatservice.protocol;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
 
/**
 * Utility methods for protocol operations.
 * Provides conversions, validations, and helper functions for the messaging protocol.
 */
@Deprecated
public class ProtocolUtils {
 
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
 
    /**
     * Converts a string to UTF-8 bytes.
     * @param str The string to convert
     * @return The UTF-8 encoded bytes
     */
    public static byte[] stringToBytes(String str) {
        if (str == null) {
            return new byte[0];
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }
 
    /**
     * Converts UTF-8 bytes to a string.
     * @param bytes The bytes to convert
     * @return The decoded string
     */
    public static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
 
    /**
     * Formats a timestamp in milliseconds to a readable string.
     * @param timestampMillis The timestamp in milliseconds
     * @return Formatted timestamp string (e.g., "2025-01-10 15:30:45")
     */
    public static String formatTimestamp(long timestampMillis) {
        return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(timestampMillis));
    }
 
    /**
     * Gets the current timestamp in milliseconds.
     * @return Current time in milliseconds since epoch
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
 
    /**
     * Creates a conversation ID from two client IDs.
     * The conversation ID is deterministic: same pair always produces same ID.
     * Order doesn't matter: conversationId(A, B) == conversationId(B, A)
     *
     * @param clientId1 First client ID
     * @param clientId2 Second client ID
     * @return A unique conversation ID for this pair
     */
    public static String createConversationId(int clientId1, int clientId2) {
        // Ensure deterministic ordering (smaller ID first)
        int min = Math.min(clientId1, clientId2);
        int max = Math.max(clientId1, clientId2);
        return "chat-" + min + "-" + max;
    }
 
    /**
     * Creates a group conversation ID from a group identifier.
     * @param groupId The group identifier
     * @return A conversation ID for the group
     */
    public static String createGroupConversationId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            throw new IllegalArgumentException("Group ID cannot be null or empty");
        }
        return "group-" + groupId;
    }
 
    /**
     * Validates that a sequence number is valid (non-negative).
     * @param sequence The sequence number to validate
     * @throws IllegalArgumentException if sequence is negative
     */
    public static void validateSequenceNumber(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Sequence number cannot be negative: " + sequence);
        }
    }
 
    /**
     * Validates that a nonce has the correct length for GCM mode.
     * @param nonce The nonce to validate
     * @throws IllegalArgumentException if nonce is invalid
     */
    public static void validateNonce(byte[] nonce) {
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce cannot be null");
        }
        if (nonce.length != 12) {
            throw new IllegalArgumentException("Nonce must be 12 bytes, got " + nonce.length);
        }
    }
 
    /**
     * Validates that ciphertext is not null or empty.
     * @param ciphertext The ciphertext to validate
     * @throws IllegalArgumentException if ciphertext is invalid
     */
    public static void validateCiphertext(byte[] ciphertext) {
        if (ciphertext == null) {
            throw new IllegalArgumentException("Ciphertext cannot be null");
        }
        // GCM adds 16-byte tag, so minimum ciphertext is 16 bytes (empty plaintext)
        if (ciphertext.length < 16) {
            throw new IllegalArgumentException(
                "Ciphertext too small: " + ciphertext.length + " bytes (minimum 16 bytes for GCM tag)"
            );
        }
    }
 
    /**
     * Creates a Packet containing an EncryptedMessage.
     * This wraps the encrypted message in the existing Packet protocol.
     *
     * @param from Sender client ID
     * @param to Recipient client ID
     * @param encryptedMessage The encrypted message to wrap
     * @return A Packet ready to send
     */
    public static Packet createPacketFromEncryptedMessage(int from, int to, EncryptedMessage encryptedMessage) {
        byte[] payload = encryptedMessage.serialize();
        return new Packet.PacketBuilder(payload.length)
                .setFrom(from)
                .setTo(to)
                .setPayload(payload)
                .build();
    }
 
    /**
     * Extracts an EncryptedMessage from a Packet.
     * @param packet The packet to extract from
     * @return The deserialized EncryptedMessage
     * @throws IllegalArgumentException if packet payload is invalid
     */
    public static EncryptedMessage extractEncryptedMessageFromPacket(Packet packet) {
        byte[] payload = new byte[packet.getPayload().remaining()];
        packet.getPayload().get(payload);
        return EncryptedMessage.deserialize(payload);
    }
 
    /**
     * Validates a client ID is valid (positive).
     * @param clientId The client ID to validate
     * @throws IllegalArgumentException if client ID is invalid
     */
    public static void validateClientId(int clientId) {
        if (clientId <= 0) {
            throw new IllegalArgumentException("Client ID must be positive: " + clientId);
        }
    }
 
    /**
     * Checks if a message type requires encryption.
     * @param type The message type
     * @return true if this message type should be encrypted
     */
    public static boolean requiresEncryption(MessageType type) {
        // All message types should be encrypted except key exchange messages
        // (which themselves contain public keys that don't need encryption)
        return type != MessageType.KEY_EXCHANGE && type != MessageType.KEY_EXCHANGE_RESPONSE;
    }
}