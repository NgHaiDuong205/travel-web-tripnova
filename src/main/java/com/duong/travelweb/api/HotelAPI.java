package com.duong.travelweb.api;

import com.duong.travelweb.model.HotelDTO;
import com.duong.travelweb.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class HotelAPI {
    @Autowired
    private HotelService hotelService;
    @GetMapping("/api/hotels/")
    public List<HotelDTO> getHotel (@RequestParam Map<String,Object> params,
                                    @RequestParam(required = false) List<String> typeCode){
        List<HotelDTO> results = hotelService.findHotel(params,typeCode);
        return results;
    }
}
