package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.ContactInfoDTO;
import com.duong.travelweb.model.dto.ContactRequestDTO;

import java.util.UUID;

public interface ContactService {
    /** @param userId null nếu khách chưa đăng nhập */
    void submit(ContactRequestDTO request, UUID userId, String ipAddress);

    ContactInfoDTO getContactInfo();
}
