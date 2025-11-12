package fr.uga.im2ag.m1info.chatservice.common.repository;

import java.util.Set;

public interface Repository<ID, T> {
    public void add(T entity);
    public void update(ID id,T entity);
    public void delete(ID id);
    public T findById(ID id);
    public Set<T> findAll();
}
