package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.ContactInfoDTO;
import com.duong.travelweb.model.dto.ContactMessageDTO;
import com.duong.travelweb.model.dto.ContactRequestDTO;
import com.duong.travelweb.model.entity.ContactMessageEntity;
import com.duong.travelweb.repository.ContactMessageRepository;
import com.duong.travelweb.service.ContactService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ContactServiceImpl implements ContactService {
    private static final List<String> STATUSES = List.of("new", "in_progress", "resolved", "spam", "closed");

    private final ContactMessageRepository contactMessageRepository;
    private final ContactInfoDTO contactInfo;

    public ContactServiceImpl(ContactMessageRepository contactMessageRepository,
                              @Value("${app.contact.email:support@tripnova.local}") String email,
                              @Value("${app.contact.phone:}") String phone,
                              @Value("${app.contact.address:}") String address,
                              @Value("${app.contact.working-hours:}") String workingHours) {
        this.contactMessageRepository = contactMessageRepository;
        this.contactInfo = new ContactInfoDTO();
        this.contactInfo.setEmail(email);
        this.contactInfo.setPhone(phone);
        this.contactInfo.setAddress(address);
        this.contactInfo.setWorkingHours(workingHours);
    }

    @Override
    @Transactional
    public void submit(ContactRequestDTO request, UUID userId, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        ContactMessageEntity message = new ContactMessageEntity();
        message.setUserId(userId);
        message.setFullName(request.getFullName().trim());
        message.setEmail(request.getEmail().trim());
        message.setPhone(blankToNull(request.getPhone()));
        message.setSubject(blankToNull(request.getSubject()));
        message.setMessage(request.getMessage().trim());
        message.setStatus("new");
        message.setIpAddress(ipAddress);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        contactMessageRepository.save(message);
    }

    @Override
    public ContactInfoDTO getContactInfo() {
        return contactInfo;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactMessageDTO> findMessages(String status, int page, int limit) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, limit);
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : validateStatus(status);
        Page<ContactMessageEntity> messages = filter == null
                ? contactMessageRepository.findAllOrdered(pageable)
                : contactMessageRepository.findByStatus(filter, pageable);
        return messages.map(this::toDTO);
    }

    @Override
    @Transactional
    public ContactMessageDTO updateStatus(UUID messageId, String status) {
        ContactMessageEntity message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tin nhắn liên hệ"));
        message.setStatus(validateStatus(status));
        message.setUpdatedAt(LocalDateTime.now());
        return toDTO(message);
    }

    private String validateStatus(String status) {
        if (!STATUSES.contains(status)) {
            throw ApiException.badRequest("Trạng thái không hợp lệ: " + status);
        }
        return status;
    }

    private ContactMessageDTO toDTO(ContactMessageEntity entity) {
        ContactMessageDTO dto = new ContactMessageDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setSubject(entity.getSubject());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
