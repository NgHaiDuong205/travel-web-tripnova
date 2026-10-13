package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.InvoiceBillingRequestDTO;
import com.duong.travelweb.model.dto.InvoiceDTO;
import com.duong.travelweb.model.dto.InvoiceItemDTO;
import com.duong.travelweb.model.entity.InvoiceEntity;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.InvoiceRepository;
import com.duong.travelweb.repository.OrderRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.service.InvoiceService;
import com.duong.travelweb.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InvoiceServiceImpl implements InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);
    private static final String NUMBER_PREFIX = "INV-";

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderBookingRouter orderBookingRouter;
    private final InvoicePdfRenderer pdfRenderer;
    private final MailService mailService;
    private final MailTemplates mailTemplates;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              OrderRepository orderRepository,
                              PaymentRepository paymentRepository,
                              OrderBookingRouter orderBookingRouter,
                              InvoicePdfRenderer pdfRenderer,
                              MailService mailService,
                              MailTemplates mailTemplates) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderBookingRouter = orderBookingRouter;
        this.pdfRenderer = pdfRenderer;
        this.mailService = mailService;
        this.mailTemplates = mailTemplates;
    }

    @Override
    @Transactional
    public void issueForOrder(OrderEntity order, PaymentEntity payment) {
        InvoiceEntity invoice = createInvoice(order, payment);
        if (invoice == null) {
            return;
        }
        // Gửi sau commit; lỗi dựng email không được làm hỏng giao dịch thanh toán.
        try {
            InvoiceDTO dto = toDetailDTO(invoice);
            mailService.send(mailTemplates.invoice(dto, () -> pdfRenderer.render(dto)));
        } catch (RuntimeException e) {
            log.error("Không tạo được email hoá đơn {}", invoice.getInvoiceNumber(), e);
        }
    }

    /** null nếu order đã có hoá đơn. */
    private InvoiceEntity createInvoice(OrderEntity order, PaymentEntity payment) {
        if (invoiceRepository.existsByOrderId(order.getId())) {
            return null;
        }
        UserEntity user = order.getUser();
        LocalDateTime issuedAt = payment != null && payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now();
        InvoiceEntity invoice = new InvoiceEntity();
        // order_code đã unique và 1 order chỉ có 1 hoá đơn -> số hoá đơn suy ra từ mã order, không cần sinh ngẫu nhiên.
        invoice.setInvoiceNumber(NUMBER_PREFIX + order.getOrderCode());
        invoice.setOrder(order);
        invoice.setPayment(payment);
        invoice.setUser(user);
        invoice.setBillingName(user.getFullName());
        invoice.setSubtotal(orZero(order.getSubtotal()));
        invoice.setDiscountTotal(orZero(order.getDiscountTotal()));
        invoice.setTaxAmount(orZero(order.getTaxAmount()));
        invoice.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount()
                : payment != null ? payment.getAmount() : BigDecimal.ZERO);
        invoice.setCurrencyCode(order.getCurrencyCode() != null ? order.getCurrencyCode()
                : payment != null ? payment.getCurrencyCode() : null);
        invoice.setIssuedAt(issuedAt);
        invoice.setCreatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        log.info("Đã phát hành hoá đơn {} cho order {}", invoice.getInvoiceNumber(), order.getOrderCode());
        return invoice;
    }

    @Override
    @Transactional
    public int backfillMissing() {
        List<UUID> orderIds = invoiceRepository.findPaidOrderIdsWithoutInvoice();
        if (orderIds.isEmpty()) {
            return 0;
        }
        Map<UUID, PaymentEntity> paidPayments = new HashMap<>();
        for (PaymentEntity payment : paymentRepository.findByOrderIds(orderIds)) {
            if (payment.getPaidAt() == null) {
                continue;
            }
            paidPayments.merge(payment.getOrder().getId(), payment,
                    (a, b) -> a.getPaidAt().isAfter(b.getPaidAt()) ? a : b);
        }
        int created = 0;
        for (OrderEntity order : orderRepository.findAllById(orderIds)) {
            // Order cũ: chỉ bổ sung hoá đơn, không gửi email.
            if (createInvoice(order, paidPayments.get(order.getId())) != null) {
                created++;
            }
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceDTO> findMine(UUID userId, int page, int limit) {
        Page<InvoiceEntity> invoices = invoiceRepository.findByUserId(userId, PageRequest.of(Math.max(page, 1) - 1, limit));
        Map<UUID, BigDecimal> refunds = refundsByOrder(invoices.getContent());
        return invoices.map(invoice -> toDTO(invoice, refunds.get(invoice.getOrder().getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDTO getMine(UUID userId, UUID invoiceId) {
        return toDetailDTO(findOwned(userId, invoiceId));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceFile downloadMine(UUID userId, UUID invoiceId) {
        return toFile(toDetailDTO(findOwned(userId, invoiceId)));
    }

    @Override
    @Transactional
    public InvoiceDTO updateMyBilling(UUID userId, UUID invoiceId, InvoiceBillingRequestDTO request) {
        InvoiceEntity invoice = findOwned(userId, invoiceId);
        invoice.setBillingName(request.getBillingName().trim());
        invoice.setBillingAddress(blankToNull(request.getBillingAddress()));
        String taxCode = blankToNull(request.getBillingTaxCode());
        invoice.setBillingTaxCode(taxCode == null ? null : taxCode.toUpperCase());
        return toDetailDTO(invoice);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceDTO> findForAdmin(String keyword, LocalDate from, LocalDate to, int page, int limit) {
        if (from != null && to != null && to.isBefore(from)) {
            throw ApiException.badRequest("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        List<InvoiceEntity> invoices = invoiceRepository.findForAdmin(keyword, from, to, page, limit);
        long total = invoiceRepository.countForAdmin(keyword, from, to);
        Map<UUID, BigDecimal> refunds = refundsByOrder(invoices);
        List<InvoiceDTO> dtos = invoices.stream()
                .map(invoice -> toDTO(invoice, refunds.get(invoice.getOrder().getId())))
                .toList();
        return new PageImpl<>(dtos, PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDTO get(UUID invoiceId) {
        return toDetailDTO(findInvoice(invoiceId));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceFile download(UUID invoiceId) {
        return toFile(toDetailDTO(findInvoice(invoiceId)));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDTO resend(UUID invoiceId) {
        InvoiceDTO invoice = toDetailDTO(findInvoice(invoiceId));
        mailService.send(mailTemplates.invoice(invoice, () -> pdfRenderer.render(invoice)));
        return invoice;
    }

    private InvoiceEntity findInvoice(UUID invoiceId) {
        return invoiceRepository.findDetailById(invoiceId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hoá đơn"));
    }

    private InvoiceEntity findOwned(UUID userId, UUID invoiceId) {
        return invoiceRepository.findDetailById(invoiceId)
                .filter(invoice -> invoice.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hoá đơn"));
    }

    private InvoiceFile toFile(InvoiceDTO invoice) {
        return new InvoiceFile(invoice.getInvoiceNumber() + ".pdf", pdfRenderer.render(invoice));
    }

    private Map<UUID, BigDecimal> refundsByOrder(List<InvoiceEntity> invoices) {
        return orderBookingRouter.refundedAmounts(invoices.stream().map(i -> i.getOrder().getId()).distinct().toList());
    }

    private InvoiceDTO toDTO(InvoiceEntity invoice, BigDecimal refunded) {
        OrderEntity order = invoice.getOrder();
        UserEntity user = invoice.getUser();
        PaymentEntity payment = invoice.getPayment();
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderStatus(order.getStatus());
        if (payment != null) {
            dto.setPaymentId(payment.getId());
            dto.setPaymentMethod(payment.getPaymentMethod());
            dto.setTransactionId(payment.getTransactionId());
            dto.setPaidAt(payment.getPaidAt());
        }
        dto.setUserId(user.getId());
        dto.setCustomerName(user.getFullName());
        dto.setCustomerEmail(user.getEmail());
        dto.setBillingName(invoice.getBillingName());
        dto.setBillingAddress(invoice.getBillingAddress());
        dto.setBillingTaxCode(invoice.getBillingTaxCode());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setDiscountTotal(orZero(invoice.getDiscountTotal()));
        dto.setTaxAmount(orZero(invoice.getTaxAmount()));
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setRefundedAmount(orZero(refunded));
        dto.setCurrencyCode(invoice.getCurrencyCode() == null ? null : invoice.getCurrencyCode().trim());
        dto.setIssuedAt(invoice.getIssuedAt());
        return dto;
    }

    private InvoiceDTO toDetailDTO(InvoiceEntity invoice) {
        List<InvoiceItemDTO> items = orderBookingRouter.handlerFor(invoice.getOrder()).invoiceItems(invoice.getOrder().getId());
        BigDecimal refunded = items.stream()
                .map(item -> orZero(item.getRefundAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        InvoiceDTO dto = toDTO(invoice, refunded);
        dto.setItems(items);
        return dto;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
