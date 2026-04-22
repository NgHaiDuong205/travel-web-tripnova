package com.duong.travelweb.repository.impl;

import com.duong.travelweb.repository.CityRepository;
import com.duong.travelweb.repository.entity.CityEntity;
import com.duong.travelweb.repository.entity.CountryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class CityRepositoryImpl implements CityRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CityEntity findNameById(Long id) {
        String sql = "FROM CityEntity c WHERE c.id = :id";
        Query query = entityManager.createQuery(sql, CityEntity.class);
        query.setParameter("id", id.intValue());
        return (CityEntity) query.getSingleResult();
    }
}
