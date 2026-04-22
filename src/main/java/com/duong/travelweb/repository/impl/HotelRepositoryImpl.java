package com.duong.travelweb.repository.impl;

import com.duong.travelweb.builder.HotelSearchBuider;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.entity.HotelEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class HotelRepositoryImpl implements HotelRepository {
    @PersistenceContext
    private EntityManager entityManager;
    @Override
    public List<HotelEntity> findHotel(HotelSearchBuider hotelSearchBuider) {
        String sql = "FROM HotelEntity H";
        Query query = entityManager.createQuery(sql,HotelEntity.class);
        return query.getResultList();
    }
}
