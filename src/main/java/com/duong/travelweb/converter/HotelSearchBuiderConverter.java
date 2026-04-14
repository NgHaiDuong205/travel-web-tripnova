package com.duong.travelweb.converter;

import com.duong.travelweb.StringUtil.MapUtil;
import com.duong.travelweb.builder.HotelSearchBuider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class HotelSearchBuiderConverter {
    public HotelSearchBuider toHotelSearchBuider(Map<String,Object> params , List<String> typeCode){
        HotelSearchBuider hotelSearchBuider = new HotelSearchBuider.Buider()
                .id(MapUtil.getObject(params,"id",Integer.class))
                .countryId(MapUtil.getObject(params,"country_id",String.class))
                .cityId(MapUtil.getObject(params,"city_id",Integer.class))
                .name(MapUtil.getObject(params,"name",String.class))
                .rating(MapUtil.getObject(params,"rating",Double.class))
                .typeCode(typeCode)
                .build();
        return hotelSearchBuider;
    }
}
