package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.ContactInfoDTO;
import com.duong.travelweb.model.dto.ContactRequestDTO;
import com.duong.travelweb.model.entity.ContactMessageEntity;
import com.duong.travelweb.repository.ContactMessageRepository;
import com.duong.travelweb.service.ContactService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ContactServiceImpl implements ContactService {
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
