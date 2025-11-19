package fr.uga.im2ag.m1info.chatservice.server.repository;

import fr.uga.im2ag.m1info.chatservice.common.repository.AbstractRepository;
import fr.uga.im2ag.m1info.chatservice.server.model.GroupInfo;

import java.util.Map;

public class GroupRepository extends AbstractRepository<Integer, GroupInfo> {
    public GroupRepository(Map<Integer, GroupInfo> groups) {
        super(groups, "groupRepository");
    }

    public GroupRepository() {
        super("groupRepository");
    }

    @Override
    protected Integer getKey(GroupInfo entity) {
        return entity.getId();
    }
}

