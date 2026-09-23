package com.duong.travelweb.converter;

import com.duong.travelweb.builder.RoomSearchBuilder;
import com.duong.travelweb.util.DateUtil;
import com.duong.travelweb.util.MapUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class RoomSearchBuilderConverter {
    public RoomSearchBuilder toRoomSearchBuilder(Map<String, Object> params, List<String> amenities) {
        RoomSearchBuilder builder = new RoomSearchBuilder();
        builder.setAmenities(amenities);
        builder.setRoomTypeName(MapUtil.getObject(params, "roomTypeName", String.class));
        builder.setMaxOccupancy(MapUtil.getObject(params, "maxOccupancy", Integer.class));
        builder.setMinPrice(MapUtil.getObject(params, "minPrice", Double.class));
        builder.setMaxPrice(MapUtil.getObject(params, "maxPrice", Double.class));

        LocalDate checkIn = DateUtil.parseLocalDate(params.get("checkIn"));
        builder.setCheckIn(checkIn);

        LocalDate checkOut = DateUtil.parseLocalDate(params.get("checkOut"));
        builder.setCheckOut(checkOut);

        return builder;
    }
}
