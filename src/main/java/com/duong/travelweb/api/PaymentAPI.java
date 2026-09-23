package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.MockPaymentRequestDTO;
import com.duong.travelweb.model.dto.PaymentDTO;
import com.duong.travelweb.service.PaymentService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class PaymentAPI {
    private final PaymentService paymentService;

    public PaymentAPI(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/payments/{paymentId}/")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(SecurityUtil.getCurrentUserId(), paymentId));
    }

    @PostMapping("/api/payments/{paymentId}/mock-confirm/")
    public ResponseEntity<PaymentDTO> mockConfirm(@PathVariable("paymentId") UUID paymentId,
                                                  @Valid @RequestBody MockPaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.mockPayment(SecurityUtil.getCurrentUserId(), paymentId, request.getSuccess()));
    }
}
