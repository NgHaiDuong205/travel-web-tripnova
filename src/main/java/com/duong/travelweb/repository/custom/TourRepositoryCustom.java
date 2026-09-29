package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.TourSearchBuilder;
import com.duong.travelweb.model.entity.TourEntity;

import java.util.List;

public interface TourRepositoryCustom {
    /** Kèm sẵn destination + country. */
    List<TourEntity> findTours(TourSearchBuilder criteria, int page, int limit);

    long countTours(TourSearchBuilder criteria);
}
