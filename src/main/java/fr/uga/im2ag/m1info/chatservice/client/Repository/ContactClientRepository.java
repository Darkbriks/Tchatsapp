package fr.uga.im2ag.m1info.chatservice.client.Repository;

import java.util.Map;
import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.Model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;

public class ContactClientRepository implements Repository<ContactClient>{

    private Map<Integer, ContactClient> contacts;

    @Override
    public void add(ContactClient entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public void update(int id, ContactClient entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void delete(int id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public ContactClient findById(int id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public Set<ContactClient> findAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }


}
