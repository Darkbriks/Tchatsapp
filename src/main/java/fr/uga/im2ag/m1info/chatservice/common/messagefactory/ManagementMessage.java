package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.uga.im2ag.m1info.chatservice.common.TypeConverter;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Class representing a management message in the chat service protocol.
 */
public class ManagementMessage extends ProtocolMessage {
    private static final Gson gson = new Gson();
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
        return TypeConverter.convert(params.get(key), clazz);
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
        StringBuilder payload = getStringBuilder();
        payload.append(messageId).append("|");
        payload.append(timestamp.toEpochMilli()).append("|");
        payload.append(gson.toJson(params));

        byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        return new Packet.PacketBuilder(bytes.length)
                .setMessageType(messageType)
                .setFrom(from)
                .setTo(to)
                .setPayload(bytes)
                .build();
    }

    @Override
    public ManagementMessage fromPacket(Packet packet) {
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();

        String payload = new String(packet.getModifiablePayload().array(), StandardCharsets.UTF_8);
        String[] parts = payload.split("\\|", 3);

        this.messageId = parts[0];
        this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[1]));

        if (parts.length > 2) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> parsedParams = gson.fromJson(parts[2], type);
            params.clear();
            params.putAll(parsedParams);
        }

        return this;
    }
}
