package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.AmenityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<AmenityEntity, UUID> {

    @Query("SELECT a FROM AmenityEntity a ORDER BY a.category, a.name")
    List<AmenityEntity> findAllOrdered();

    @Query("SELECT a FROM AmenityEntity a WHERE a.category = :category ORDER BY a.name")
    List<AmenityEntity> findByCategory(@Param("category") String category);
}
