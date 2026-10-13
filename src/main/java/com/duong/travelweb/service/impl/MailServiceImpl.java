package com.duong.travelweb.service.impl;

import com.duong.travelweb.service.MailService;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MailServiceImpl implements MailService {
    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BASE_MILLIS = 2000;

    /** Chỉ có bean khi đặt spring.mail.host (auto-config của Spring Boot). */
    private final JavaMailSender mailSender;
    private final String from;
    private final ExecutorService executor;

    public MailServiceImpl(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.from:TripNova <no-reply@tripnova.local>}") String from) {
        this.mailSender = mailSender.getIfAvailable();
        this.from = from;
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "mail-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        log.info(mailSender.getIfAvailable() != null
                ? "Mail: gửi qua SMTP"
                : "Mail: chưa cấu hình spring.mail.host -> chỉ ghi log nội dung email");
    }

    @Override
    public boolean isSmtpEnabled() {
        return mailSender != null;
    }

    @Override
    public void send(Mail mail) {
        if (mail.to() == null || mail.to().isBlank()) {
            return;
        }
        Runnable task = () -> deliver(mail);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
        } else {
            executor.execute(task);
        }
    }

    private void deliver(Mail mail) {
        List<Rendered> attachments = renderAttachments(mail);
        if (mailSender == null) {
            log.info("[MAIL] To: {} | Subject: {}{}\n{}", mail.to(), mail.subject(),
                    attachments.isEmpty() ? "" : " | Attachments: " + attachments.stream().map(Rendered::filename).toList(),
                    mail.text());
            return;
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mailSender.send(toMime(mail, attachments));
                log.info("Đã gửi email \"{}\" tới {}", mail.subject(), mail.to());
                return;
            } catch (MailAuthenticationException | MailParseException | MessagingException e) {
                // Sai cấu hình / địa chỉ: thử lại cũng không được.
                log.error("Không gửi được email tới {}: {}", mail.to(), e.getMessage());
                return;
            } catch (MailException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Không gửi được email tới {} sau {} lần: {}", mail.to(), MAX_ATTEMPTS, e.getMessage());
                    return;
                }
                log.warn("Gửi email tới {} lỗi (lần {}), thử lại: {}", mail.to(), attempt, e.getMessage());
                if (!sleep(RETRY_BASE_MILLIS << (attempt - 1))) {
                    return;
                }
            }
        }
    }

    private MimeMessage toMime(Mail mail, List<Rendered> attachments) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(mail.to());
        helper.setSubject(mail.subject());
        if (mail.html() != null) {
            helper.setText(mail.text(), mail.html());
        } else {
            helper.setText(mail.text());
        }
        for (Rendered attachment : attachments) {
            helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.content()), attachment.contentType());
        }
        return message;
    }

    /** Tệp đính kèm lỗi khi tạo thì bỏ qua tệp đó, email vẫn được gửi. */
    private List<Rendered> renderAttachments(Mail mail) {
        List<Rendered> result = new ArrayList<>();
        for (Attachment attachment : mail.attachments()) {
            try {
                result.add(new Rendered(attachment.filename(), attachment.contentType(), attachment.content().get()));
            } catch (RuntimeException e) {
                log.error("Không tạo được tệp đính kèm {} cho email tới {}", attachment.filename(), mail.to(), e);
            }
        }
        return result;
    }

    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
            log.warn("Còn email chưa gửi xong khi tắt ứng dụng");
        }
    }

    private record Rendered(String filename, String contentType, byte[] content) {
    }
}
