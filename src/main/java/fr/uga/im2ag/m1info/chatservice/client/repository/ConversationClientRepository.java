package fr.uga.im2ag.m1info.chatservice.client.repository;

import java.util.Map;
import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.client.utils.RepositoryWriter;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;

public class ConversationClientRepository implements Repository<String, ConversationClient>{

    private final Map<String, ConversationClient> conversations;
    private final RepositoryWriter<ConversationClient> repositoryWriter = new RepositoryWriter<ConversationClient>("conversations");

    public ConversationClientRepository(Map<String, ConversationClient> conversations) {
        this.conversations = conversations;
        loadFromCache();
    }

    public ConversationClientRepository() {
        this(new java.util.HashMap<>());
    }

    @Override
    public void add(ConversationClient entity) {
        conversations.put(entity.getConversationId(), entity);
        repositoryWriter.writeData(entity);
    }

    @Override
    public void update(String id, ConversationClient entity) {
        conversations.put(id, entity);
        repositoryWriter.updateData(c -> c.getConversationId() == id, entity);
    }

    @Override
    public void delete(String id) {
        conversations.remove(id);
        repositoryWriter.removeData(c -> c.getConversationId() == id);
    }

    @Override
    public ConversationClient findById(String id) {
        return conversations.get(id);
    }

    @Override
    public Set<ConversationClient> findAll() {
        return Set.copyOf(conversations.values());
    }

    private void loadFromCache() {
        Set<ConversationClient> cachedConversations = repositoryWriter.readData();
        for (ConversationClient conversation : cachedConversations) {
            conversations.put(conversation.getConversationId(), conversation);
        }
    }
}
