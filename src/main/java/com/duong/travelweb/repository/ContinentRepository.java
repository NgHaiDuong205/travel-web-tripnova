package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ContinentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ContinentRepository extends JpaRepository<ContinentEntity, UUID>, JpaSpecificationExecutor<ContinentEntity> {
    
}
