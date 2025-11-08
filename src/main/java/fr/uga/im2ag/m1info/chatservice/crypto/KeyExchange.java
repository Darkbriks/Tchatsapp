package fr.uga.im2ag.m1info.chatservice.crypto;

import java.security.*;

/**
 * Handles ECDH (Elliptic Curve Diffie-Hellman) key exchange for establishing
 * shared secrets between clients
 * Uses Curve25519 for secure and efficient key agreement
 */
public class KeyExchange {

    /**
     * Generates a new ECDH keypair for key exchange
     * @return A KeyPair containing private and public keys
     * @throws GeneralSecurityException if key generation fails
     */
    public KeyPair generateKeyPair() throws GeneralSecurityException {
        // TODO: Implement ECDH key generation using Curve25519
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Derives a shared secret from our private key and peer's public key.
     * @param myPrivateKey Our private key
     * @param peerPublicKey The peer's public key
     * @return The shared secret bytes
     * @throws GeneralSecurityException if key agreement fails
     */
    public byte[] deriveSharedSecret(PrivateKey myPrivateKey, PublicKey peerPublicKey)
            throws GeneralSecurityException {
        // TODO: Implement ECDH shared secret derivation
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Derives a session key from the shared secret using HKDF
     * @param sharedSecret The ECDH shared secret
     * @param conversationId Unique identifier for this conversation
     * @return The derived session key
     * @throws GeneralSecurityException if key derivation fails
     */
    public byte[] deriveSessionKey(byte[] sharedSecret, String conversationId)
            throws GeneralSecurityException {
        // TODO: Implement HKDF key derivation
        throw new UnsupportedOperationException("Not implemented yet");
    }
}