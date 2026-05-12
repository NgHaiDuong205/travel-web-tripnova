package com.duong.travelweb.api;

import com.duong.travelweb.model.CountryDTO;

import com.duong.travelweb.model.CountryRequestDTO;
import com.duong.travelweb.repository.entity.ContinentEntity;
import com.duong.travelweb.repository.entity.CountryEntity;
import com.duong.travelweb.service.CountryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
public class CountryAPI {
    @Autowired
    private CountryService countriesService;
    @PersistenceContext
    private EntityManager entityManager;
    @GetMapping("/api/countries/")
    public List<CountryDTO> getCountry(@RequestParam Map<String,Object> params,
                                        @RequestParam (required = false) List<String> typeCode){
        List<CountryDTO> results = countriesService.findCountry(params,typeCode);
        return results;
    }
    @PostMapping("/api/countries/")
    @Transactional
    public void createCountry(@RequestBody CountryRequestDTO countryRequestDTO){
        CountryEntity countryEntity = new CountryEntity();
        countryEntity.setId(countryRequestDTO.getId());
        countryEntity.setCountryName(countryRequestDTO.getCountryName());
        countryEntity.setFlag(countryRequestDTO.getFlag());
        ContinentEntity continentEntity = new ContinentEntity();
        continentEntity.setId(countryRequestDTO.getContinentId());
        countryEntity.setContinent(continentEntity);
        entityManager.persist(countryEntity);
        System.out.println("create");
    }
    @DeleteMapping( "/api/countries/{id}/")
    @Transactional
    public void deleteCountry(@PathVariable String id){
        CountryEntity countryEntity = entityManager.find(CountryEntity.class,id);
        entityManager.remove(countryEntity);
        System.out.println("delete");
    }
}
