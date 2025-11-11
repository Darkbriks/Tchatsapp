package fr.uga.im2ag.m1info.chatservice.client.Repository;

import java.util.Map;
import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.Model.ConversationClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;

public class ConversationClientRepository implements Repository<ConversationClient>{

    private Map<Integer, ConversationClient> conversations;

    @Override
    public void add(ConversationClient entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public void update(int id, ConversationClient entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void delete(int id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public ConversationClient findById(int id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public Set<ConversationClient> findAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }


}
