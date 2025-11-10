package fr.uga.im2ag.m1info.chatservice.protocol;
 
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Unit tests for ProtocolUtils.
 */
class ProtocolUtilsTest {
 
    @Test
    @DisplayName("String to bytes and back should work")
    void testStringBytesConversion() {
        String original = "Hello, World! 你好";
        byte[] bytes = ProtocolUtils.stringToBytes(original);
        String converted = ProtocolUtils.bytesToString(bytes);
 
        assertEquals(original, converted);
    }
 
    @Test
    @DisplayName("Null string should convert to empty bytes")
    void testNullStringToBytes() {
        byte[] bytes = ProtocolUtils.stringToBytes(null);
        assertEquals(0, bytes.length);
    }
 
    @Test
    @DisplayName("Null bytes should convert to empty string")
    void testNullBytesToString() {
        String str = ProtocolUtils.bytesToString(null);
        assertEquals("", str);
    }
 
    @Test
    @DisplayName("Empty bytes should convert to empty string")
    void testEmptyBytesToString() {
        String str = ProtocolUtils.bytesToString(new byte[0]);
        assertEquals("", str);
    }
 
    @Test
    @DisplayName("Format timestamp should produce readable string")
    void testFormatTimestamp() {
        long timestamp = 1704902400000L; // 2024-01-10 12:00:00 UTC (approx)
        String formatted = ProtocolUtils.formatTimestamp(timestamp);
 
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Timestamp should match format: yyyy-MM-dd HH:mm:ss");
    }
 
    @Test
    @DisplayName("Get current timestamp should return reasonable value")
    void testGetCurrentTimestamp() {
        long now = ProtocolUtils.getCurrentTimestamp();
        long systemNow = System.currentTimeMillis();
 
        // Should be within 1 second
        assertTrue(Math.abs(now - systemNow) < 1000);
    }
 
    @Test
    @DisplayName("Conversation ID should be deterministic")
    void testConversationIdDeterministic() {
        String id1 = ProtocolUtils.createConversationId(10, 20);
        String id2 = ProtocolUtils.createConversationId(10, 20);
 
        assertEquals(id1, id2);
    }
 
    @Test
    @DisplayName("Conversation ID should be symmetric")
    void testConversationIdSymmetric() {
        String id1 = ProtocolUtils.createConversationId(10, 20);
        String id2 = ProtocolUtils.createConversationId(20, 10);
 
        assertEquals(id1, id2, "Conversation ID should be same regardless of order");
    }
 
    @Test
    @DisplayName("Different client pairs should have different conversation IDs")
    void testConversationIdUnique() {
        String id1 = ProtocolUtils.createConversationId(1, 2);
        String id2 = ProtocolUtils.createConversationId(1, 3);
        String id3 = ProtocolUtils.createConversationId(2, 3);
 
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);
    }
 
    @Test
    @DisplayName("Group conversation ID should include group identifier")
    void testGroupConversationId() {
        String groupId = "team-alpha";
        String conversationId = ProtocolUtils.createGroupConversationId(groupId);
 
        assertTrue(conversationId.contains(groupId));
        assertTrue(conversationId.startsWith("group-"));
    }
 
    @Test
    @DisplayName("Null group ID should throw exception")
    void testNullGroupId() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.createGroupConversationId(null);
        });
    }
 
    @Test
    @DisplayName("Empty group ID should throw exception")
    void testEmptyGroupId() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.createGroupConversationId("");
        });
    }
 
    @Test
    @DisplayName("Valid sequence number should pass validation")
    void testValidateSequenceNumberValid() {
        assertDoesNotThrow(() -> {
            ProtocolUtils.validateSequenceNumber(0);
            ProtocolUtils.validateSequenceNumber(1);
            ProtocolUtils.validateSequenceNumber(1000);
            ProtocolUtils.validateSequenceNumber(Long.MAX_VALUE);
        });
    }
 
    @Test
    @DisplayName("Negative sequence number should throw exception")
    void testValidateSequenceNumberNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateSequenceNumber(-1);
        });
    }
 
    @Test
    @DisplayName("Valid nonce should pass validation")
    void testValidateNonceValid() {
        byte[] nonce = new byte[12];
        assertDoesNotThrow(() -> {
            ProtocolUtils.validateNonce(nonce);
        });
    }
 
    @Test
    @DisplayName("Null nonce should throw exception")
    void testValidateNonceNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateNonce(null);
        });
    }
 
    @Test
    @DisplayName("Wrong size nonce should throw exception")
    void testValidateNonceWrongSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateNonce(new byte[8]);
        });
 
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateNonce(new byte[16]);
        });
    }
 
    @Test
    @DisplayName("Valid ciphertext should pass validation")
    void testValidateCiphertextValid() {
        byte[] ciphertext = new byte[16]; // Minimum for GCM tag
        assertDoesNotThrow(() -> {
            ProtocolUtils.validateCiphertext(ciphertext);
        });
    }
 
    @Test
    @DisplayName("Null ciphertext should throw exception")
    void testValidateCiphertextNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateCiphertext(null);
        });
    }
 
    @Test
    @DisplayName("Too small ciphertext should throw exception")
    void testValidateCiphertextTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateCiphertext(new byte[15]); // Less than 16 bytes
        });
    }
 
    @Test
    @DisplayName("Valid client ID should pass validation")
    void testValidateClientIdValid() {
        assertDoesNotThrow(() -> {
            ProtocolUtils.validateClientId(1);
            ProtocolUtils.validateClientId(100);
            ProtocolUtils.validateClientId(Integer.MAX_VALUE);
        });
    }
 
    @Test
    @DisplayName("Invalid client ID should throw exception")
    void testValidateClientIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateClientId(0);
        });
 
        assertThrows(IllegalArgumentException.class, () -> {
            ProtocolUtils.validateClientId(-1);
        });
    }
 
    @Test
    @DisplayName("Encrypted text message requires encryption")
    void testRequiresEncryptionForEncryptedTypes() {
        assertTrue(ProtocolUtils.requiresEncryption(MessageType.ENCRYPTED_TEXT));
        assertTrue(ProtocolUtils.requiresEncryption(MessageType.ENCRYPTED_FILE_CHUNK));
        assertTrue(ProtocolUtils.requiresEncryption(MessageType.FILE_TRANSFER_START));
        assertTrue(ProtocolUtils.requiresEncryption(MessageType.FILE_TRANSFER_ACK));
        assertTrue(ProtocolUtils.requiresEncryption(MessageType.GROUP_KEY_DISTRIBUTION));
    }
 
    @Test
    @DisplayName("Key exchange messages don't require encryption")
    void testRequiresEncryptionForKeyExchange() {
        assertFalse(ProtocolUtils.requiresEncryption(MessageType.KEY_EXCHANGE));
        assertFalse(ProtocolUtils.requiresEncryption(MessageType.KEY_EXCHANGE_RESPONSE));
    }
 
    @Test
    @DisplayName("Create packet from encrypted message should work")
    void testCreatePacketFromEncryptedMessage() {
        byte[] nonce = new byte[12];
        byte[] ciphertext = new byte[32];
        EncryptedMessage encMsg = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                1,
                nonce,
                ciphertext
        );
 
        var packet = ProtocolUtils.createPacketFromEncryptedMessage(10, 20, encMsg);
 
        assertNotNull(packet);
        assertEquals(10, packet.from());
        assertEquals(20, packet.to());
        assertTrue(packet.payloadSize() > 0);
    }
 
    @Test
    @DisplayName("Extract encrypted message from packet should work")
    void testExtractEncryptedMessageFromPacket() {
        byte[] nonce = new byte[12];
        byte[] ciphertext = "test_ciphertext".getBytes();
        EncryptedMessage original = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                42,
                nonce,
                ciphertext
        );
 
        // Create packet
        var packet = ProtocolUtils.createPacketFromEncryptedMessage(10, 20, original);
 
        // Extract message
        EncryptedMessage extracted = ProtocolUtils.extractEncryptedMessageFromPacket(packet);
 
        assertEquals(original.getType(), extracted.getType());
        assertEquals(original.getSequenceNumber(), extracted.getSequenceNumber());
        assertArrayEquals(original.getNonce(), extracted.getNonce());
        assertArrayEquals(original.getCiphertext(), extracted.getCiphertext());
    }
}