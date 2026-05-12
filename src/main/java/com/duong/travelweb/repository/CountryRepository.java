package com.duong.travelweb.repository;

import com.duong.travelweb.builder.CountrySearchBuilder;
import com.duong.travelweb.repository.entity.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;

public interface CountryRepository extends JpaRepository<CountryEntity, String> {
    List<CountryEntity> findCountry(CountrySearchBuilder countrySearchBuilder);
    CountryEntity findNameById(String id);
}
