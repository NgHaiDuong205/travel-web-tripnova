package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ContinentEntity;
import com.duong.travelweb.repository.custom.ContinentRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ContinentRepository extends JpaRepository<ContinentEntity, UUID>, JpaSpecificationExecutor<ContinentEntity>, ContinentRepositoryCustom {
    
}
