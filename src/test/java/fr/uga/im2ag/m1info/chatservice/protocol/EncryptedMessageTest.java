package fr.uga.im2ag.m1info.chatservice.protocol;
 
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
 
import java.util.Arrays;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Unit tests for EncryptedMessage serialization and deserialization.
 */
class EncryptedMessageTest {
 
    @Test
    @DisplayName("Serialize and deserialize should work correctly")
    void testSerializeDeserialize() {
        // Create a test message
        MessageType type = MessageType.ENCRYPTED_TEXT;
        long sequence = 42;
        byte[] nonce = new byte[12];
        Arrays.fill(nonce, (byte) 0xAB);
        byte[] ciphertext = "encrypted_data_here".getBytes();
 
        EncryptedMessage original = new EncryptedMessage(type, sequence, nonce, ciphertext);
 
        // Serialize
        byte[] serialized = original.serialize();
 
        // Deserialize
        EncryptedMessage deserialized = EncryptedMessage.deserialize(serialized);
 
        // Verify all fields match
        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getSequenceNumber(), deserialized.getSequenceNumber());
        assertArrayEquals(original.getNonce(), deserialized.getNonce());
        assertArrayEquals(original.getCiphertext(), deserialized.getCiphertext());
    }
 
    @Test
    @DisplayName("Serialized format should have correct structure")
    void testSerializedFormat() {
        MessageType type = MessageType.ENCRYPTED_TEXT;
        long sequence = 100;
        byte[] nonce = new byte[12];
        byte[] ciphertext = new byte[32]; // 32 bytes ciphertext
 
        EncryptedMessage message = new EncryptedMessage(type, sequence, nonce, ciphertext);
        byte[] serialized = message.serialize();
 
        // Check total size: 1 (type) + 8 (sequence) + 12 (nonce) + 32 (ciphertext) = 53
        assertEquals(53, serialized.length);
 
        // Check type code at position 0
        assertEquals(type.getCode(), serialized[0]);
    }
 
    @Test
    @DisplayName("Different message types should serialize correctly")
    void testDifferentMessageTypes() {
        byte[] nonce = new byte[12];
        byte[] ciphertext = new byte[16];
 
        for (MessageType type : MessageType.values()) {
            EncryptedMessage message = new EncryptedMessage(type, 1, nonce, ciphertext);
            byte[] serialized = message.serialize();
            EncryptedMessage deserialized = EncryptedMessage.deserialize(serialized);
 
            assertEquals(type, deserialized.getType());
        }
    }
 
    @Test
    @DisplayName("Large sequence numbers should work")
    void testLargeSequenceNumber() {
        MessageType type = MessageType.ENCRYPTED_TEXT;
        long sequence = Long.MAX_VALUE;
        byte[] nonce = new byte[12];
        byte[] ciphertext = new byte[16];
 
        EncryptedMessage message = new EncryptedMessage(type, sequence, nonce, ciphertext);
        byte[] serialized = message.serialize();
        EncryptedMessage deserialized = EncryptedMessage.deserialize(serialized);
 
        assertEquals(sequence, deserialized.getSequenceNumber());
    }
 
    @Test
    @DisplayName("Empty ciphertext should work")
    void testEmptyCiphertext() {
        MessageType type = MessageType.ENCRYPTED_TEXT;
        long sequence = 1;
        byte[] nonce = new byte[12];
        byte[] ciphertext = new byte[0];
 
        EncryptedMessage message = new EncryptedMessage(type, sequence, nonce, ciphertext);
        byte[] serialized = message.serialize();
 
        // Should have minimum size: 1 + 8 + 12 = 21 bytes
        assertEquals(21, serialized.length);
 
        EncryptedMessage deserialized = EncryptedMessage.deserialize(serialized);
        assertEquals(0, deserialized.getCiphertext().length);
    }
 
    @Test
    @DisplayName("Large ciphertext should work")
    void testLargeCiphertext() {
        MessageType type = MessageType.ENCRYPTED_FILE_CHUNK;
        long sequence = 5;
        byte[] nonce = new byte[12];
        byte[] ciphertext = new byte[64 * 1024]; // 64 KB
        Arrays.fill(ciphertext, (byte) 0xFF);
 
        EncryptedMessage message = new EncryptedMessage(type, sequence, nonce, ciphertext);
        byte[] serialized = message.serialize();
        EncryptedMessage deserialized = EncryptedMessage.deserialize(serialized);
 
        assertArrayEquals(ciphertext, deserialized.getCiphertext());
    }
 
    @Test
    @DisplayName("Deserialize null data should throw exception")
    void testDeserializeNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptedMessage.deserialize(null);
        });
    }
 
    @Test
    @DisplayName("Deserialize data too small should throw exception")
    void testDeserializeTooSmall() {
        byte[] tooSmall = new byte[20]; // Need at least 21 bytes
 
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptedMessage.deserialize(tooSmall);
        });
    }
 
    @Test
    @DisplayName("Deserialize with invalid message type should throw exception")
    void testDeserializeInvalidMessageType() {
        byte[] data = new byte[21];
        data[0] = (byte) 0xFF; // Invalid message type code
 
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptedMessage.deserialize(data);
        });
    }
 
    @Test
    @DisplayName("Serialize with null nonce should throw exception")
    void testSerializeNullNonce() {
        EncryptedMessage message = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                1,
                null, // null nonce
                new byte[16]
        );
 
        assertThrows(IllegalStateException.class, () -> {
            message.serialize();
        });
    }
 
    @Test
    @DisplayName("Serialize with null ciphertext should throw exception")
    void testSerializeNullCiphertext() {
        EncryptedMessage message = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                1,
                new byte[12],
                null // null ciphertext
        );
 
        assertThrows(IllegalStateException.class, () -> {
            message.serialize();
        });
    }
 
    @Test
    @DisplayName("Get serialized size should return correct value")
    void testGetSerializedSize() {
        byte[] nonce = new byte[12];
        byte[] ciphertext = new byte[100];
 
        EncryptedMessage message = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                1,
                nonce,
                ciphertext
        );
 
        // Expected: 1 + 8 + 12 + 100 = 121
        assertEquals(121, message.getSerializedSize());
    }
 
    @Test
    @DisplayName("Multiple serialization should produce same result")
    void testSerializationDeterministic() {
        MessageType type = MessageType.ENCRYPTED_TEXT;
        long sequence = 10;
        byte[] nonce = new byte[12];
        Arrays.fill(nonce, (byte) 0x42);
        byte[] ciphertext = "test_data".getBytes();
 
        EncryptedMessage message = new EncryptedMessage(type, sequence, nonce, ciphertext);
 
        byte[] serialized1 = message.serialize();
        byte[] serialized2 = message.serialize();
 
        assertArrayEquals(serialized1, serialized2);
    }
 
    @Test
    @DisplayName("Nonce should be preserved exactly")
    void testNoncePreservation() {
        byte[] nonce = new byte[12];
        for (int i = 0; i < 12; i++) {
            nonce[i] = (byte) i;
        }
 
        EncryptedMessage message = new EncryptedMessage(
                MessageType.ENCRYPTED_TEXT,
                1,
                nonce,
                new byte[16]
        );
 
        byte[] serialized = message.serialize();
        EncryptedMessage deserialized = EncryptedMessage.deserialize(serialized);
 
        assertArrayEquals(nonce, deserialized.getNonce());
    }
 
    @Test
    @DisplayName("All message type codes should be reversible")
    void testMessageTypeCodeReversibility() {
        for (MessageType type : MessageType.values()) {
            byte code = type.getCode();
            MessageType decoded = MessageType.fromCode(code);
            assertEquals(type, decoded);
        }
    }
}