package fr.uga.im2ag.m1info.chatservice.crypto.strategy;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.EncryptedWrapper;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.security.GeneralSecurityException;

/**
 * Strategy interface for encrypting and decrypting messages.
 */
public interface EncryptionStrategy {

    /**
     * Determine if a message should be encrypted.
     *
     * @param type the message type
     * @param recipientId the recipient (0 = server)
     * @return true if the message should be encrypted
     */
    boolean shouldEncrypt(MessageType type, int recipientId);

    /**
     * Encrypt a message.
     *
     * @param message the original message
     * @return EncryptedWrapper or the original message if no encryption
     */
    ProtocolMessage encrypt(ProtocolMessage message) throws GeneralSecurityException;

    /**
     * Decrypt an encrypted wrapper.
     *
     * @param wrapper the encrypted message
     * @return the original decrypted message
     */
    ProtocolMessage decrypt(EncryptedWrapper wrapper) throws GeneralSecurityException;

    /**
     * Check if a session key exists with a peer.
     *
     * @param peerId the peer id
     * @return true if a session key exists
     */
    boolean hasSessionKey(int peerId);

    /**
     * Initiate a key exchange with a peer.
     *
     * @param peerId the peer id
     * @throws GeneralSecurityException if an error occurs during key exchange
     */
    void initiateKeyExchange(int peerId) throws GeneralSecurityException;

    /**
     * Determine if a message type is excluded from encryption.
     *
     * @param type the message type
     * @return true if the message type is excluded from encryption
     */
    default boolean isExcludedFromEncryption(MessageType type) {
        return type == MessageType.KEY_EXCHANGE
                || type == MessageType.KEY_EXCHANGE_RESPONSE
                || type == MessageType.MESSAGE_ACK;
    }
}