package fr.uga.im2ag.m1info.chatservice.client.repository;

import fr.uga.im2ag.m1info.chatservice.client.model.GroupClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.AbstractRepository;

import java.util.Map;

public class GroupClientRepository extends AbstractRepository<Integer, GroupClient> {

    public GroupClientRepository(Map<Integer, GroupClient> groups) {
        super(groups, "groupClientRepository");
    }

    public GroupClientRepository() {
        super("groupClientRepository");
    }

    @Override
    protected Integer getKey(GroupClient entity) {
        return entity.getGroupId();
    }
}