package fr.uga.im2ag.m1info.chatservice.client.repository;

import fr.uga.im2ag.m1info.chatservice.client.model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.AbstractRepository;

import java.util.Map;

public class ConversationClientRepository extends AbstractRepository<String, ConversationClient> {


    public ConversationClientRepository(Map<String, ConversationClient> conversations) {
        super(conversations, "conversationClientRepository");
    }

    public ConversationClientRepository() {
        super("conversationClientRepository");
    }

    @Override
    protected String getKey(ConversationClient entity) {
        return entity.getConversationId();
    }
}