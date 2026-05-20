package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.CountryDTO;
import com.duong.travelweb.model.dto.CountryRequestDTO;
import com.duong.travelweb.service.CountryService;
import org.springframework.http.HttpStatus;
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
                                        @RequestParam (required = false) List<String> typeCode) {
        List<CountryDTO> results = countryService.findCountry(params, typeCode);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/")
    public ResponseEntity<CountryDTO> getCountryById(@PathVariable UUID id) {
        CountryDTO result = countryService.getCountryById(id);
        if (result != null && result.getId() != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/")
    public ResponseEntity<Void> createCountry(@RequestBody CountryRequestDTO countryRequestDTO) {
        countryService.createCountry(countryRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @DeleteMapping("/{id}/")
    public ResponseEntity<Void> deleteCountry(@PathVariable UUID id) {
        countryService.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }
}
