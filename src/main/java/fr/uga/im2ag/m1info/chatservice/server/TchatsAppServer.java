/*
 * Copyright (c) 2025.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of TchatsApp.
 *
 * TchatsApp is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * TchatsApp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TchatsApp. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.im2ag.m1info.chatservice.server;

import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.PacketProcessor;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageFactory;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessage;
import fr.uga.im2ag.m1info.chatservice.server.handlers.TextMessageHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A server that
 */
public class TchatsAppServer {
    private final static Logger LOG = Logger.getLogger(TchatsAppServer.class.getName());

    /**
     * Hard limit for a single messages (in bytes)
     */
    private final static int MAX_MSG_SIZE = 2<<20;

    /**
     * Size (in bytes) of the buffer used by the server to read
     */
    private final static int BUFFER_LENGTH = 2<<16; // 64ko

    /**
     * Delay (in s) after the connection that a client has to send its id (after this dely the connection is closed)
     */
    private final int IDENTIFY_TIMEOUT = 1;

    /**
     * Associates each channel (one per client, even if the client is not identified yet) to its state.
     */
    private final Map<SocketChannel, ConnectionState> activeConnections = new ConcurrentHashMap<>();

    /**
     * Associates each connected and known client id to its state.
     */
    private final Map<Integer, ConnectionState> connectedClients = new ConcurrentHashMap<>();

    /**
     * Associate client id to the queue of packets to be sent.
     * The client id is not necessarily connected.
     * The server can send message to clients where they connect.
     */
    private final Map<Integer, Queue<ByteBuffer>> clientQueues = new ConcurrentHashMap<>();

    /**
     * The processor responsible for processing received packets
     */
    private PacketProcessor packetProcessor;

    /**
     * Generator used for new client ids
     */
    private IdGenerator idGenerator;

    /**
     * Workers used to send notifications for message or connection events.
     */
    private final ExecutorService workers;

    /**
     * Scheduler used to check the timeout for client identification
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Multiplexor used for the channels
     */
    private final Selector selector;
    private volatile boolean started;

    private final ServerContext serverContext;

    static class ConnectionState {
        final SocketChannel channel;
        int clientId;
        final Instant connectedAt;
        final ByteBuffer readBuffer;
        volatile boolean identified;
        Packet.PacketBuilder currentPacket;

        ConnectionState(SocketChannel c) {
            this.clientId = 0;
            this.channel = c;
            this.readBuffer = ByteBuffer.allocate(BUFFER_LENGTH);
            this.connectedAt = Instant.now();
            this.identified = false;
            this.currentPacket = null;
        }
    }

    public class ServerContext {
        public void sendPacketToClient(Packet pkt) {
            TchatsAppServer.this.sendPacket(pkt);
        }

        public int generateClientId() {
            return idGenerator.generateId();
        }
    }

    /**
     * Initializes a new server with a default packet processor that forwards packet to the
     * recipient. This default behavior can be changed by supplying a customized PacketProcessor to
     * the method {@link #setPacketProcessor setPacketProcessor }
     * @param port the port on which the server is listening
     * @param workerThreads the number of threads used to process packets
     * @throws IOException
     */
    public TchatsAppServer(int port, int workerThreads) throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.workers = Executors.newFixedThreadPool(workerThreads);
        setClientIdGenerator(new AtomicInteger(1)::getAndIncrement);
        this.serverContext = new ServerContext();
        registerFactoryMessages();
        LOG.info("Server started on port " + port + " with " + workerThreads + " workers");
        // default processors left empty -> defaultForwardProcessor used when missing
    }

    private void registerFactoryMessages() {
        MessageFactory.register(MessageType.TEXT, TextMessage::new);
    }

    /* ----------------------- configuration / registration ----------------------- */

    public void setClientIdGenerator(IdGenerator gen) {
        if (gen == null) throw new NullPointerException("IdGenerator cannot be null");
        idGenerator = gen;
    }

    public void setPacketProcessor(PacketProcessor processor) {
        if (processor == null) {
            throw new NullPointerException("PacketProcessor cannot be null");
        }
        this.packetProcessor = processor;
    }

    /* ----------------------- core NIO loop ----------------------- */

    /**
     * starts the server. This method blocks until the server is running.
     * The server can be stoped via a call to {@link #stop stop() method}
     * @throws IOException
     */
    public void start() throws IOException {
        started = true;
        while (started) {
            selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                try {
                    if (key.isValid() && key.isAcceptable()) accept(key);
                    if (key.isValid() && key.isReadable()) read(key);
                    if (key.isValid() && key.isWritable()) write(key);
                } catch (CancelledKeyException | IOException e) {
                    closeKey(key);
                }
            }
        }
       // selector.close(); workers.shutdownNow(); scheduler.shutdownNow();
    }

    /**
     * Stops th server
     */
    public void stop() {
        started = false;
        selector.wakeup();
    }

    /* ----------------------- accept / read / write ----------------------- */

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        if (sc == null) return;
        sc.configureBlocking(false);
        activeConnections.put(sc, new ConnectionState(sc));
        sc.register(selector, SelectionKey.OP_READ);
        // if no client id is received after IDENTIFY_TIMEOUT seconds after opening the connection
        // then the connection is closed.
        scheduler.schedule(() -> {
            ConnectionState st = activeConnections.get(sc);
            if (st == null || !st.identified) {
                closeChannel(sc);
                LOG.info("Client not identified after " + IDENTIFY_TIMEOUT + " second(s) : connection closed");
            }
        }, IDENTIFY_TIMEOUT, TimeUnit.SECONDS);
        LOG.info("Accepted connection from " + sc.getRemoteAddress());
    }

    private void read(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ConnectionState state = activeConnections.get(sc);
        if (state == null) { closeChannel(sc); return; }
        ByteBuffer buf = state.readBuffer;

        try {
            int read = sc.read(buf);
            if (read == -1) {
                closeChannel(sc);
                return;
            }
            buf.flip();
            loop:
            while (buf.hasRemaining()) {
                if (!state.identified) {
                    if (buf.remaining() < Integer.BYTES) break loop;
                    int clientId = buf.getInt();
                    if (clientId == 0) {
                        clientId = serverContext.generateClientId();
                        clientQueues.put(clientId, new ConcurrentLinkedQueue<>());
                    } else if (!clientQueues.containsKey(clientId)) {
                        LOG.info("Client " + clientId + " is not registered. Closing connexion.");
                        closeChannel(sc);
                        return;
                    }
                    if (connectedClients.putIfAbsent(clientId, state) != null) {
                        LOG.info("Client " + clientId + " already connected. Closing connexion.");
                        closeChannel(sc);
                        return;
                    }
                    state.clientId = clientId;
                    state.identified = true;

                    // send identification ok (empty packet) using same semantics as before
                    sc.write(Packet.createEmptyPacket(0, clientId, MessageType.CREATE_USER).asByteBuffer());

                    wakeupSendQueue(state.channel);
                    LOG.info("Client " + state.clientId + " identified");
                }

                if (state.currentPacket == null) {
                    if (buf.remaining() < Integer.BYTES) break loop;
                    if (buf.remaining() >= Packet.getHeaderSize()) {
                        readPacket(buf, state);
                    } else {
                        break loop;
                    }
                }

                if (state.currentPacket != null && state.currentPacket.isReady()) {
                    if (state.currentPacket.fillFrom(buf).isCompleted()) {
                        Packet pkt = state.currentPacket.build();
                        state.currentPacket = null;
                        // désérialiser vers ProtocolMessage via MessageFactory
                        final Packet finalPkt = pkt;
                        workers.submit(() -> {
                            try {
                                ProtocolMessage message = MessageFactory.fromPacket(finalPkt);
                                LOG.info("packet converted to ProtocolMessage of type " + message.getMessageType() + " from client " + state.clientId);
                                processMessage(message);
                            } catch (RuntimeException e) {
                                LOG.warning("Failed to convert packet to ProtocolMessage: " + e.getMessage());
                            }
                        });
                        LOG.info("packet read from client " + state.clientId);
                    }
                }
            }
            buf.compact();
        } catch (IOException e) {
            closeChannel(sc);
        }
    }

    private void readPacket(ByteBuffer buf, ConnectionState state) {
        int msgLength = buf.getInt();
        if (msgLength < 0 || msgLength > MAX_MSG_SIZE) {
            LOG.warning("Invalid packet length " + msgLength + " from client " + state.clientId);
            closeChannel(state.channel);
            return;
        }

        MessageType mt = MessageType.fromInt(buf.getInt());
        int from = buf.getInt();
        int to = buf.getInt();

        state.currentPacket = new Packet.PacketBuilder(msgLength)
                .setFrom(from)
                .setTo(to)
                .setMessageType(mt);
    }

    private void write(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ConnectionState st = activeConnections.get(sc);
        if (st == null) { closeChannel(sc); return; }
        Queue<ByteBuffer> writeQueue = clientQueues.get(st.clientId);
        if (writeQueue == null) { closeChannel(sc); return; }
        try {
            ByteBuffer buf;
            while ((buf = writeQueue.peek()) != null) {
                LOG.info("write packet from "+buf.getInt(4)+" to "+buf.getInt(8)+"("+st.clientId+")");
                sc.write(buf);
                if (buf.hasRemaining()) break;
                LOG.info("writed packet on  "+sc);
                writeQueue.poll();
            }
            if (writeQueue.isEmpty()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            closeChannel(sc);
        }
    }

    /* ----------------------- dispatch / default forwarding ----------------------- */

    /**
     * Envoie un Packet vers la queue du destinataire
     */
    public void sendPacket(Packet pkt) {
        int to = pkt.to();
        Queue<ByteBuffer> q = clientQueues.get(to);
        if (q != null) {
            q.offer(pkt.asByteBuffer());
            ConnectionState cs = connectedClients.get(to);
            if (cs != null) wakeupSendQueue(cs.channel);
        } else {
            LOG.info("No queue for " + to + ", packet ignored");
        }
    }

    private void wakeupSendQueue(SocketChannel channel) {
        SelectionKey key = channel.keyFor(selector);
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }

    private void processMessage(ProtocolMessage message) {
        if (packetProcessor != null) {
            try {
                packetProcessor.process(message);
            } catch (RuntimeException e) {
                LOG.warning("Packet processing failed for " + message.getMessageType() + ": " + e.getMessage());
            }
        } else {
            // default forward behaviour : transforme en Packet et envoie
            try {
                Packet pkt = message.toPacket();
                sendPacket(pkt);
            } catch (RuntimeException e) {
                LOG.warning("Default forward failed for " + message.getMessageType() + ": " + e.getMessage());
            }
        }
    }

    /* ----------------------- cleanup ----------------------- */

    private void closeChannel(SocketChannel sc) {
        try { sc.close(); } catch (IOException ignored) {}
        ConnectionState cs = activeConnections.remove(sc);
        if (cs != null && cs.identified) {
            connectedClients.remove(cs.clientId, cs);
        }
    }

    private void closeKey(SelectionKey key) {
        try {
            Channel ch = key.channel();
            key.cancel();
            ch.close();
        } catch (IOException ignored) {}
    }

    public void removeClient(int clientId) {
        ConnectionState cs = connectedClients.remove(clientId);
        if (cs != null && cs.channel != null) {
            activeConnections.remove(cs.channel, cs);
            try { cs.channel.close(); } catch (IOException ignored) {}
        }
        clientQueues.remove(clientId);
    }

    /* ----------------------- main (exemple d'enregistrement de processors) ----------------------- */

    public static void main(String[] args) throws Exception {
        int port = 1666;
        int workers = Math.max(2, Runtime.getRuntime().availableProcessors());
        TchatsAppServer s = new TchatsAppServer(port, workers);

        ServerPacketRouter router = new ServerPacketRouter(s.serverContext);
        router.addHandler(new TextMessageHandler());
        s.setPacketProcessor(router);

        s.start();
    }

    /* ----------------------- simple IdGenerator functional type ----------------------- */

    @FunctionalInterface
    public interface IdGenerator {
        int generateId();
    }
}
