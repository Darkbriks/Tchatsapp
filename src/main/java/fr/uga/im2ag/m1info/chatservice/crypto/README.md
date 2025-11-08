
### Cryptographic Primitives

**Key Agreement:**
- Algorithm: ECDH (Elliptic Curve Diffie-Hellman) with Curve25519
- Purpose: Establish shared secret between two clients
- Library: Java Cryptography Architecture (JCA) or BouncyCastle (a voir)

**Symmetric Encryption:**
- Algorithm: AES-256-GCM (Authenticated Encryption with Associated Data)
- Purpose: Encrypt message payloads with authentication
- Benefits: Provides both confidentiality and integrity
- Nonce: 96-bit random value per message (never reused)

**Key Derivation:**
- Algorithm: HKDF (HMAC-based Key Derivation Function)
- Purpose: Derive session keys from ECDH shared secret
- Input: Shared secret + conversation ID + nonce

**Random Number Generation:**
- Use: SecureRandom for nonces and key generation
- No hardcoded values

### Key Exchange Protocol

**Initial Key Exchange (per conversation):**

1. Client A generates ECDH keypair (private_A, public_A)
2. Client A sends KEY_EXCHANGE packet to Client B containing public_A
3. Client B generates ECDH keypair (private_B, public_B)
4. Client B computes shared secret: ECDH(private_B, public_A)
5. Client B sends KEY_EXCHANGE_RESPONSE with public_B
6. Client A computes shared secret: ECDH(private_A, public_B)
7. Both derive session key: HKDF(shared_secret, conversation_id)

**Session Key Management:**
- One session key per conversation (1-to-1 chat)
- Rotated after N messages or time period (e.g., 1000 messages or 24 hours)
- Stored encrypted at rest with master key derived from user password

### Message Protection

**Encrypted Payload Structure:**
```
[1 byte: message type] [8 bytes: sequence number] [12 bytes: nonce]
[N bytes: ciphertext] [16 bytes: GCM tag]
```

**Security Properties:**
- **Confidentiality:** AES-256 encryption
- **Authenticity:** GCM authentication tag
- **Replay Protection:** Sequence numbers (reject old/duplicate sequences)
- **Ordering:** Sequence numbers ensure correct message order
- **Forward Secrecy:** Session key rotation limits exposure

**Message Types:**
```
0x01 - KEY_EXCHANGE (public key)
0x02 - KEY_EXCHANGE_RESPONSE (public key)
0x03 - ENCRYPTED_TEXT (text message)
0x04 - ENCRYPTED_FILE_CHUNK (file data)
0x05 - FILE_TRANSFER_START (file metadata)
0x06 - FILE_TRANSFER_ACK (chunk acknowledgment)
0x07 - GROUP_KEY_DISTRIBUTION (encrypted group key)
```

### Group Chat Security

**Approach:** Server-Assisted Group Key Distribution (compatible with relay architecture)

1. Group creator generates symmetric group key (AES-256)
2. For each member, encrypt group key using their pairwise session key
3. Distribute encrypted group key via GROUP_KEY_DISTRIBUTION packets
4. All members use shared group key for group messages
5. Rotate group key when membership changes

**Trade-offs:**
- Simple implementation
- Compatible with server relay model
- No perfect forward secrecy for groups (acceptable for MVP)
- Future: Consider more advanced protocols (e.g., Double Ratchet for groups)

### File Transfer Security

**Chunking Strategy:**
```
Chunk size: 64KB (configurable)
Each chunk encrypted independently with:
- Chunk sequence number
- File ID
- Chunk index
- Per-chunk nonce
```

**File Transfer Protocol:**
1. Sender sends FILE_TRANSFER_START with metadata (encrypted):
   - File ID (UUID)
   - Filename (encrypted)
   - Total size
   - Number of chunks
   - File hash (SHA-256) for integrity
2. Sender sends ENCRYPTED_FILE_CHUNK packets
3. Receiver sends FILE_TRANSFER_ACK for each chunk
4. Receiver verifies hash after reassembly
5. Retry lost chunks (timeout-based)

**Integrity:**
- Per-chunk authentication (GCM tag)
- Overall file hash verification
- Abort transfer on any integrity failure
