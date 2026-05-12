package com.duong.travelweb.repository.impl;

import com.duong.travelweb.builder.CountrySearchBuilder;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.entity.CountryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
@Primary
public class CountryRepositoryImpl implements CountryRepository {
    @PersistenceContext
    private EntityManager entityManager;
    @Override
    public List<CountryEntity> findCountry(CountrySearchBuilder countrySearchBuilder) {
        //JPQL
//        String sql = "FROM CountryEntity C";
//        Query query = entityManager.createQuery(sql,CountryEntity.class);
        // SQL Native
        String sql = "SELECT * FROM countries c ";
        Query query = entityManager.createNativeQuery(sql, CountryEntity.class);
        return query.getResultList();
    }

    @Override
    public CountryEntity findNameById(String id) {
        return entityManager.find(CountryEntity.class, id);
    }
}
