package com.duong.travelweb.converter;

import com.duong.travelweb.StringUtil.MapUtil;
import com.duong.travelweb.builder.CountrySearchBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class CountrySearchBuiderConverter {
    public CountrySearchBuilder toCountrySearchBuilder(Map<String,Object> params , List<String> typeCode){
        CountrySearchBuilder countrySearchBuilder = new CountrySearchBuilder.Builder()
                                                                            .name(MapUtil.getObject(params,"name",String.class))
                                                                            .id(MapUtil.getObject(params,"id",String.class))
                                                                            .flag(MapUtil.getObject(params,"flag",String.class))
                                                                            .continentId(MapUtil.getObject(params,"continent_id",Integer.class))
                                                                            .typeCode(typeCode)
                                                                            .build();
        return countrySearchBuilder;
    }
}
