package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CountryEntity;
import com.duong.travelweb.repository.custom.CountryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CountryRepository extends JpaRepository<CountryEntity, UUID>, JpaSpecificationExecutor<CountryEntity>, CountryRepositoryCustom {
    
}
