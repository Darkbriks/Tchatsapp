package fr.uga.im2ag.m1info.chatservice.client.Repository;

import java.util.Map;
import java.util.Set;

import fr.uga.im2ag.m1info.chatservice.client.Model.GroupClient;
import fr.uga.im2ag.m1info.chatservice.common.repository.Repository;

public class GroupClientRepository implements Repository<GroupClient>{

    private Map<Integer, GroupClient> groups;

    @Override
    public void add(GroupClient entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public void update(int id, GroupClient entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void delete(int id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public GroupClient findById(int id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public Set<GroupClient> findAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    

}
