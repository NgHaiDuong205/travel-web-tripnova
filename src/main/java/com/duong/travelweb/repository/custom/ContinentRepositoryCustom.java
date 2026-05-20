package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.ContinentEntity;
import java.util.UUID;

public interface ContinentRepositoryCustom {
    ContinentEntity findNameById(UUID id);
}
