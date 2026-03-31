package com.duong.travelweb.api;

import com.duong.travelweb.model.CountryDTO;

import com.duong.travelweb.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class CountryAPI {
    @Autowired
    private CountryService countriesService;
    @GetMapping("/api/countries/")
    public List<CountryDTO> getCountry(@RequestParam(name="name") String countryName){
        List<CountryDTO> results = countriesService.findCountry(countryName);
        return results;
    }
//    @PostMapping("/api/countries/")
//    public Object getCountry2(@RequestBody CountryDTO countries){
//        validDate(countries);
//        return null;
//    }
//    public void validDate(CountryDTO countryDTO){
//        if(countryDTO.getCountry() == null || countryDTO.getCountry().equals("")){
//            throw  new FieldRequireException("name is null");
//        }
//    }
    @DeleteMapping( "/api/countries/{id}/{country}")
    public void deleteCountry(@PathVariable String id,
                              @PathVariable String country){
        System.out.println("delete");
    }

}
