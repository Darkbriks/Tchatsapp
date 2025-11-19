package fr.uga.im2ag.m1info.chatservice.server.repository;

import fr.uga.im2ag.m1info.chatservice.common.repository.AbstractRepository;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;

import java.util.Map;

public class UserRepository extends AbstractRepository<Integer, UserInfo> {
    public UserRepository(Map<Integer, UserInfo> users) {
        super(users, "userRepository");
    }

    public UserRepository() {
        super("userRepository");
    }

    @Override
    protected Integer getKey(UserInfo entity) {
        return entity.getId();
    }
}
