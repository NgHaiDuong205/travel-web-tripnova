package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.CityEntity;

import java.util.UUID;

public interface CityRepositoryCustom {
    CityEntity findNameById(UUID id);
}
