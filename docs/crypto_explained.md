# Cryptography Explained 🔐

**TChatsApp Security Guide**

This document explains the cryptography used in TChatsApp in simple terms, without requiring any prior knowledge of security or encryption.

---

## Table of Contents

1. [The Core Problem: Secret Communication](#the-core-problem-secret-communication)
2. [Part 1: Symmetric Encryption (The Shared Secret Box)](#part-1-symmetric-encryption-the-shared-secret-box)
3. [Part 2: The Key Exchange Problem](#part-2-the-key-exchange-problem)
4. [Part 3: ECDH - The Magic Solution](#part-3-ecdh---the-magic-solution)
5. [Part 4: Putting It All Together](#part-4-putting-it-all-together)
6. [Part 5: Protection Against Attacks](#part-5-protection-against-attacks)
7. [Visual Summary](#visual-summary)
8. [Real-World Example](#real-world-example)
9. [Key Takeaways](#key-takeaways)
10. [Glossary](#glossary)

---

## The Core Problem: Secret Communication

**Goal:** Alice wants to send a message to Bob through a server, but the server shouldn't be able to read it.

Think of it like sending a letter through a mail carrier - you want the carrier to deliver it, but not read your private message.

**Why does this matter?**
- The server might be compromised
- The server might log everything
- We don't want to trust the server with our private conversations

**The Solution:** End-to-End Encryption (E2EE)
- Only Alice and Bob can read the messages
- The server just delivers encrypted data (gibberish)

---

## Part 1: Symmetric Encryption (The Shared Secret Box)

### What is AES-256-GCM?

**Analogy:** Think of a lockbox with a key.

- **AES-256** = A really strong lockbox
- **The Key** = A secret password (256 bits = super long and secure)
- **Encryption** = Putting your message in the box and locking it
- **Decryption** = Opening the box with the same key

### How it works:

```
Alice has message: "Hello Bob!"
Alice has key: "supersecret123..."

Step 1: Lock message
   Alice locks message in box → Encrypted: "x7f@9#k2..."

Step 2: Send locked box through server
   ↓ (sends locked box through server)
   Server sees: "x7f@9#k2..." (complete gibberish!)

Step 3: Bob receives and unlocks
   Bob receives locked box
   Bob has same key: "supersecret123..."
   Bob unlocks box → Gets message: "Hello Bob!"
```

**The server only sees `"x7f@9#k2..."` - complete gibberish!**

### What is GCM? (The Tamper-Proof Seal)

**Analogy:** Like a wax seal on an old letter.

Regular encryption is like putting a letter in an envelope. Someone could:
- Open it, read it, change it, and seal it back up

**GCM adds authentication** = It's like a tamper-proof seal that:
1. Shows if anyone opened the envelope
2. Proves it came from the real sender
3. If even 1 bit is changed, decryption fails

**Example:**

```
Alice encrypts: "Hello" → "x7f9k2" + seal "abc123"

Eve (hacker) intercepts and changes it:
   "x7f9k2" → "y8g0l3" + seal "abc123"

Bob tries to decrypt:
   ❌ REJECTED! Seal doesn't match anymore
   Bob knows: "This message was tampered with!"
```

**Without GCM:** Bob would get a corrupted or modified message and wouldn't know

**With GCM:** Bob immediately knows the message was tampered with and rejects it

---

## Part 2: The Key Exchange Problem

### The Big Problem

If Alice and Bob both need the same key, how do they share it without the server seeing it?

**Bad Solution #1: Send key in plain text**

```
Alice → Server: "Hey Bob, our key is 'supersecret123'"
Server: "Hehe, I can read this! 😈"
Bob ← Server: "Hey Alice, our key is 'supersecret123'"

Result: Server knows the key → Can decrypt all messages!
```

**Bad Solution #2: Meet in person**

```
Alice and Bob meet at a coffee shop
Alice: "Let's use 'supersecret123' as our key"
Bob: "OK!"

Result: Works, but not practical for internet communication!
```

### The Challenge

- They can't meet in person to exchange the key
- All communication goes through an untrusted server
- The server will see anything they send
- But they need the SAME key on both sides!

**This is like:** Two people in different cities trying to agree on a secret password while talking through a phone line that someone might be listening to!

**How is this even possible?!** 🤔

---

## Part 3: ECDH - The Magic Solution

**ECDH** = Elliptic Curve Diffie-Hellman

This is mathematical magic that solves the "impossible" problem!

### How ECDH Works (The Paint Mixing Analogy)

This is a famous analogy that makes ECDH really clear:

#### Setup

1. Alice and Bob publicly agree on a "common color" (e.g., **yellow**)
2. Server can see this - that's fine!

#### The Magic

**Alice's side:**

1. Alice picks a **secret color: RED** (keeps it private, never shares!)
2. Alice mixes: **YELLOW + RED = ORANGE**
3. Alice sends **ORANGE** to Bob
   - Server sees this, but **can't "unmix" it** to find RED

**Bob's side:**

1. Bob picks a **secret color: BLUE** (keeps it private, never shares!)
2. Bob mixes: **YELLOW + BLUE = GREEN**
3. Bob sends **GREEN** to Alice
   - Server sees this, but **can't "unmix" it** to find BLUE

#### The Final Mix

- **Alice** receives GREEN, mixes with her secret RED:
  - GREEN + RED = (YELLOW+BLUE) + RED = **YELLOW+RED+BLUE**

- **Bob** receives ORANGE, mixes with his secret BLUE:
  - ORANGE + BLUE = (YELLOW+RED) + BLUE = **YELLOW+RED+BLUE**

**Result:** Both get the **same final color** (YELLOW+RED+BLUE)!

```
Alice: YELLOW + RED + BLUE = Final Color
Bob:   YELLOW + BLUE + RED = Final Color (same!)

Server: ???
  - Knows: YELLOW (public)
  - Saw: ORANGE (YELLOW+RED, but can't extract RED)
  - Saw: GREEN (YELLOW+BLUE, but can't extract BLUE)
  - Can't figure out: YELLOW+RED+BLUE (needs both RED and BLUE!)
```

### In Real Crypto Terms

Instead of paint colors, we use mathematical operations on elliptic curves:

```
1. Alice generates:
   - Private key: a_secret (random number, kept secret)
   - Public key: A_public (calculated from a_secret using math)
   - Sends A_public to Bob

2. Bob generates:
   - Private key: b_secret (random number, kept secret)
   - Public key: B_public (calculated from b_secret using math)
   - Sends B_public to Alice

3. Alice computes: shared_secret = ECDH(a_secret, B_public)
4. Bob computes:   shared_secret = ECDH(b_secret, A_public)

Both get the SAME shared_secret!
```

**Why it's secure:**

- Even if the server sees `A_public` and `B_public`, it can't compute `shared_secret`
- It's mathematically hard (like unmixing paint colors)
- This is called the "Elliptic Curve Discrete Logarithm Problem" - super hard to solve!

**Why we use Curve25519:**

- It's a specific "mixing formula" (elliptic curve) that's very fast and secure
- Industry standard, proven safe
- Used by: Signal, WhatsApp, TLS 1.3, SSH, etc.

---

## Part 4: Putting It All Together

Now let's see how ECDH and AES-GCM work together in our chat app!

### Step-by-Step Message Flow

#### Phase 1: Initial Setup (Key Exchange)

This happens **once** when Alice first wants to chat with Bob:

```
Alice                          Server                          Bob
  |                              |                              |
  | "Hey Bob, here's A_public"   |                              |
  |----------------------------->|----------------------------->|
  |                              |                              |
  |                              |  "Hey Alice, here's B_public"|
  |<-----------------------------|<-----------------------------|
  |                              |                              |

Alice computes:                                    Bob computes:
shared = ECDH(a_secret, B_public)                  shared = ECDH(b_secret, A_public)
session_key = derive(shared)                       session_key = derive(shared)

Both have the same session_key now! 🎉
```

**What the server saw:**
- `A_public` (useless without `a_secret`)
- `B_public` (useless without `b_secret`)
- Cannot compute `shared_secret` or `session_key`

#### Phase 2: Sending Encrypted Messages

This happens **every time** Alice sends a message:

```
Alice wants to send: "Meet at 3pm"

Alice's side:
  1. Generate random nonce (like a unique message ID)
  2. Sequence number: seq = 1 (first message)
  3. Encrypt: ciphertext = AES-GCM("Meet at 3pm", session_key, nonce)
  4. Send to server: [seq:1, nonce:xyz, ciphertext:@#$%, auth_tag:abc]

Server's side:
  - Receives: [1, xyz, @#$%, abc]
  - Sees: Random bytes and gibberish
  - Can't decrypt (doesn't have session_key)
  - Just forwards to Bob

Bob's side:
  1. Receives: [seq:1, nonce:xyz, ciphertext:@#$%, auth_tag:abc]
  2. Check sequence: Is seq=1 > last_seq=0? Yes, accept
  3. Decrypt: plaintext = AES-GCM-Decrypt(ciphertext, session_key, nonce)
  4. Verify auth_tag: Does it match? Yes ✅
  5. Read message: "Meet at 3pm"
  6. Update last_seq = 1
```

**Security achieved:**
- ✅ Confidential: Server can't read "Meet at 3pm"
- ✅ Authentic: Bob knows it's really from Alice (GCM tag)
- ✅ Not tampered: Any change would break GCM tag
- ✅ Not replayed: Sequence number prevents reuse

---

## Part 5: Protection Against Attacks

Let's see how our crypto protects against real attacks!

### Attack 1: Eavesdropping (Passive Listener)

**Scenario:** Server (or hacker) tries to read messages

```
Alice → Server: [seq:5, nonce:xyz, ciphertext:"x7f@9#k2...", tag:abc]
                     ↓
Server tries to decrypt: "Give me the plaintext!"
  - Needs: session_key
  - Has: Only A_public and B_public
  - Can compute session_key? NO! (ECDH is secure)
  - Can decrypt? NO! (Doesn't have the key)
  - Sees: Just random bytes
```

**Protection:**
- Messages encrypted with AES-256
- Server can't get session_key (ECDH protects it)

**Result:** ✅ BLOCKED

---

### Attack 2: Tampering (Active Attacker)

**Scenario:** Server tries to change message content

```
Original message from Alice:
  plaintext: "Transfer $10 to Bob"
  encrypted: [nonce:xyz, ciphertext:AAAA, tag:1234]

Server's evil plan:
  1. Change ciphertext: AAAA → BBBB
  2. Hope it decrypts to: "Transfer $1000 to Server"
  3. Forward to Bob: [nonce:xyz, ciphertext:BBBB, tag:1234]

Bob receives and tries to decrypt:
  1. Decrypt: ciphertext=BBBB with session_key
  2. Verify GCM tag=1234 against decrypted data
  3. GCM verification: ❌ FAIL! Tag doesn't match!
  4. Reject message, alert user: "Message tampered!"
```

**Why it fails:**
- GCM tag is calculated from: nonce + ciphertext + session_key
- Changing ciphertext breaks the tag
- Server can't create valid tag (doesn't have session_key)

**Protection:** GCM authentication tag

**Result:** ✅ BLOCKED

---

### Attack 3: Replay Attack

**Scenario:** Server captures a message and resends it later

```
Day 1, 10:00 AM:
  Alice → Bob: "Transfer $10 to charity" [seq: 1]
  Bob receives, decrypts: "Transfer $10 to charity"
  Bob's last_seq = 1
  Bob transfers $10 ✅

Day 1, 11:00 AM:
  Server replays same message:
  Server → Bob: "Transfer $10 to charity" [seq: 1]

Bob receives:
  1. Check: Is seq=1 > last_seq=1? NO!
  2. Reject: "This is an old message!"
  3. No transfer happens

Day 1, 2:00 PM:
  Alice → Bob: "Transfer $5 to charity" [seq: 2]
  Bob receives:
  1. Check: Is seq=2 > last_seq=1? YES!
  2. Accept and decrypt
  3. Update: last_seq = 2
  4. Transfer $5 ✅
```

**Protection:** Sequence numbers
- Each message has increasing number: 1, 2, 3, 4...
- Bob tracks highest sequence seen
- Old sequence numbers rejected

**Result:** ✅ BLOCKED

---

### Attack 4: Man-in-the-Middle (During Key Exchange)

**Scenario:** Server tries to intercept key exchange

```
Attempt 1: Substitute public keys
──────────────────────────────────
Alice sends: A_public
  ↓
Server intercepts, tries to send: Server_public (pretending to be Bob)

Problem: Bob also sends B_public
  ↓
Alice receives B_public (real)
Bob receives A_public (real)

Server can't be in the middle of both sides!

Attempt 2: Separate connections
────────────────────────────────
Server tries:
  - Alice ↔ Server (Server pretends to be Bob)
  - Server ↔ Bob (Server pretends to be Alice)

Problem: In our design, this is possible! 😱

Solution (Future Enhancement):
  - Key verification: Alice and Bob compare key fingerprints
  - Out-of-band: Via QR code, phone call, etc.
  - Public Key Infrastructure (PKI): Trust chain
```

**Current Status:**
- ⚠️ Vulnerable to active MITM during key exchange
- ✅ Protected after key exchange established
- 📋 Future: Add key verification feature

**Protection for MVP:**
- Trust-On-First-Use (TOFU): First exchange assumed safe
- Future: Manual key verification

---

## Visual Summary

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    SECURE MESSAGING FLOW                     │
└─────────────────────────────────────────────────────────────┘

PHASE 1: KEY EXCHANGE (once per conversation)
══════════════════════════════════════════════════════════════

Alice                  Server                  Bob
  │                      │                      │
  │  A_public            │                      │
  ├────────────────────->├─────────────────────>│
  │                      │        B_public      │
  │<─────────────────────┤<─────────────────────┤
  │                      │                      │
 [Computes]            [Sees public keys]    [Computes]
shared_secret          but can't derive      shared_secret
     ↓                 shared_secret!              ↓
session_key                                   session_key
  (same!)                                       (same!)


PHASE 2: ENCRYPTED MESSAGING (every message)
══════════════════════════════════════════════════════════════

Alice: "Hi Bob!"
  ↓
[Encrypt with session_key + GCM]
  ↓
[seq:1, nonce:xyz, ciphertext:@#$%, tag:abc]
  │
  ├──────────────────>├─────────────────────>│
                      │                      │
Server sees:          Just forwards:         Bob receives:
Random bytes!         Random bytes!             ↓
Can't read it!                            [Decrypt with
                                          session_key + GCM]
                                                ↓
                                             [Verify tag]
                                                ↓
                                             [Check seq]
                                                ↓
                                             "Hi Bob!" ✅
```

### Encryption Layers

```
    Original Message: "Hello Bob!"
                ↓
┌──────────────────────────────────────┐
│ Layer 1: Add sequence number         │
│ [seq:1] + "Hello Bob!"               │
└──────────────────────────────────────┘
                ↓
┌──────────────────────────────────────┐
│ Layer 2: AES-GCM Encryption          │
│ Encrypted: "x7f@9#k2..."             │
│ Tag: "abc123..."                     │
└──────────────────────────────────────┘
                ↓
┌──────────────────────────────────────┐
│ Layer 3: Add nonce                   │
│ [nonce:xyz] + ciphertext + tag       │
└──────────────────────────────────────┘
                ↓
┌──────────────────────────────────────┐
│ Layer 4: Packet wrapper              │
│ [from:Alice, to:Bob, payload:...]    │
└──────────────────────────────────────┘
                ↓
          Sent to server
```

---

## Real-World Example

Let's walk through a complete scenario: Alice sends Bob a picture.

### Scenario: Sending `vacation.jpg` (2 MB)

#### Step 1: Initial Key Exchange (First time only)

```
Alice opens chat with Bob
  ↓
App: "No session key found, initiating key exchange..."
  ↓
Alice generates keypair: (a_secret, A_public)
Bob generates keypair: (b_secret, B_public)
  ↓
Exchange public keys via server
  ↓
Both compute: session_key = ECDH(...)
  ↓
App: "Secure connection established! 🔒"
```

**Server saw:** Two random-looking numbers (public keys)

**Server knows:** Nothing useful

---

#### Step 2: Split File into Chunks

```
vacation.jpg = 2 MB = 2,048,000 bytes
Chunk size = 64 KB = 65,536 bytes

Number of chunks = 2,048,000 / 65,536 = 32 chunks

Chunks:
  [0]: bytes 0-65535
  [1]: bytes 65536-131071
  [2]: bytes 131072-196607
  ...
  [31]: bytes 2031616-2047999
```

---

#### Step 3: Send File Metadata

```
Alice creates metadata message:
  {
    file_id: "uuid-1234-5678",
    filename: "vacation.jpg",
    total_size: 2048000,
    num_chunks: 32,
    file_hash: "sha256:abcd1234..."  (hash of entire file)
  }
  ↓
Serialize to JSON: "{...}"
  ↓
Encrypt with session_key:
  type: FILE_TRANSFER_START
  seq: 5
  nonce: random()
  ciphertext: AES-GCM("{...}", session_key, nonce)
  ↓
Send to server → Forward to Bob
  ↓
Bob decrypts, sees:
  "Alice is sending vacation.jpg, 2MB, 32 chunks"
  ↓
Bob: "Accept file? [Yes] [No]"
```

---

#### Step 4: Send Each Chunk (32 times)

For each chunk (e.g., chunk #5):

```
Alice:
  1. Read chunk data: chunk_5 = read(vacation.jpg, offset=327680, size=65536)
  2. Create chunk message:
     {
       file_id: "uuid-1234-5678",
       chunk_index: 5,
       chunk_data: <65536 bytes>
     }
  3. Encrypt:
     type: ENCRYPTED_FILE_CHUNK
     seq: 10 (message sequence, not chunk index)
     nonce: random()
     ciphertext: AES-GCM({...}, session_key, nonce)
  4. Send encrypted chunk → Server → Bob

Server:
  - Receives encrypted chunk
  - Sees: Random bytes, ~65 KB
  - Forwards to Bob
  - Has NO IDEA it's part of an image!

Bob:
  1. Receives encrypted chunk
  2. Checks sequence: seq=10 > last_seq=9 ✅
  3. Decrypts: AES-GCM-Decrypt(...)
  4. Verifies GCM tag ✅
  5. Extracts: chunk_index=5, chunk_data
  6. Stores: chunks[5] = chunk_data
  7. Updates: last_seq = 10
  8. UI: "Receiving vacation.jpg... 5/32 chunks (15%)"
```

Repeat for all 32 chunks...

---

#### Step 5: Reassemble and Verify

```
Bob (after receiving all 32 chunks):
  1. Reassemble file:
     vacation.jpg = chunks[0] + chunks[1] + ... + chunks[31]

  2. Compute hash:
     received_hash = SHA256(vacation.jpg)
     expected_hash = "sha256:abcd1234..." (from metadata)

  3. Verify integrity:
     if received_hash == expected_hash:
       ✅ "File received successfully!"
       Save to disk
       Show in chat
     else:
       ❌ "File corrupted! Hash mismatch!"
       Request re-send
```

---

#### What the Server Saw

```
Server's view of the entire transaction:
───────────────────────────────────────────

1. Random encrypted message (metadata)
   - Size: ~500 bytes
   - Content: gibberish

2. Random encrypted message (chunk 0)
   - Size: ~65 KB
   - Content: gibberish

3. Random encrypted message (chunk 1)
   - Size: ~65 KB
   - Content: gibberish

... (30 more similar messages)

34. Random encrypted message (chunk 31)
    - Size: ~65 KB
    - Content: gibberish

Server knows:
  - Alice sent 33 messages to Bob
  - Total size: ~2 MB

Server DOESN'T know:
  - That it's a file transfer
  - The filename
  - The file type (image)
  - The file content
  - Anything about the image!
```

---

#### Security Summary for This Transfer

**Confidentiality:** ✅
- Image encrypted chunk-by-chunk
- Server sees only random bytes
- Only Bob can decrypt with session_key

**Integrity:** ✅
- Each chunk: GCM tag prevents tampering
- Entire file: SHA-256 hash verification
- If 1 byte is wrong, Bob detects it

**Authenticity:** ✅
- GCM proves each chunk from Alice
- Bob knows file is really from Alice

**No Replay:** ✅
- Sequence numbers on each chunk
- Server can't re-send old chunks

**Resume Support:** ✅ (future)
- Bob tracks which chunks received
- Can request missing chunks
- If transfer interrupted, resume from where it stopped

---

## Key Takeaways

### The Big Picture

1. **AES-GCM** = Lock box with tamper-proof seal
   - Keeps message secret (confidentiality)
   - Detects tampering (authenticity)
   - Fast and efficient

2. **ECDH** = Magic key agreement
   - Both sides get same key without sharing it directly
   - Server can't figure out the key
   - Based on hard math problem

3. **Sequence Numbers** = Prevents replay attacks
   - Each message numbered: 1, 2, 3...
   - Old messages rejected
   - Ensures ordering

4. **Result:**
   - ✅ Server can't read messages
   - ✅ Server can't change messages
   - ✅ Server can't replay old messages
   - ✅ Only Alice and Bob can communicate

### What Makes This Secure?

**Strong Crypto Primitives:**
- AES-256: Industry standard, unbroken
- Curve25519: Modern, fast, secure
- GCM: Authenticated encryption
- SHA-256: Collision-resistant hashing

**Proper Usage:**
- Unique nonces (never reused)
- Random key generation
- Sequence numbers for replay protection
- Authentication tags verified

**Defense in Depth:**
- Encryption (confidentiality)
- Authentication (integrity)
- Sequence checking (ordering)
- Hash verification (files)

### Limitations (Be Honest!)

**What we DON'T protect against:**

1. **Compromised endpoints**
   - If Alice's computer has malware → malware sees plaintext
   - If Bob's computer is hacked → hacker sees messages
   - E2EE only protects data in transit, not at endpoints

2. **Metadata**
   - Server knows: who talks to whom, when, how much data
   - Server doesn't know: what they say
   - Like: Server sees "Alice sent 50 messages to Bob" but not content

3. **Trust-On-First-Use**
   - First key exchange assumed safe
   - No protection against MITM during first connection
   - Solution: Manual key verification (future)

4. **Quantum computers (future)**
   - Current crypto vulnerable to quantum attacks
   - Timeline: 10-20+ years away
   - Solution: Post-quantum algorithms (when needed)

### Why This Approach?

**Industry Proven:**
- Used by: Signal, WhatsApp, iMessage
- Vetted by security researchers
- Open algorithms (no "security by obscurity")

**Practical Balance:**
- Strong security
- Good performance
- Reasonable complexity
- Well-documented

**Incremental Security:**
- MVP: Basic E2EE (this design)
- Phase 2: Key verification
- Phase 3: Forward secrecy (ratcheting)
- Phase 4: Post-quantum crypto

---

## Glossary

**AES (Advanced Encryption Standard)**
- Symmetric encryption algorithm
- Uses same key for encrypt and decrypt
- AES-256: Uses 256-bit keys (very strong)

**Authentication Tag**
- A signature proving message integrity
- Generated by GCM mode
- If message changed, tag won't match

**Ciphertext**
- Encrypted data (gibberish)
- Opposite of plaintext
- Example: "x7f@9#k2..." instead of "Hello"

**ECDH (Elliptic Curve Diffie-Hellman)**
- Key agreement protocol
- Lets two parties agree on shared secret
- Without actually sending the secret

**E2EE (End-to-End Encryption)**
- Encryption from sender to recipient
- Intermediaries can't decrypt
- Only endpoints can read plaintext

**GCM (Galois/Counter Mode)**
- Authenticated encryption mode for AES
- Provides encryption + authentication
- Detects tampering

**Hash**
- One-way function: data → fixed-size output
- Same input → same output
- Can't reverse: output → input
- Example: SHA-256

**HKDF (HMAC-based Key Derivation Function)**
- Derives keys from shared secret
- Expands short secret to full-length key
- Prevents related-key attacks

**Nonce**
- "Number used once"
- Unique value for each encryption
- Prevents pattern detection
- Must NEVER be reused with same key

**Plaintext**
- Original, unencrypted data
- Readable by humans
- Example: "Hello Bob!"

**Private Key**
- Secret half of a keypair
- Never shared, kept secret
- Used to decrypt or sign

**Public Key**
- Public half of a keypair
- Can be shared openly
- Used to encrypt or verify

**Replay Attack**
- Attacker records message
- Resends it later
- Prevented by sequence numbers

**Sequence Number**
- Increasing counter: 1, 2, 3...
- Each message has unique sequence
- Prevents replay attacks

**Session Key**
- Temporary key for a conversation
- Derived from ECDH shared secret
- Used for AES encryption

**Symmetric Encryption**
- Same key for encrypt and decrypt
- Fast and efficient
- Example: AES

**Tampering**
- Modifying message without authorization
- Detected by authentication tags
- Causes decryption failure

---

## Further Reading

### If You Want to Learn More

**Beginner-Friendly:**
- "Crypto 101" by Laurens Van Houtven (free ebook)
- "Serious Cryptography" by Jean-Philippe Aumasson
- YouTube: Computerphile channel (crypto videos)

**Technical:**
- "Applied Cryptography" by Bruce Schneier
- "Cryptography Engineering" by Ferguson, Schneier, Kohno
- NIST Guidelines: https://csrc.nist.gov/

**Our Implementation:**
- Design Document: `design/analysis.md`
- Developer Guide: `README-dev.md`
- Source Code: `src/main/java/.../crypto/`

### Questions?

If anything is still unclear:
1. Read the code comments in `crypto/` package
2. Check `design/analysis.md` for technical details
3. Draw diagrams yourself - teaching aid!
4. Ask your instructor or teammates

---

**Document Version:** 1.0
**Last Updated:** 2025-11-08
**Author:** TChatsApp Team
**License:** GPL-3.0 (same as project)

---

*Remember: Cryptography is hard. We use proven, standard algorithms and follow best practices. When in doubt, consult experts and don't roll your own crypto!*

🔒 **Stay Secure!** 🔒