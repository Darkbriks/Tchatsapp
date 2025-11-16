package fr.uga.im2ag.m1info.chatservice.client.repository;

import java.util.Map;
import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.model.GroupClient;
import fr.uga.im2ag.m1info.chatservice.client.utils.RepositoryWriter;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;

public class GroupClientRepository implements Repository<Integer, GroupClient>{

    private final Map<Integer, GroupClient> groups;
    private final RepositoryWriter<GroupClient> repositoryWriter = new RepositoryWriter<GroupClient>("groups");

    public GroupClientRepository(Map<Integer, GroupClient> groups) {
        this.groups = groups;
        loadFromCache();
    }

    public GroupClientRepository() {
        this(new java.util.HashMap<>());
    }

    @Override
    public void add(GroupClient entity) {
        groups.put(entity.getGroupId(), entity);
        repositoryWriter.writeData(entity);
    }

    @Override
    public void update(Integer id, GroupClient entity) {
        groups.put(id, entity);
        repositoryWriter.updateData(g -> g.getGroupId() == id, entity);
    }

    @Override
    public void delete(Integer id) {
        groups.remove(id);
        repositoryWriter.removeData(g -> g.getGroupId() == id);
    }

    @Override
    public GroupClient findById(Integer id) {
        return groups.get(id);
    }

    @Override
    public Set<GroupClient> findAll() {
        return Set.copyOf(groups.values());
    }

    private void loadFromCache() {
        Set<GroupClient> cachedGroups = repositoryWriter.readData();
        for (GroupClient group : cachedGroups) {
            groups.put(group.getGroupId(), group);
        }
    }
}
