package com.duong.travelweb.repository;

import com.duong.travelweb.repository.entity.ContinentEntity;

public interface ContinentRepository {
    ContinentEntity findNameById(Long id);
}
