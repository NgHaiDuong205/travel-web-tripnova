package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.CountrySearchBuilder;
import com.duong.travelweb.repository.custom.CountryRepositoryCustom;
import com.duong.travelweb.model.entity.CountryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;
import java.util.UUID;

public class CountryRepositoryImpl implements CountryRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<CountryEntity> findCountry(CountrySearchBuilder countrySearchBuilder) {
        String sql = "SELECT * FROM countries c ";
        Query query = entityManager.createNativeQuery(sql, CountryEntity.class);
        return query.getResultList();
    }

    public CountryEntity findNameById(UUID id) {
        return entityManager.find(CountryEntity.class, id);
    }
}
