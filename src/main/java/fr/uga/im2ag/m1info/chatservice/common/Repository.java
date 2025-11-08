package fr.uga.im2ag.m1info.chatservice.common;

import java.util.List;

// TODO: Maybe can be replaced by JPA, to discuss
public interface Repository {
    void add(Object entity);
    void update(Object entity);
    void delete(Object entity);
    void deleteById(Class<?> entityClass, Object id);
    <T> T findById(Class<T> entityClass, Object id);
    List<Object> findAll(Class<?> entityClass);
}
