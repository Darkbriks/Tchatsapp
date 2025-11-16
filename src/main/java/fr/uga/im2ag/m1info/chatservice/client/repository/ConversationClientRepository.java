package fr.uga.im2ag.m1info.chatservice.client.repository;

import java.util.Map;

import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.AbstractRepository;

public class ConversationClientRepository extends AbstractRepository<String, ConversationClient> {


    public ConversationClientRepository(Map<String, ConversationClient> conversations) {
        super(conversations);
    }

    public ConversationClientRepository() {
        super();
    }

    @Override
    protected String getKey(ConversationClient entity) {
        return entity.getConversationId();
    }
}
