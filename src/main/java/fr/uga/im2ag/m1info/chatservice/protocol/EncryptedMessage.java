package fr.uga.im2ag.m1info.chatservice.protocol;
 
import java.nio.ByteBuffer;
 
/**
 * Represents an encrypted message with type, sequence number, nonce, and ciphertext.
 * Provides serialization and deserialization for the encrypted payload format.
 */
public class EncryptedMessage {
 
    private final MessageType type;
    private final long sequenceNumber;
    private final byte[] nonce;
    private final byte[] ciphertext;
 
    /**
     * Constructs an encrypted message.
     * @param type The message type
     * @param sequenceNumber The sequence number for replay protection
     * @param nonce The nonce used for encryption
     * @param ciphertext The encrypted payload (including GCM tag)
     */
    public EncryptedMessage(MessageType type, long sequenceNumber, byte[] nonce, byte[] ciphertext) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.nonce = nonce;
        this.ciphertext = ciphertext;
    }
 
    public MessageType getType() {
        return type;
    }
 
    public long getSequenceNumber() {
        return sequenceNumber;
    }
 
    public byte[] getNonce() {
        return nonce;
    }
 
    public byte[] getCiphertext() {
        return ciphertext;
    }
 
    /**
     * Serializes the message to bytes.
     * Format: [1 byte type][8 bytes sequence][12 bytes nonce][N bytes ciphertext]
     * @return The serialized message
     */
    public byte[] serialize() {
        // TODO: Implement serialization
        throw new UnsupportedOperationException("Not implemented yet");
    }
 
    /**
     * Deserializes a message from bytes.
     * @param data The serialized message data
     * @return The deserialized EncryptedMessage
     * @throws IllegalArgumentException if data is invalid
     */
    public static EncryptedMessage deserialize(byte[] data) {
        // TODO: Implement deserialization
        throw new UnsupportedOperationException("Not implemented yet");
    }
}