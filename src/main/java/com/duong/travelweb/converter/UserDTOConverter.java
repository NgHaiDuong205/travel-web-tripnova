package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserDTOConverter {

    public UserDTO toUserDTO(UserEntity entity, List<String> roles) {
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setFullName(entity.getFullName());
        dto.setPhone(entity.getPhone());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setGender(entity.getGender());
        dto.setLoyaltyPoints(entity.getLoyaltyPoints());
        dto.setIsVerified(entity.getIsVerified());
        dto.setRoles(roles != null ? roles : new ArrayList<>());
        return dto;
    }
}
