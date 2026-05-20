package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.repository.custom.CityRepositoryCustom;
import com.duong.travelweb.model.entity.CityEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.UUID;

public class CityRepositoryImpl implements CityRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CityEntity findNameById(UUID id) {
        String sql = "FROM CityEntity c WHERE c.id = :id";
        Query query = entityManager.createQuery(sql, CityEntity.class);
        query.setParameter("id", id);
        return (CityEntity) query.getSingleResult();
    }
}
