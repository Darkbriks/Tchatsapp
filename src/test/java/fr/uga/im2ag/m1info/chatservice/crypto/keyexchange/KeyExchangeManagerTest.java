package fr.uga.im2ag.m1info.chatservice.crypto.keyexchange;

import fr.uga.im2ag.m1info.chatservice.crypto.KeyExchange;
import fr.uga.im2ag.m1info.chatservice.crypto.SessionKeyManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KeyExchangeManager}.
 * <p>
 * Tests cover:
 * <ul>
 *   <li>Basic key exchange flow (initiator and responder)</li>
 *   <li>Session key derivation and storage</li>
 *   <li>Error handling and edge cases</li>
 *   <li>Concurrent operations</li>
 *   <li>Timeout and expiration</li>
 * </ul>
 */
@DisplayName("KeyExchangeManager Tests")
class KeyExchangeManagerTest {
    
    private static final int ALICE_ID = 1;
    private static final int BOB_ID = 2;
    
    private KeyExchangeManager aliceManager;
    private KeyExchangeManager bobManager;
    private SessionKeyManager aliceSessionManager;
    private SessionKeyManager bobSessionManager;
    private KeyExchange keyExchange;
    
    // Message transport simulation
    private AtomicReference<KeyExchangeManager.KeyExchangeMessageData> aliceOutgoing;
    private AtomicReference<KeyExchangeManager.KeyExchangeMessageData> bobOutgoing;
    
    @BeforeEach
    void setUp() {
        keyExchange = new KeyExchange();
        aliceSessionManager = new SessionKeyManager();
        bobSessionManager = new SessionKeyManager();
        
        aliceOutgoing = new AtomicReference<>();
        bobOutgoing = new AtomicReference<>();
        
        aliceManager = new KeyExchangeManager(ALICE_ID, aliceSessionManager, null);
        bobManager = new KeyExchangeManager(BOB_ID, bobSessionManager, null);
        
        // Configure message senders
        aliceManager.setMessageSender(aliceOutgoing::set);
        bobManager.setMessageSender(bobOutgoing::set);
        
        // Start managers
        aliceManager.start();
        bobManager.start();
    }
    
    @AfterEach
    void tearDown() {
        aliceManager.shutdown();
        bobManager.shutdown();
    }
    
    // ========================= Basic Flow Tests =========================
    
    @Test
    @DisplayName("Complete key exchange flow should establish session")
    void testCompleteKeyExchangeFlow() throws Exception {
        // Alice initiates
        aliceManager.initiateKeyExchange(BOB_ID);
        
        // Verify Alice sent KEY_EXCHANGE
        KeyExchangeManager.KeyExchangeMessageData aliceMsg = aliceOutgoing.get();
        assertNotNull(aliceMsg, "Alice should have sent a message");
        assertEquals(ALICE_ID, aliceMsg.getFromId());
        assertEquals(BOB_ID, aliceMsg.getToId());
        assertFalse(aliceMsg.isResponse());
        assertTrue(aliceMsg.getPublicKey().length > 0);
        
        // Simulate network: Bob receives Alice's KEY_EXCHANGE
        bobManager.handleKeyExchangeRequest(ALICE_ID, aliceMsg.getPublicKey());
        
        // Verify Bob sent KEY_EXCHANGE_RESPONSE
        KeyExchangeManager.KeyExchangeMessageData bobMsg = bobOutgoing.get();
        assertNotNull(bobMsg, "Bob should have sent a response");
        assertEquals(BOB_ID, bobMsg.getFromId());
        assertEquals(ALICE_ID, bobMsg.getToId());
        assertTrue(bobMsg.isResponse());
        
        // Verify Bob now has a session
        assertTrue(bobManager.hasSessionWith(ALICE_ID), "Bob should have session with Alice");
        assertNotNull(bobManager.getSessionKey(ALICE_ID), "Bob should have session key");
        
        // Simulate network: Alice receives Bob's KEY_EXCHANGE_RESPONSE
        aliceManager.handleKeyExchangeResponse(BOB_ID, bobMsg.getPublicKey());
        
        // Verify Alice now has a session
        assertTrue(aliceManager.hasSessionWith(BOB_ID), "Alice should have session with Bob");
        assertNotNull(aliceManager.getSessionKey(BOB_ID), "Alice should have session key");
        
        // Verify both derived the same key
        SecretKey aliceKey = aliceManager.getSessionKey(BOB_ID);
        SecretKey bobKey = bobManager.getSessionKey(ALICE_ID);
        assertArrayEquals(aliceKey.getEncoded(), bobKey.getEncoded(), 
            "Alice and Bob should have the same session key");
    }
    
    @Test
    @DisplayName("Conversation ID should be symmetric")
    void testConversationIdSymmetry() {
        String aliceConvId = aliceManager.createConversationId(BOB_ID);
        String bobConvId = bobManager.createConversationId(ALICE_ID);
        
        assertEquals(aliceConvId, bobConvId, 
            "Conversation IDs should be the same regardless of who creates them");
        assertEquals("1_2", aliceConvId, "Conversation ID format should be min_max");
    }
    
    @Test
    @DisplayName("Listener should be notified on completion")
    void testListenerNotification() throws Exception {
        CountDownLatch aliceLatch = new CountDownLatch(1);
        CountDownLatch bobLatch = new CountDownLatch(1);
        AtomicReference<SecretKey> aliceReceivedKey = new AtomicReference<>();
        AtomicReference<SecretKey> bobReceivedKey = new AtomicReference<>();
        
        aliceManager.addListener(new TestListener() {
            @Override
            public void onKeyExchangeCompleted(int peerId, SecretKey sessionKey) {
                aliceReceivedKey.set(sessionKey);
                aliceLatch.countDown();
            }
        });
        
        bobManager.addListener(new TestListener() {
            @Override
            public void onKeyExchangeCompleted(int peerId, SecretKey sessionKey) {
                bobReceivedKey.set(sessionKey);
                bobLatch.countDown();
            }
        });
        
        // Perform exchange
        aliceManager.initiateKeyExchange(BOB_ID);
        bobManager.handleKeyExchangeRequest(ALICE_ID, aliceOutgoing.get().getPublicKey());
        aliceManager.handleKeyExchangeResponse(BOB_ID, bobOutgoing.get().getPublicKey());
        
        // Wait for notifications
        assertTrue(bobLatch.await(1, TimeUnit.SECONDS), "Bob's listener should be called");
        assertTrue(aliceLatch.await(1, TimeUnit.SECONDS), "Alice's listener should be called");
        
        assertNotNull(aliceReceivedKey.get(), "Alice should receive key in listener");
        assertNotNull(bobReceivedKey.get(), "Bob should receive key in listener");
    }
    
    // ========================= Error Handling Tests =========================
    
    @Test
    @DisplayName("Initiating with invalid peer ID should throw")
    void testInvalidPeerIdThrows() {
        assertThrows(KeyExchangeException.class, () -> 
            aliceManager.initiateKeyExchange(0), "Peer ID 0 should be rejected");
        
        assertThrows(KeyExchangeException.class, () -> 
            aliceManager.initiateKeyExchange(-1), "Negative peer ID should be rejected");
        
        assertThrows(KeyExchangeException.class, () -> 
            aliceManager.initiateKeyExchange(ALICE_ID), "Self peer ID should be rejected");
    }
    
    @Test
    @DisplayName("Double initiation should throw")
    void testDoubleInitiationThrows() throws Exception {
        aliceManager.initiateKeyExchange(BOB_ID);
        
        KeyExchangeException exception = assertThrows(
            KeyExchangeException.class,
            () -> aliceManager.initiateKeyExchange(BOB_ID),
            "Second initiation should throw"
        );
        
        assertEquals(KeyExchangeException.ErrorCode.EXCHANGE_ALREADY_IN_PROGRESS, 
            exception.getErrorCode());
    }
    
    @Test
    @DisplayName("Response without pending exchange should throw")
    void testResponseWithoutPendingThrows() throws Exception {
        // Generate a valid public key
        KeyPair keyPair = keyExchange.generateKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        
        KeyExchangeException exception = assertThrows(
            KeyExchangeException.class,
            () -> aliceManager.handleKeyExchangeResponse(BOB_ID, publicKey),
            "Response without pending should throw"
        );
        
        assertEquals(KeyExchangeException.ErrorCode.NO_PENDING_EXCHANGE, 
            exception.getErrorCode());
    }
    
    @Test
    @DisplayName("Invalid public key should throw")
    void testInvalidPublicKeyThrows() {
        byte[] invalidKey = new byte[]{1, 2, 3, 4, 5}; // Invalid X509 format
        
        assertThrows(KeyExchangeException.class, () ->
            aliceManager.handleKeyExchangeRequest(BOB_ID, invalidKey),
            "Invalid public key should throw"
        );
    }
    
    @Test
    @DisplayName("Operations before start should throw")
    void testOperationsBeforeStartThrow() {
        KeyExchangeManager manager = new KeyExchangeManager(100, new SessionKeyManager());
        // Don't call start()
        
        assertThrows(KeyExchangeException.class, () ->
            manager.initiateKeyExchange(200),
            "Operations before start should throw"
        );
    }
    
    @Test
    @DisplayName("Exchange after session exists should throw")
    void testExchangeAfterSessionThrows() throws Exception {
        // Complete an exchange first
        aliceManager.initiateKeyExchange(BOB_ID);
        bobManager.handleKeyExchangeRequest(ALICE_ID, aliceOutgoing.get().getPublicKey());
        aliceManager.handleKeyExchangeResponse(BOB_ID, bobOutgoing.get().getPublicKey());
        
        assertTrue(aliceManager.hasSessionWith(BOB_ID));
        
        // Try to initiate again
        KeyExchangeException exception = assertThrows(
            KeyExchangeException.class,
            () -> aliceManager.initiateKeyExchange(BOB_ID),
            "Exchange after session exists should throw"
        );
        
        assertEquals(KeyExchangeException.ErrorCode.SESSION_ALREADY_EXISTS, 
            exception.getErrorCode());
    }
    
    // ========================= Session Management Tests =========================
    
    @Test
    @DisplayName("ensureSessionExists should be idempotent")
    void testEnsureSessionExistsIdempotent() throws Exception {
        // First call initiates
        aliceManager.ensureSessionExists(BOB_ID);
        assertTrue(aliceManager.isExchangePending(BOB_ID));
        
        // Second call should not throw (idempotent)
        assertDoesNotThrow(() -> aliceManager.ensureSessionExists(BOB_ID));
        
        // Complete the exchange
        bobManager.handleKeyExchangeRequest(ALICE_ID, aliceOutgoing.get().getPublicKey());
        aliceManager.handleKeyExchangeResponse(BOB_ID, bobOutgoing.get().getPublicKey());
        
        // Now session exists, should still not throw
        assertTrue(aliceManager.hasSessionWith(BOB_ID));
        assertDoesNotThrow(() -> aliceManager.ensureSessionExists(BOB_ID));
    }
    
    @Test
    @DisplayName("Key rotation should replace session key")
    void testKeyRotation() throws Exception {
        // Complete initial exchange
        aliceManager.initiateKeyExchange(BOB_ID);
        bobManager.handleKeyExchangeRequest(ALICE_ID, aliceOutgoing.get().getPublicKey());
        aliceManager.handleKeyExchangeResponse(BOB_ID, bobOutgoing.get().getPublicKey());
        
        SecretKey originalKey = aliceManager.getSessionKey(BOB_ID);
        assertNotNull(originalKey);
        
        // Rotate key
        aliceManager.rotateKey(BOB_ID);
        
        // Alice should have initiated new exchange
        KeyExchangeManager.KeyExchangeMessageData rotationMsg = aliceOutgoing.get();
        assertNotNull(rotationMsg);
        assertFalse(rotationMsg.isResponse());
        
        // Complete new exchange
        bobManager.handleKeyExchangeRequest(ALICE_ID, rotationMsg.getPublicKey());
        aliceManager.handleKeyExchangeResponse(BOB_ID, bobOutgoing.get().getPublicKey());
        
        SecretKey newKey = aliceManager.getSessionKey(BOB_ID);
        assertNotNull(newKey);
        
        // Keys should be different (with extremely high probability)
        assertFalse(java.util.Arrays.equals(originalKey.getEncoded(), newKey.getEncoded()),
            "Rotated key should be different");
    }
    
    @Test
    @DisplayName("Invalidate session should remove key")
    void testInvalidateSession() throws Exception {
        // Complete exchange
        aliceManager.initiateKeyExchange(BOB_ID);
        bobManager.handleKeyExchangeRequest(ALICE_ID, aliceOutgoing.get().getPublicKey());
        aliceManager.handleKeyExchangeResponse(BOB_ID, bobOutgoing.get().getPublicKey());
        
        assertTrue(aliceManager.hasSessionWith(BOB_ID));
        
        // Invalidate
        aliceManager.invalidateSession(BOB_ID, "Test invalidation");
        
        assertFalse(aliceManager.hasSessionWith(BOB_ID));
        assertNull(aliceManager.getSessionKey(BOB_ID));
    }
    
    @Test
    @DisplayName("getActivePeers should return all peers with sessions")
    void testGetActivePeers() throws Exception {
        int charlieId = 3;
        KeyExchangeManager charlieManager = new KeyExchangeManager(charlieId, new SessionKeyManager());
        AtomicReference<KeyExchangeManager.KeyExchangeMessageData> charlieOutgoing = new AtomicReference<>();
        charlieManager.setMessageSender(charlieOutgoing::set);
        charlieManager.start();
        
        try {
            // Alice exchanges with Bob
            aliceManager.initiateKeyExchange(BOB_ID);
            bobManager.handleKeyExchangeRequest(ALICE_ID, aliceOutgoing.get().getPublicKey());
            aliceManager.handleKeyExchangeResponse(BOB_ID, bobOutgoing.get().getPublicKey());
            
            // Alice exchanges with Charlie
            aliceManager.initiateKeyExchange(charlieId);
            charlieManager.handleKeyExchangeRequest(ALICE_ID, aliceOutgoing.get().getPublicKey());
            aliceManager.handleKeyExchangeResponse(charlieId, charlieOutgoing.get().getPublicKey());
            
            // Check active peers
            var peers = aliceManager.getActivePeers();
            assertEquals(2, peers.size());
            assertTrue(peers.contains(BOB_ID));
            assertTrue(peers.contains(charlieId));
        } finally {
            charlieManager.shutdown();
        }
    }
    
    // ========================= Pending Exchange Tests =========================
    
    @Test
    @DisplayName("Pending exchange should be tracked")
    void testPendingExchangeTracking() throws Exception {
        assertFalse(aliceManager.isExchangePending(BOB_ID));
        
        aliceManager.initiateKeyExchange(BOB_ID);
        
        assertTrue(aliceManager.isExchangePending(BOB_ID));
        
        PendingKeyExchange pending = aliceManager.getPendingExchange(BOB_ID);
        assertNotNull(pending);
        assertEquals(BOB_ID, pending.getPeerId());
        assertTrue(pending.isInitiator());
        assertEquals(KeyExchangeState.INITIATED, pending.getState());
    }
    
    // ========================= Thread Safety Tests =========================
    
    @Test
    @DisplayName("Concurrent initiations to different peers should succeed")
    void testConcurrentInitiations() throws Exception {
        int numPeers = 10;
        CountDownLatch latch = new CountDownLatch(numPeers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < numPeers; i++) {
            final int peerId = 100 + i;
            new Thread(() -> {
                try {
                    aliceManager.initiateKeyExchange(peerId);
                    successCount.incrementAndGet();
                } catch (KeyExchangeException e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(numPeers, successCount.get(), "All initiations should succeed");
        assertEquals(0, errorCount.get(), "No errors expected");
    }
    
    // ========================= Helper Classes =========================
    
    /**
     * Test listener with default implementations for all methods.
     */
    private abstract static class TestListener implements KeyExchangeListener {
        @Override
        public void onKeyExchangeCompleted(int peerId, SecretKey sessionKey) {}
        
        @Override
        public void onKeyExchangeFailed(int peerId, KeyExchangeException cause) {}
    }
}
