package fr.uga.im2ag.m1info.chatservice.server.repository;

import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserRepository implements Repository<UserInfo> {
    private final Map<Integer, UserInfo> users;

    public UserRepository(Map<Integer, UserInfo> users) {
        this.users = users;
    }

    public UserRepository() {
        this.users = new HashMap<Integer, UserInfo>();
    }

    @Override
    public void add(UserInfo entity) {
        users.put(entity.getId(), entity);
    }

    @Override
    public void update(int id, UserInfo entity) {
        users.put(id, entity);
    }

    @Override
    public void delete(int id) {
        users.remove(id);
    }

    @Override
    public UserInfo findById(int id) {
        return users.get(id);
    }

    @Override
    public Set<UserInfo> findAll() {
        return Set.copyOf(users.values());
    }
}
