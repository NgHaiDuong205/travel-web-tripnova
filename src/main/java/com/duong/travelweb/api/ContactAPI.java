package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.ContactInfoDTO;
import com.duong.travelweb.model.dto.ContactRequestDTO;
import com.duong.travelweb.service.ContactService;
import com.duong.travelweb.util.RequestUtil;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactAPI {
    private final ContactService contactService;

    public ContactAPI(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/api/contact/")
    public ResponseEntity<Void> submit(@Valid @RequestBody ContactRequestDTO request, HttpServletRequest httpRequest) {
        contactService.submit(request, SecurityUtil.findCurrentUserId(), RequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/contact/info/")
    public ResponseEntity<ContactInfoDTO> getContactInfo() {
        return ResponseEntity.ok(contactService.getContactInfo());
    }
}
