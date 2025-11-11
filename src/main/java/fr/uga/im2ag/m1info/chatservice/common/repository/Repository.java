package fr.uga.im2ag.m1info.chatservice.common.repository;

import java.util.Set;

public interface Repository<T> {
    public void add(T entity);
    public void update(int id,T entity);
    public void delete(int id);
    public T findById(int id);
    public Set<T> findAll();
}
