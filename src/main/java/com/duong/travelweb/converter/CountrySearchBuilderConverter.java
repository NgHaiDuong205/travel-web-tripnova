package com.duong.travelweb.converter;

import com.duong.travelweb.util.MapUtil;
import com.duong.travelweb.builder.CountrySearchBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CountrySearchBuilderConverter {
    public CountrySearchBuilder toCountrySearchBuilder(Map<String,Object> params , List<String> typeCode){
        CountrySearchBuilder countrySearchBuilder = new CountrySearchBuilder.Builder()
                .name(MapUtil.getObject(params,"name",String.class))
                .id(MapUtil.getObject(params,"id",UUID.class))
                .countryCode(MapUtil.getObject(params,"country_code",String.class))
                .slug(MapUtil.getObject(params,"slug",String.class))
                .continentId(MapUtil.getObject(params,"continent_id",UUID.class))
                .typeCode(typeCode)
                .build();
        return countrySearchBuilder;
    }
}
