package com.duong.travelweb.repository.custom.impl;

import com.duong.travelweb.builder.HotelSearchBuilder;
import com.duong.travelweb.repository.custom.HotelRepositoryCustom;
import com.duong.travelweb.model.entity.HotelEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;
import java.util.UUID;

public class HotelRepositoryImpl implements HotelRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<HotelEntity> findHotel(HotelSearchBuilder hotelSearchBuilder) {
        String sql = "FROM HotelEntity H";
        Query query = entityManager.createQuery(sql,HotelEntity.class);
        return query.getResultList();
    }
}
