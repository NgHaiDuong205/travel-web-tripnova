package com.duong.travelweb.repository;

import com.duong.travelweb.repository.entity.CountryEntity;

import java.util.List;

public interface CountryRepository {
    List<CountryEntity> findAll(String countryName);

}
