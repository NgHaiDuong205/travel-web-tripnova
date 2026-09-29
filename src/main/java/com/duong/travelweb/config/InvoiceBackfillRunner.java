package com.duong.travelweb.config;

import com.duong.travelweb.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Lúc khởi động: phát hành hoá đơn cho các order đã thanh toán trước khi có tính năng hoá đơn (idempotent). */
@Component
public class InvoiceBackfillRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InvoiceBackfillRunner.class);

    private final InvoiceService invoiceService;

    public InvoiceBackfillRunner(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int created = invoiceService.backfillMissing();
            if (created > 0) {
                log.info("Đã bổ sung {} hoá đơn cho order đã thanh toán", created);
            }
        } catch (RuntimeException e) {
            // Không chặn khởi động vì lỗi dữ liệu cũ.
            log.error("Bổ sung hoá đơn thất bại", e);
        }
    }
}
