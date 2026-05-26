package com.duong.travelweb.converter;

import com.duong.travelweb.util.MapUtil;
import com.duong.travelweb.builder.HotelSearchBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HotelSearchBuilderConverter {
    public HotelSearchBuilder toHotelSearchBuilder(Map<String,Object> params , List<String> amenities){
        Integer minStarRating = MapUtil.getObject(params, "minStarRating", Integer.class);
        if (minStarRating == null) {
            minStarRating = MapUtil.getObject(params, "min_star_rating", Integer.class);
        }

        Integer maxStarRating = MapUtil.getObject(params, "maxStarRating", Integer.class);
        if (maxStarRating == null) {
            maxStarRating = MapUtil.getObject(params, "max_star_rating", Integer.class);
        }

        Integer totalRooms = MapUtil.getObject(params, "totalRooms", Integer.class);
        if (totalRooms == null) {
            totalRooms = MapUtil.getObject(params, "total_rooms", Integer.class);
        }

        String countryName = MapUtil.getObject(params, "countryName", String.class);
        if (countryName == null) {
            countryName = MapUtil.getObject(params, "country_name", String.class);
        }

        String destinationName = MapUtil.getObject(params, "destinationName", String.class);
        if (destinationName == null) {
            destinationName = MapUtil.getObject(params, "destination_name", String.class);
        }

        UUID destinationId = MapUtil.getObject(params, "destinationId", UUID.class);
        if (destinationId == null) {
            destinationId = MapUtil.getObject(params, "destination_id", UUID.class);
        }

        UUID managedById = MapUtil.getObject(params, "managedById", UUID.class);
        if (managedById == null) {
            managedById = MapUtil.getObject(params, "managed_by_id", UUID.class);
        }

        Integer starRating = MapUtil.getObject(params, "starRating", Integer.class);
        if (starRating == null) {
            starRating = MapUtil.getObject(params, "star_rating", Integer.class);
        }

        Integer page = MapUtil.getObject(params, "page", Integer.class);
        Integer limit = MapUtil.getObject(params, "limit", Integer.class);
        if (limit == null) {
            limit = MapUtil.getObject(params, "size", Integer.class);
        }
        System.out.println("--- HotelSearchBuilderConverter ---");
        System.out.println("Raw params map: " + params);
        System.out.println("Parsed page: " + page + ", limit: " + limit);

        HotelSearchBuilder hotelSearchBuilder = new HotelSearchBuilder.Builder()
                .id(MapUtil.getObject(params,"id",UUID.class))
                .destinationId(destinationId)
                .managedById(managedById)
                .name(MapUtil.getObject(params,"name",String.class))
                .starRating(starRating)
                .minStarRating(minStarRating)
                .maxStarRating(maxStarRating)
                .totalRooms(totalRooms)
                .countryName(countryName)
                .destinationName(destinationName)
                .amenities(amenities)
                .page(page)
                .limit(limit)
                .build();
        return hotelSearchBuilder;
    }
}
