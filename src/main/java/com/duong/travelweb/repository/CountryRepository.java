package com.duong.travelweb.repository;

import com.duong.travelweb.repository.entity.CountryEntity;

import java.util.List;
import java.util.Map;

public interface CountryRepository {
    List<CountryEntity> findCountry(Map<String,Object> params,List<String> typeCode);
    CountryEntity findNameById(String id);
}
