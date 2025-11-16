package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.time.Instant;
import java.util.Map;

/**
 * Class representing a management message in the chat service protocol.
 */
public class ManagementMessage extends ProtocolMessage {
    private final Map<String, Object> params;

    /** Default constructor for ManagementMessage with CREATE_USER type.
     * This is used for message factory registration only.
     */
    public ManagementMessage() {
        super(MessageType.NONE, -1, -1);
        params = new java.util.HashMap<>();
    }

    /** Get a parameter by key.
     *
     * @param key the parameter key
     * @return the parameter value
     */
    public Object getParam(String key) {
        return params.get(key);
    }

    /** Get a parameter by key and cast it to the specified type.
     *
     * @param key the parameter key
     * @param clazz the class of the type to cast to
     * @param <T> the type to cast to
     * @return the parameter value cast to the specified type, or null if not found or cannot be cast
     */
    public <T> T getParamAsType(String key, Class<T> clazz) {
        Object value = params.get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        } else {
            try {
                if (clazz == Integer.class) {
                    return clazz.cast(Integer.parseInt(value.toString()));
                } else if (clazz == Long.class) {
                    return clazz.cast(Long.parseLong(value.toString()));
                } else if (clazz == Double.class) {
                    return clazz.cast(Double.parseDouble(value.toString()));
                } else if (clazz == Boolean.class) {
                    return clazz.cast(Boolean.parseBoolean(value.toString()));
                } else if (clazz == String.class) {
                    return clazz.cast(value.toString());
                }
            } catch (Exception e) {
                // Conversion failed
            }
            return null;
        }
    }

    /** Add a parameter to the message.
     *
     * @param key the parameter key
     * @param value the parameter value
     * @return the ManagementMessage instance (for chaining)
     */
    public ManagementMessage addParam(String key, Object value) {
        params.put(key, value);
        return this;
    }

    /** Remove a parameter from the message.
     *
     * @param key the parameter key
     * @return the ManagementMessage instance (for chaining)
     */
    public ManagementMessage removeParam(String key) {
        params.remove(key);
        return this;
    }

    @Override
    public Packet toPacket() {
        StringBuilder payload = new StringBuilder();
        payload.append(messageId).append("|").append(timestamp.toEpochMilli()).append("|");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            payload.append(entry.getKey()).append("=").append(entry.getValue().toString()).append(";");
        }
        int payloadLength = payload.length();
        return new Packet.PacketBuilder(payloadLength)
                .setMessageType(messageType)
                .setFrom(from)
                .setTo(to)
                .setPayload(payload.toString().getBytes())
                .build();
    }

    @Override
    public ManagementMessage fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();
        params.clear();
        String payload = new String(packet.getModifiablePayload().array());
        String[] parts = payload.split("\\|", 3);
        this.messageId = parts[0];
        this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        if (parts.length > 2) {
            String[] entries = parts[2].split(";");
            for (String entry : entries) {
                String[] keyValue = entry.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return this;
    }
}
