package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;
import fr.uga.im2ag.m1info.chatservice.common.ShaIdGenerator;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.providers.MessageProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Factory class for creating ProtocolMessage instances from Packets.
 */
public class MessageFactory {
    private static final Map<MessageType, Supplier<ProtocolMessage>> registry = new HashMap<>();
    private static MessageIdGenerator messageIdGenerator;

    /* Static initializer to load message providers using ServiceLoader. */
    static {
        ServiceLoader<MessageProvider> loader = ServiceLoader.load(MessageProvider.class);
        for (MessageProvider provider : loader) {
            for (MessageType type : provider.getType()) {
                if (registry.containsKey(type)) {
                    System.err.println("Warning: Overriding existing message provider for type: " + type);
                }
                registry.put(type, provider::createInstance);
                System.out.println("Registered message provider for type: " + type);
            }
        }

        if (registry.isEmpty()) {
            throw new IllegalStateException("No message providers found! Check META-INF/services configuration.");
        }

        messageIdGenerator = new ShaIdGenerator(0);
    }

    public void setMessageIdGenerator(MessageIdGenerator generator) {
        if (generator == null) {
            throw new IllegalArgumentException("MessageIdGenerator cannot be null");
        }
        messageIdGenerator = generator;
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
        msg.generateNewMessageId(messageIdGenerator);
        return msg;
    }
}
