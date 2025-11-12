package fr.uga.im2ag.m1info.chatservice.client.model;

import java.time.Instant;
import java.util.*;

public class ConversationClient {
    private final String conversationId;
    private String conversationName;
    private final Map<String, Message> messages;
    private final SortedMap<Instant, String> messageOrder; // Red Black Tree, O(log n) for get, put, remove
    private final Set<Integer> participantIds;
    private final boolean isGroupConversation;

    public ConversationClient(String conversationId, String conversationName, Map<String, Message> messages, SortedMap<Instant, String> messageOrder, Set<Integer> participantIds, boolean isGroupConversation) {
        this.conversationId = conversationId;
        this.conversationName = conversationName;
        this.messages = messages;
        this.messageOrder = messageOrder;
        this.participantIds = participantIds;
        this.isGroupConversation = isGroupConversation;
    }

    public ConversationClient(String conversationId, Set<Integer> participantIds, boolean isGroupConversation) {
        this(conversationId, "Conversation " + conversationId, new HashMap<>(), new TreeMap<>(), participantIds, isGroupConversation);
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public void addMessage(Message message) {
        messages.put(message.getMessageId(), message);
        messageOrder.put(message.getTimestamp(), message.getMessageId());
    }

    public void removeMessage(String messageId) {
        Message message = messages.remove(messageId);
        if (message != null) {
            messageOrder.remove(message.getTimestamp());
        }
    }

    /** Get a sublist of n (all if n is -1) messages, ordered by timestamp,
     * from the message with id startMessageId (inclusive if specified),
     * or from the beginning if startMessageId is null.
     *
     * @param startMessageId the id of the message to start from (inclusive), or null to start from the beginning
     * @param n the number of messages to retrieve, or -1 to retrieve all messages
     * @param inclusive whether to include the startMessageId message
     * @param ascending whether to return messages in ascending order (oldest first) or descending order (newest first)
     * @return a list of messages
     */
    public List<Message> getMessagesFrom(String startMessageId, int n, boolean inclusive, boolean ascending) {
        List<Message> result = new ArrayList<>();
        boolean startAdding = (startMessageId == null);
        for (Map.Entry<Instant, String> entry : messageOrder.entrySet()) {
            String messageId = entry.getValue();
            if (!startAdding) {
                if (messageId.equals(startMessageId)) {
                    startAdding = true;
                    if (inclusive) {
                        result.add(messages.get(messageId));
                    }
                }
            } else {
                if (n != -1 && result.size() >= n) {
                    break;
                }
                result.add(messages.get(messageId));
            }
        }
        return ascending ? result : result.reversed();
    }

    public Set<Integer> getParticipantIds() {
        return Set.copyOf(participantIds);
    }

    public boolean isGroupConversation() {
        return isGroupConversation;
    }
}
