package fr.uga.im2ag.m1info.chatservice.client.repository;

import java.util.Map;

import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
<<<<<<< HEAD
import fr.uga.im2ag.m1info.chatservice.client.utils.RepositoryWriter;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;
=======
import fr.uga.im2ag.m1info.chatservice.common.repository.AbstractRepository;
>>>>>>> 6eb8a37c98c57f76ac0100dec68a6172e323f059

public class ConversationClientRepository extends AbstractRepository<String, ConversationClient> {

<<<<<<< HEAD
    private final Map<String, ConversationClient> conversations;
    private final RepositoryWriter<ConversationClient> repositoryWriter = new RepositoryWriter<ConversationClient>("conversations");

    public ConversationClientRepository(Map<String, ConversationClient> conversations) {
        this.conversations = conversations;
        loadFromCache();
=======

    public ConversationClientRepository(Map<String, ConversationClient> conversations) {
        super(conversations);
>>>>>>> 6eb8a37c98c57f76ac0100dec68a6172e323f059
    }

    public ConversationClientRepository() {
        super();
    }

    @Override
<<<<<<< HEAD
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
=======
    protected String getKey(ConversationClient entity) {
        return entity.getConversationId();
>>>>>>> 6eb8a37c98c57f76ac0100dec68a6172e323f059
    }

    private void loadFromCache() {
        Set<ConversationClient> cachedConversations = repositoryWriter.readData();
        for (ConversationClient conversation : cachedConversations) {
            conversations.put(conversation.getConversationId(), conversation);
        }
    }
}
