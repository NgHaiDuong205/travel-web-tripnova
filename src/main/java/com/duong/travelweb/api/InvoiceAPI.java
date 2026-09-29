package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.InvoiceDTO;
import com.duong.travelweb.service.InvoiceService;
import com.duong.travelweb.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Hoá đơn của user (/api/me/**) và quản trị hoá đơn (/api/admin/**, ROLE_ADMIN). */
@RestController
public class InvoiceAPI {
    private static final int MAX_LIMIT = 100;

    private final InvoiceService invoiceService;

    public InvoiceAPI(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/api/me/invoices/")
    public ResponseEntity<List<InvoiceDTO>> getMyInvoices(@RequestParam(value = "page", defaultValue = "1") int page,
                                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return toResponse(invoiceService.findMine(SecurityUtil.getCurrentUserId(), page, clamp(limit)));
    }

    @GetMapping("/api/me/invoices/{invoiceId}/")
    public ResponseEntity<InvoiceDTO> getMyInvoice(@PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(invoiceService.getMine(SecurityUtil.getCurrentUserId(), invoiceId));
    }

    @GetMapping("/api/me/invoices/{invoiceId}/download/")
    public ResponseEntity<byte[]> downloadMyInvoice(@PathVariable("invoiceId") UUID invoiceId) {
        return toPdf(invoiceService.downloadMine(SecurityUtil.getCurrentUserId(), invoiceId));
    }

    /** ?q= số hoá đơn / mã order / email / tên khách; from, to = ngày phát hành (yyyy-MM-dd, tính cả 2 đầu). */
    @GetMapping("/api/admin/invoices/")
    public ResponseEntity<List<InvoiceDTO>> getInvoices(@RequestParam(value = "q", required = false) String q,
                                                        @RequestParam(value = "from", required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                        @RequestParam(value = "to", required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        String keyword = q == null || q.isBlank() ? null : q.trim();
        return toResponse(invoiceService.findForAdmin(keyword, from, to, page, clamp(limit)));
    }

    @GetMapping("/api/admin/invoices/{invoiceId}/")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(invoiceService.get(invoiceId));
    }

    @GetMapping("/api/admin/invoices/{invoiceId}/download/")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("invoiceId") UUID invoiceId) {
        return toPdf(invoiceService.download(invoiceId));
    }

    @PostMapping("/api/admin/invoices/{invoiceId}/resend/")
    public ResponseEntity<InvoiceDTO> resendInvoice(@PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(invoiceService.resend(invoiceId));
    }

    private ResponseEntity<byte[]> toPdf(InvoiceService.InvoiceFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }

    private ResponseEntity<List<InvoiceDTO>> toResponse(Page<InvoiceDTO> result) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    private int clamp(int limit) {
        return limit < 1 ? 10 : Math.min(limit, MAX_LIMIT);
    }
}
