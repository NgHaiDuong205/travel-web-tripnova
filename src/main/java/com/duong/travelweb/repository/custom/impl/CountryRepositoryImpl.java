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
        StringBuilder sql = new StringBuilder("SELECT * FROM countries c WHERE 1=1 ");

        if (countrySearchBuilder.getName() != null && !countrySearchBuilder.getName().isEmpty()) {
            sql.append("AND LOWER(c.name) LIKE :name ");
        }
        if (countrySearchBuilder.getCountryCode() != null && !countrySearchBuilder.getCountryCode().isEmpty()) {
            sql.append("AND LOWER(c.country_code) = :countryCode ");
        }
        if (countrySearchBuilder.getSlug() != null && !countrySearchBuilder.getSlug().isEmpty()) {
            sql.append("AND LOWER(c.slug) = :slug ");
        }
        if (countrySearchBuilder.getContinentId() != null) {
            sql.append("AND c.continent_id = :continentId ");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), CountryEntity.class);

        if (countrySearchBuilder.getName() != null && !countrySearchBuilder.getName().isEmpty()) {
            query.setParameter("name", "%" + countrySearchBuilder.getName().toLowerCase() + "%");
        }
        if (countrySearchBuilder.getCountryCode() != null && !countrySearchBuilder.getCountryCode().isEmpty()) {
            query.setParameter("countryCode", countrySearchBuilder.getCountryCode().toLowerCase());
        }
        if (countrySearchBuilder.getSlug() != null && !countrySearchBuilder.getSlug().isEmpty()) {
            query.setParameter("slug", countrySearchBuilder.getSlug().toLowerCase());
        }
        if (countrySearchBuilder.getContinentId() != null) {
            query.setParameter("continentId", countrySearchBuilder.getContinentId());
        }

        return query.getResultList();
    }
}
