package com.duong.travelweb.service;

import java.util.List;
import java.util.function.Supplier;

/**
 * Gửi email. Có cấu hình SMTP (spring.mail.host) thì gửi thật, không thì chỉ ghi log (môi trường dev).
 * Gọi trong transaction -> chỉ gửi sau khi commit (rollback thì không gửi). Việc gửi chạy nền, tự thử lại.
 */
public interface MailService {

    /** Nội dung tệp đính kèm được tạo lúc gửi (VD render PDF) để không làm chậm request. */
    record Attachment(String filename, String contentType, Supplier<byte[]> content) {
    }

    record Mail(String to, String subject, String text, String html, List<Attachment> attachments) {
        public Mail(String to, String subject, String text, String html) {
            this(to, subject, text, html, List.of());
        }
    }

    void send(Mail mail);

    /** true nếu đang gửi qua SMTP thật. */
    boolean isSmtpEnabled();
}
