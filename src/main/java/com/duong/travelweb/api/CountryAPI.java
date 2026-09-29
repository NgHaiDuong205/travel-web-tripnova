package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.CountryDTO;
import com.duong.travelweb.service.CountryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/countries")
public class CountryAPI {
    
    private final CountryService countryService;

    public CountryAPI(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping("/")
    public ResponseEntity<List<CountryDTO>> getCountry(@RequestParam Map<String,Object> params,
                                        @RequestParam(value = "typeCode", required = false) List<String> typeCode) {
        List<CountryDTO> results = countryService.findCountry(params, typeCode);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/")
    public ResponseEntity<CountryDTO> getCountryById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(countryService.getCountryById(id));
    }
    }
