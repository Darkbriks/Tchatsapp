package fr.uga.im2ag.m1info.chatservice.crypto.strategy;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.EncryptedWrapper;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;

import java.security.GeneralSecurityException;

public class NoOpEncryptionStrategy implements EncryptionStrategy {
    @Override
    public boolean shouldEncrypt(MessageType type, int recipientId) {
        return false;
    }

    @Override
    public ProtocolMessage encrypt(ProtocolMessage message) throws GeneralSecurityException {
        return message;
    }

    @Override
    public ProtocolMessage decrypt(EncryptedWrapper wrapper) throws GeneralSecurityException {
        throw new GeneralSecurityException("NoOpEncryptionStrategy cannot decrypt messages");
    }

    @Override
    public boolean hasSessionKey(int peerId) {
        return false;
    }

    @Override
    public void initiateKeyExchange(int peerId) throws GeneralSecurityException {
        throw new GeneralSecurityException("NoOpEncryptionStrategy cannot initiate key exchange");
    }
}
