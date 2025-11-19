package fr.uga.im2ag.m1info.chatservice.server.repository;

import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;
import fr.uga.im2ag.m1info.chatservice.server.model.GroupInfo;
import fr.uga.im2ag.m1info.chatservice.server.model.UserInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GroupRepository implements Repository<Integer, GroupInfo>{

    private final Map<Integer, GroupInfo> groups;

    public GroupRepository(Map<Integer, GroupInfo> groups) {
        this.groups = groups;
    }

    public GroupRepository() {
        this.groups = new HashMap<Integer, GroupInfo>();
    }

    @Override
    public void add(GroupInfo entity) {
        groups.put(entity.getId(), entity);
    }

    @Override
    public void update(Integer id, GroupInfo entity) {
        groups.put(id, entity);
    }

    @Override
    public void delete(Integer id) {
        groups.remove(id);
    }

    @Override
    public GroupInfo findById(Integer id) {
        return groups.get(id);
    }

    @Override
    public Set<GroupInfo> findAll() {
        return Set.copyOf(groups.values());
    }
}
