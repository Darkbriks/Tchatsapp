package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory class for creating ProtocolMessage instances from Packets.
 */
public class MessageFactory {
    private static final Map<MessageType, Supplier<ProtocolMessage>> registry = new HashMap<>();

    /** Register a ProtocolMessage constructor for a given MessageType.
     *
     * @param type the MessageType to register
     * @param constructor a Supplier that constructs a ProtocolMessage of the given type
     */
    public static void register(MessageType type, Supplier<ProtocolMessage> constructor) {
        registry.put(type, constructor);
    }

    /** Create a ProtocolMessage from a Packet.
     *
     * @param packet the Packet to convert
     * @return the corresponding ProtocolMessage
     * @throws IllegalArgumentException if the MessageType is unknown
     */
    public static ProtocolMessage fromPacket(Packet packet) {
        MessageType type = packet.messageType();
        Supplier<ProtocolMessage> constructor = registry.get(type);
        if (constructor == null) {
            System.out.println("Supported types: " + registry.keySet());
            throw new IllegalArgumentException("Unknown message type: " + type);
        }
        ProtocolMessage msg = constructor.get();
        msg.fromPacket(packet);
        return msg;
    }

    /** Create a ProtocolMessage of a given type with specified sender and recipient.
     *
     * @param type the MessageType of the message
     * @param from the sender ID
     * @param to the recipient ID
     * @return the constructed ProtocolMessage
     * @throws IllegalArgumentException if the MessageType is unknown
     */
    public static ProtocolMessage create(MessageType type, int from, int to) {
        Supplier<ProtocolMessage> constructor = registry.get(type);
        if (constructor == null) {
            System.out.println("Supported types: " + registry.keySet());
            throw new IllegalArgumentException("Unknown message type: " + type);
        }
        ProtocolMessage msg = constructor.get();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setMessageType(type);
        return msg;
    }
}
