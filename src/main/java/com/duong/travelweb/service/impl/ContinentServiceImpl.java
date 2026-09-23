package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.ContinentDTO;
import com.duong.travelweb.model.entity.ContinentEntity;
import com.duong.travelweb.repository.ContinentRepository;
import com.duong.travelweb.service.ContinentService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContinentServiceImpl implements ContinentService {
    private final ContinentRepository continentRepository;

    public ContinentServiceImpl(ContinentRepository continentRepository) {
        this.continentRepository = continentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContinentDTO> findAll() {
        return continentRepository.findAll(Sort.by("name")).stream().map(this::toDTO).toList();
    }

    private ContinentDTO toDTO(ContinentEntity entity) {
        ContinentDTO dto = new ContinentDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        return dto;
    }
}
