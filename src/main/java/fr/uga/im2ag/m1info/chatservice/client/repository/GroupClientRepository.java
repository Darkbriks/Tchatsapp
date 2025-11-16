package fr.uga.im2ag.m1info.chatservice.client.repository;

import java.util.Map;

import fr.uga.im2ag.m1info.chatservice.client.model.GroupClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.AbstractRepository;

public class GroupClientRepository extends AbstractRepository<Integer, GroupClient> {

    public GroupClientRepository(Map<Integer, GroupClient> groups) {
        super(groups);
    }

    public GroupClientRepository() {
        super();
    }

    @Override
    protected Integer getKey(GroupClient entity) {
        return entity.getGroupId();
    }
}
