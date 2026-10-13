package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.InvoiceDTO;
import com.duong.travelweb.service.MailService.Mail;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailTemplatesTest {
    private final MailTemplates templates = new MailTemplates(engine(), "http://fe.test", "help@tripnova.test");

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void passwordResetHasLinkInBothLanguages() {
        Mail mail = templates.passwordReset("a@b.test", "Nguyễn An", "tok_123", 30);
        assertEquals("a@b.test", mail.to());
        assertTrue(mail.text().contains("Chào Nguyễn An,"));
        assertTrue(mail.text().contains("Hi Nguyễn An,"));
        assertTrue(mail.text().contains("Đặt mật khẩu mới: http://fe.test/reset-password?token=tok_123"));
        assertTrue(mail.text().contains("30 phút") && mail.text().contains("30 minutes"));
        assertTrue(mail.html().contains("href=\"http://fe.test/reset-password?token=tok_123\""));
        assertTrue(mail.html().contains("lang=\"vi\"") && mail.html().contains("lang=\"en\""));
    }

    @Test
    void contactReplyEscapesHtmlAndSplitsParagraphs() {
        Mail mail = templates.contactReply("c@d.test", null, "Hỏi giá", "Dòng 1 <script>x</script>\n\nĐoạn 2", "Tin gốc");
        assertEquals("Re: Hỏi giá", mail.subject());
        assertFalse(mail.html().contains("<script>"));
        assertTrue(mail.html().contains("&lt;script&gt;"));
        assertTrue(mail.text().contains("Dòng 1 <script>x</script>\n\nĐoạn 2"));
        assertTrue(mail.text().contains("\"Tin gốc\""));
        assertTrue(mail.text().startsWith("Chào bạn / Hi there,"));
    }

    @Test
    void invoiceAttachesPdfLazily() {
        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setInvoiceNumber("INV-TN1");
        invoice.setOrderCode("TN1");
        invoice.setCustomerEmail("e@f.test");
        invoice.setCustomerName("Khách");
        invoice.setTotalAmount(new BigDecimal("1234.5"));
        invoice.setCurrencyCode("USD");
        invoice.setIssuedAt(LocalDateTime.of(2026, 10, 13, 9, 30));
        int[] rendered = {0};
        Mail mail = templates.invoice(invoice, () -> {
            rendered[0]++;
            return new byte[]{'%', 'P', 'D', 'F'};
        });
        assertEquals(0, rendered[0], "PDF chỉ được tạo lúc gửi");
        assertEquals("e@f.test", mail.to());
        assertTrue(mail.subject().contains("INV-TN1"));
        assertTrue(mail.text().contains("$1,234.50") && mail.text().contains("13/10/2026 09:30"));
        assertEquals("INV-TN1.pdf", mail.attachments().get(0).filename());
        assertEquals(4, mail.attachments().get(0).content().get().length);
    }
}
