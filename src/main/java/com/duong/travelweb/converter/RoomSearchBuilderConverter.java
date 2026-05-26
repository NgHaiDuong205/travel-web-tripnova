package com.duong.travelweb.converter;

import com.duong.travelweb.builder.RoomSearchBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Component
public class RoomSearchBuilderConverter {
    public RoomSearchBuilder toRoomSearchBuilder(Map<String, Object> params, List<String> amenities) {
        RoomSearchBuilder builder = new RoomSearchBuilder();
        builder.setAmenities(amenities);

        if (params.get("roomTypeName") != null && !((String) params.get("roomTypeName")).trim().isEmpty()) {
            builder.setRoomTypeName((String) params.get("roomTypeName"));
        }

        if (params.get("maxOccupancy") != null) {
            try {
                builder.setMaxOccupancy(Integer.parseInt((String) params.get("maxOccupancy")));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (params.get("checkIn") != null && !((String) params.get("checkIn")).trim().isEmpty()) {
            try {
                builder.setCheckIn(LocalDate.parse((String) params.get("checkIn")));
            } catch (DateTimeParseException e) {
                // ignore
            }
        }

        if (params.get("checkOut") != null && !((String) params.get("checkOut")).trim().isEmpty()) {
            try {
                builder.setCheckOut(LocalDate.parse((String) params.get("checkOut")));
            } catch (DateTimeParseException e) {
                // ignore
            }
        }

        if (params.get("minPrice") != null) {
            try {
                builder.setMinPrice(Double.parseDouble((String) params.get("minPrice")));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (params.get("maxPrice") != null) {
            try {
                builder.setMaxPrice(Double.parseDouble((String) params.get("maxPrice")));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return builder;
    }
}
