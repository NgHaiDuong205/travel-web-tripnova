package com.duong.travelweb.repository;

import com.duong.travelweb.repository.entity.CityEntity;

public interface CityRepository {
    CityEntity findNameById(Long id);
}
