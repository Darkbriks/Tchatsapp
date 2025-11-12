package fr.uga.im2ag.m1info.chatservice.client.event.types;

import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;
import fr.uga.im2ag.m1info.chatservice.client.model.Message;

/**
 * Abstract class representing a message-related event in a chat service.
 */
public abstract class MessageEvent extends Event {
    private final Message message;
    private final int conversationId;

    /** Constructor for the MessageEvent class.
     *
     * @param source The source object that generated the event.
     * @param conversationId The ID of the conversation related to the message.
     * @param message The message associated with the event.
     */
    public MessageEvent(Object source, int conversationId, Message message) {
        super(source);
        this.conversationId = conversationId;
        this.message = message;
    }

    /** Gets the message associated with the event.
     *
     * @return The message of the event.
     */
    public Message getMessage() {
        return message;
    }

    /** Gets the ID of the conversation related to the message.
     *
     * @return The conversation ID.
     */
    public int getConversationId() {
        return conversationId;
    }
}
