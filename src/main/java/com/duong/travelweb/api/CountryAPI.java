package com.duong.travelweb.api;

import com.duong.travelweb.model.CountryDTO;

import com.duong.travelweb.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
public class CountryAPI {
    @Autowired
    private CountryService countriesService;
    @GetMapping("/api/countries/")
    public List<CountryDTO> getCountry(@RequestParam Map<String,Object> params,
                                        @RequestParam (required = false) List<String> typeCode){
        List<CountryDTO> results = countriesService.findCountry(params,typeCode);
        return results;
    }
    @DeleteMapping( "/api/countries/{id}/{country}")
    public void deleteCountry(@PathVariable String id,
                              @PathVariable String country){
        System.out.println("delete");
    }

}
