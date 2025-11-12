package fr.uga.im2ag.m1info.chatservice.client.repository;

import java.util.Map;
import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.model.ContactClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;

public class ContactClientRepository implements Repository<Integer, ContactClient>{

    private final Map<Integer, ContactClient> contacts;

    public ContactClientRepository(Map<Integer, ContactClient> contacts) {
        this.contacts = contacts;
    }

    public ContactClientRepository() {
        this(new java.util.HashMap<>());
    }

    @Override
    public void add(ContactClient entity) {
        contacts.put(entity.getContactId(), entity);
    }

    @Override
    public void update(Integer id, ContactClient entity) {
        contacts.put(id, entity);
    }

    @Override
    public void delete(Integer id) {
        contacts.remove(id);
    }

    @Override
    public ContactClient findById(Integer id) {
        return contacts.get(id);
    }

    @Override
    public Set<ContactClient> findAll() {
        return Set.copyOf(contacts.values());
    }
}
