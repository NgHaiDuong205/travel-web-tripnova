package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.DashboardDTO;
import com.duong.travelweb.model.dto.ProfileUpdateRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;

import java.util.UUID;

public interface UserService {
    UserDTO getProfile(UUID userId);
    UserDTO updateProfile(UUID userId, ProfileUpdateRequestDTO request);
    DashboardDTO getDashboard(UUID userId);
}
