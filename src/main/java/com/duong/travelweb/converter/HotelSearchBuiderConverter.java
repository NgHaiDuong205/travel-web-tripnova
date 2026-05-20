package com.duong.travelweb.converter;

import com.duong.travelweb.util.MapUtil;
import com.duong.travelweb.builder.HotelSearchBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
@Component
public class HotelSearchBuiderConverter {
    public HotelSearchBuilder toHotelSearchBuider(Map<String,Object> params , List<String> typeCode){
        HotelSearchBuilder hotelSearchBuider = new HotelSearchBuilder.Builder()
                .id(MapUtil.getObject(params,"id",UUID.class))
                .destinationId(MapUtil.getObject(params,"destination_id",UUID.class))
                .managedById(MapUtil.getObject(params,"managed_by_id",UUID.class))
                .name(MapUtil.getObject(params,"name",String.class))
                .starRating(MapUtil.getObject(params,"star_rating",Integer.class))
                .typeCode(typeCode)
                .build();
        return hotelSearchBuider;
    }
}
