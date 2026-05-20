package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.CountrySearchBuilder;
import com.duong.travelweb.model.entity.CountryEntity;

import java.util.List;
import java.util.UUID;

public interface CountryRepositoryCustom {
    List<CountryEntity> findCountry(CountrySearchBuilder countrySearchBuilder);
    CountryEntity findNameById(UUID id);
}
