package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.repository.custom.ContinentRepositoryCustom;
import com.duong.travelweb.model.entity.ContinentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.UUID;

public class ContinentRepositoryImpl implements ContinentRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ContinentEntity findNameById(UUID id) {
        String sql = "SELECT c FROM ContinentEntity c WHERE c.id = :id";
        Query query = entityManager.createQuery(sql, ContinentEntity.class);
        query.setParameter("id", id);
        try {
            return (ContinentEntity) query.getSingleResult();
        } catch (Exception e) {
            return new ContinentEntity();
        }
    }
}
