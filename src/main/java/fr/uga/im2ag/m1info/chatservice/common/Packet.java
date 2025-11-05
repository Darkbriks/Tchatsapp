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

package fr.uga.im2ag.m1info.chatservice.common;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Represents a packet sent between the client and the server (or vice versa)
 * It contains a ByteBuffer where the first 4 bytes are the length (in bytes) of the payload (i.e. the content),
 * then 4 bytes for the sender id, 4 bytes for the recipient id and then the payload.
 */
public class Packet {

    private static final int OFFSET_LENGTH = 0;
    private static final int OFFSET_FROM   = Integer.BYTES;
    private static final int OFFSET_TO     = 2 * Integer.BYTES;
    private final static int HEADER_SIZE = 3*Integer.BYTES;


    /**
     * A builder for packets.
     * */
    public static final class PacketBuilder {

        private ByteBuffer buf;

        public PacketBuilder(int payloadSize,int from) {
            buf = ByteBuffer.allocate(payloadSize+HEADER_SIZE);
            buf.putInt(payloadSize);
            buf.putInt(from);
        }


        public PacketBuilder(int dataSize,int from, int to) {
            this(dataSize,from);
            buf.putInt(to);
        }

       /* public PacketBuilder setFrom(int from) {
            buf.putInt(OFFSET_FROM,from);
            return this;
        }*/

        public PacketBuilder setTo(int to) {
            buf.putInt(OFFSET_TO,to);
            return this;
        }

        public PacketBuilder setPayload(byte[] payload) {
            if (payload.length<buf.capacity()-HEADER_SIZE) {
                throw new IllegalArgumentException("payload is of length "+payload.length+" but payload is of length "+(buf.capacity()-HEADER_SIZE));
            }
            buf.position(HEADER_SIZE);
            buf.put(payload);
            return this;
        }

        public boolean isReady() {
            return buf!=null && !isCompleted();
        }

        public ByteBuffer getPayload() {
            return  buf.slice(HEADER_SIZE,buf.capacity());
        }

        public PacketBuilder fillFrom(ByteBuffer bf) {
            if (buf==null) throw new IllegalStateException("reset method has to be called before");
            int length = Math.min(buf.remaining(),bf.remaining());
            buf.put(buf.position(),bf,bf.position(),length);
            buf.position(buf.position()+length);
            bf.position(bf.position()+length);
            //return !buf.hasRemaining();
            return this;
        }

        public boolean isCompleted() {
            if (buf==null) throw new IllegalStateException("reset method has to be called before");
            return !buf.hasRemaining();
        }

        public Packet build() {
            if (isCompleted()) {
                buf.position(0);
                Packet res=new Packet(buf);
                buf=null;
                return res;
            }
            throw new RuntimeException("Packet not finished..."); // could have computed automatically the size...
        }
    }

    // ORDER : payload length(4) - from(4) - to(4) - payload(size)
    private final ByteBuffer buffer;


    private Packet(ByteBuffer buf) {
        buffer = buf.rewind().asReadOnlyBuffer();
    }

    public int from() {
        return buffer.getInt(OFFSET_FROM);
    }

    public int to() {
        return buffer.getInt(OFFSET_TO);
    }


    public int payloadSize() {
        return buffer.getInt(0);
    }

    /**
     * Returns a readonly view of the payload
     * The position of the buffer is 0.
     */
    public ByteBuffer getPayload() {
        return buffer.slice(HEADER_SIZE, buffer.getInt(0));
    }

    /**
     * Returns a (readonly) view of the whole packet
     */
    public ByteBuffer asByteBuffer() {
        return buffer.duplicate().rewind();
    }


    public static Packet readFrom(DataInputStream dis) throws IOException {
        int s = dis.readInt();
        ByteBuffer buf = ByteBuffer.allocate(s+HEADER_SIZE);
        buf.putInt(s);
        byte[] content=buf.array();
        dis.readFully(content,4,content.length-4);
        return new Packet(buf);
    }

    public static Packet createTextMessage(int from, int to, String content) {
        byte[] payload = content.getBytes();
        return  new PacketBuilder(payload.length,from,to).setPayload(payload).build();
    }

    public static Packet createEmptyPacket(int from, int to) {
        return new PacketBuilder(0,from,to).build();
    }
}
