package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.FlightSearchBuilder;
import com.duong.travelweb.model.entity.FlightEntity;

import java.util.List;

public interface FlightRepositoryCustom {
    List<FlightEntity> findFlights(FlightSearchBuilder criteria, int page, int limit);

    long countFlights(FlightSearchBuilder criteria);
}
