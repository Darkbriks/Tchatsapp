package fr.uga.im2ag.m1info.chatservice.client.repository;

import java.util.Map;
import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;

public class ConversationClientRepository implements Repository<String, ConversationClient>{

    private final Map<String, ConversationClient> conversations;

    public ConversationClientRepository(Map<String, ConversationClient> conversations) {
        this.conversations = conversations;
    }

    public ConversationClientRepository() {
        this(new java.util.HashMap<>());
    }

    @Override
    public void add(ConversationClient entity) {
        conversations.put(entity.getConversationId(), entity);
    }

    @Override
    public void update(String id, ConversationClient entity) {
        conversations.put(id, entity);
    }

    @Override
    public void delete(String id) {
        conversations.remove(id);
    }

    @Override
    public ConversationClient findById(String id) {
        return conversations.get(id);
    }

    @Override
    public Set<ConversationClient> findAll() {
        return Set.copyOf(conversations.values());
    }
}
