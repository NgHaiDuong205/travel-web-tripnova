package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.ContactInfoDTO;
import com.duong.travelweb.model.dto.ContactMessageDTO;
import com.duong.travelweb.model.dto.ContactRequestDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ContactService {
    /** @param userId null nếu khách chưa đăng nhập */
    void submit(ContactRequestDTO request, UUID userId, String ipAddress);

    ContactInfoDTO getContactInfo();

    // ---- Admin ----
    Page<ContactMessageDTO> findMessages(String status, int page, int limit);

    /** status: new | in_progress | resolved | spam | closed */
    ContactMessageDTO updateStatus(UUID messageId, String status);
}
