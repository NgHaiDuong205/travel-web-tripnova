package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CityEntity;
import com.duong.travelweb.repository.custom.CityRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CityRepository extends JpaRepository<CityEntity, UUID>, JpaSpecificationExecutor<CityEntity>, CityRepositoryCustom {
    
}
