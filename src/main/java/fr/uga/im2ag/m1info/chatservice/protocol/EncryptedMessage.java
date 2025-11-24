package fr.uga.im2ag.m1info.chatservice.protocol;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.nio.ByteBuffer;
 
/**
 * Represents an encrypted message with type, sequence number, nonce, and ciphertext.
 * Provides serialization and deserialization for the encrypted payload format.
 */
@Deprecated
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
     * Minimum size: 21 bytes (1 + 8 + 12 + 0)
     *
     * @return The serialized message
     */
    public byte[] serialize() {
        if (nonce == null) {
            throw new IllegalStateException("Nonce cannot be null");
        }
        if (ciphertext == null) {
            throw new IllegalStateException("Ciphertext cannot be null");
        }
 
        // Calculate total size: 1 (type) + 8 (sequence) + 12 (nonce) + N (ciphertext)
        int totalSize = 1 + 8 + 12 + ciphertext.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
 
        // Write fields in order
        buffer.put(type.toByte());              // 1 byte: message type
        buffer.putLong(sequenceNumber);          // 8 bytes: sequence number
        buffer.put(nonce);                       // 12 bytes: nonce
        buffer.put(ciphertext);                  // N bytes: ciphertext + GCM tag
 
        return buffer.array();
    } 
    
    /**
     * Deserializes a message from bytes.
     *
     * @param data The serialized message data
     * @return The deserialized EncryptedMessage
     * @throws IllegalArgumentException if data is invalid or too small
     */
    public static EncryptedMessage deserialize(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
 
        // Minimum size: 1 (type) + 8 (sequence) + 12 (nonce) = 21 bytes
        final int MIN_SIZE = 21;
        if (data.length < MIN_SIZE) {
            throw new IllegalArgumentException(
                "Data too small: " + data.length + " bytes, minimum " + MIN_SIZE + " bytes required"
            );
        }
 
        ByteBuffer buffer = ByteBuffer.wrap(data);
 
        // Read fields in order
        byte typeCode = buffer.get();                    // 1 byte: message type
        MessageType type = MessageType.fromByte(typeCode);
 
        long sequenceNumber = buffer.getLong();          // 8 bytes: sequence number
 
        byte[] nonce = new byte[12];                     // 12 bytes: nonce
        buffer.get(nonce);
 
        // Remaining bytes are ciphertext (including GCM tag)
        int ciphertextLength = buffer.remaining();
        byte[] ciphertext = new byte[ciphertextLength];
        buffer.get(ciphertext);
 
        return new EncryptedMessage(type, sequenceNumber, nonce, ciphertext);
    }
 
    /**
     * Gets the total serialized size of this message.
     * @return The size in bytes (minimum 21 bytes)
     */
    public int getSerializedSize() {
        return 1 + 8 + 12 + (ciphertext != null ? ciphertext.length : 0);
    }

}