# TChatsApp - Developer Documentation

## Project Structure

```
TChatsApp/
|
├── src/
│   ├── main/java/fr/uga/im2ag/m1info/chatservice/
│   │   ├── client/                  # Client-side code
│   │   │   ├── Client.java          # Main client class
│   │   │   └── gui/                 # JavaFX GUI components (to be implemented)
│   │   ├── server/                  # Server-side code
│   │   │   ├── TchatsAppServer.java # NIO-based relay server
│   │   │   └── IdGenerator.java     # Client ID generation
│   │   ├── common/                  # Shared protocol classes
│   │   │   ├── Packet.java          # Base packet structure
│   │   │   └── PacketProcessor.java # Packet processing interface
│   │   ├── crypto/                  # Cryptographic operations (NEW)
│   │   │   ├── KeyExchange.java     # ECDH key agreement
│   │   │   ├── SymmetricCipher.java # AES-GCM encryption
│   │   │   └── SessionKeyManager.java # Session management
│   │   ├── protocol/                # Enhanced protocol types (NEW)
│   │   │   ├── MessageType.java     # Message type enum
│   │   │   └── EncryptedMessage.java # Encrypted payload format
│   │   ├── storage/                 # Persistent storage (NEW)
│   │   │   └── KeyStore.java        # Encrypted key storage
│   │   └── attachments/             # File transfer (NEW)
│   │       └── FileChunker.java     # File chunking utilities
│   └── test/java/                   # Unit and integration tests
└── pom.xml                          # Maven configuration
```

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- Internet connection (for downloading dependencies)

## Building the Project

### Clean Build

```bash
mvn clean package
```

This will:
1. Download all dependencies (BouncyCastle, JavaFX, etc.)
2. Compile source code
3. Run unit tests
4. Create JAR file in `target/`

### Build Without Tests

```bash
mvn clean package -DskipTests
```

### Run Tests Only

```bash
mvn test
```

## Running the Application

### Start the Server

```bash
mvn exec:java -Dexec.mainClass="fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer"
```

The server will start on port **1666** by default.

**Note:** If you get a `BindException`, another server is already running on that port.

### Start a Client (CLI)

In a separate terminal:

```bash
mvn exec:java -Dexec.mainClass="fr.uga.im2ag.m1info.chatservice.client.Client"
```

The CLI will prompt for:
1. Your client ID (use `0` to create a new client)
2. Recipient ID
3. Message text

### Start Multiple Clients

Open multiple terminals and run the client command in each. The server will assign unique IDs.


### Module Responsibilities

| Module | Responsibility | Status |
|--------|---------------|--------|
| `crypto/` | Key exchange, encryption, session management | Skeleton only |
| `protocol/` | Message types and serialization | Skeleton only |
| `storage/` | Persistent key and message storage | Skeleton only |
| `attachments/` | File chunking and transfer | Skeleton only |
| `client/` | Client logic and GUI | Basic CLI exists |
| `server/` | Relay server | Fully implemented |
| `common/` | Shared packet protocol | Fully implemented |

## Testing Strategy

### Unit Tests

Located in `src/test/java/` mirroring the main package structure.

**Run specific test class:**
```bash
mvn test -Dtest=KeyExchangeTest
```

**Run tests for a package:**
```bash
mvn test -Dtest="fr.uga.im2ag.m1info.chatservice.crypto.*"
```

### Integration Tests

Integration tests will simulate multiple clients and server interactions.

**Naming convention:** `*IntegrationTest.java`


## Troubleshooting

### Build Errors

**Problem:** `Failed to execute goal [...] compiler`
- **Solution:** Verify Java 17 is installed: `java -version`

**Problem:** Dependencies not downloading
- **Solution:** Check internet connection, try `mvn clean install -U`

### Runtime Errors

**Problem:** `BindException: Address already in use`
- **Solution:** Another server is running. Kill it or change port.

**Problem:** `ClassNotFoundException`
- **Solution:** Rebuild: `mvn clean package`

**Problem:** `NoSuchAlgorithmException` for crypto
- **Solution:** Ensure BouncyCastle is in classpath (check pom.xml)

### Testing Errors

**Problem:** Tests fail with crypto errors
- **Solution:** Check BouncyCastle provider is registered:
  ```java
  Security.addProvider(new BouncyCastleProvider());
  ```

## Useful Maven Commands

| Command | Description |
|---------|-------------|
| `mvn clean` | Delete build artifacts |
| `mvn compile` | Compile source code only |
| `mvn test` | Run all tests |
| `mvn package` | Build JAR file |
| `mvn dependency:tree` | Show dependency tree |
| `mvn dependency:resolve` | Download all dependencies |
| `mvn exec:java -Dexec.mainClass="..."` | Run a main class |

## Resources

### Documentation
- **Design Document:** `design/analysis.md` - Complete architecture and implementation plan
- **Project Spec:** See course page (INRIA)
- **JavaDoc:** Generate with `mvn javadoc:javadoc` (output in `target/site/apidocs/`)

### Libraries
- **BouncyCastle Docs:** https://www.bouncycastle.org/java.html
- **JavaFX Docs:** https://openjfx.io/javadoc/21/
- **JUnit 5 Guide:** https://junit.org/junit5/docs/current/user-guide/

### Crypto Resources
- **NIST Guidelines:** https://csrc.nist.gov/publications
- **OWASP Crypto Cheat Sheet:** https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html

## Git Workflow

### Commit Messages

Format:
```
[module] Short description

Longer explanation if needed.
- Bullet points for details
```

Example:
```
[crypto] Implement ECDH key exchange

- Add Curve25519 key generation
- Add shared secret derivation
- Add HKDF key derivation
- Include unit tests for all operations
```

### Before Committing

1. Run tests: `mvn test`
2. Check code compiles: `mvn compile`
3. Review your changes: `git diff`
4. Stage files: `git add <files>`
5. Commit: `git commit -m "message"`


**Last Updated:** 2025-11-08