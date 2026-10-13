package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.InvoiceDTO;
import com.duong.travelweb.service.MailService.Attachment;
import com.duong.travelweb.service.MailService.Mail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Nội dung các email hệ thống. Bảng users chưa lưu ngôn ngữ ưa thích nên mỗi email gồm 2 phần: tiếng Việt rồi tiếng Anh.
 * HTML dựng từ một layout chung (templates/mail/layout.html), bản text thuần dựng từ cùng dữ liệu.
 */
@Component
public class MailTemplates {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TemplateEngine templateEngine;
    private final String frontendUrl;
    private final String supportEmail;

    public MailTemplates(TemplateEngine templateEngine,
                         @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
                         @Value("${app.contact.email:support@tripnova.local}") String supportEmail) {
        this.templateEngine = templateEngine;
        this.frontendUrl = frontendUrl;
        this.supportEmail = supportEmail;
    }

    public Mail verifyEmail(String to, String name, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        return build(to, "Xác thực email TripNova / Verify your TripNova email", List.of(
                new Section("vi", "Chào " + nameOr(name, "bạn") + ",",
                        List.of("Vui lòng xác thực địa chỉ email để hoàn tất tài khoản TripNova của bạn."),
                        "Xác thực email", link,
                        List.of("Liên kết có hiệu lực trong 24 giờ. Nếu bạn không tạo tài khoản, hãy bỏ qua email này.")),
                new Section("en", "Hi " + nameOr(name, "there") + ",",
                        List.of("Please verify your email address to finish setting up your TripNova account."),
                        "Verify email", link,
                        List.of("This link expires in 24 hours. If you did not create an account, you can ignore this email."))));
    }

    public Mail passwordReset(String to, String name, String token, long ttlMinutes) {
        String link = frontendUrl + "/reset-password?token=" + token;
        return build(to, "Đặt lại mật khẩu TripNova / Reset your TripNova password", List.of(
                new Section("vi", "Chào " + nameOr(name, "bạn") + ",",
                        List.of("Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn."),
                        "Đặt mật khẩu mới", link,
                        List.of("Liên kết có hiệu lực trong " + ttlMinutes + " phút và chỉ dùng được một lần.",
                                "Nếu bạn không yêu cầu, hãy bỏ qua email này — mật khẩu hiện tại vẫn giữ nguyên.")),
                new Section("en", "Hi " + nameOr(name, "there") + ",",
                        List.of("We received a request to reset the password for your account."),
                        "Choose a new password", link,
                        List.of("This link expires in " + ttlMinutes + " minutes and can only be used once.",
                                "If you did not request this, ignore this email — your password will not change."))));
    }

    /** Gửi tới email CŨ để chủ tài khoản biết nếu không phải họ đổi. */
    public Mail emailChanged(String oldEmail, String name, String newEmail) {
        return build(oldEmail, "Email tài khoản TripNova đã thay đổi / Your TripNova email was changed", List.of(
                new Section("vi", "Chào " + nameOr(name, "bạn") + ",",
                        List.of("Email đăng nhập của tài khoản TripNova vừa được đổi sang " + newEmail + "."),
                        null, null,
                        List.of("Nếu không phải bạn thực hiện, hãy liên hệ " + supportEmail + " ngay.")),
                new Section("en", "Hi " + nameOr(name, "there") + ",",
                        List.of("The sign-in email of your TripNova account was just changed to " + newEmail + "."),
                        null, null,
                        List.of("If this wasn't you, contact " + supportEmail + " immediately."))));
    }

    /** Nội dung trả lời do admin viết (một ngôn ngữ), phần chào/ghi chú song ngữ. */
    public Mail contactReply(String to, String name, String subject, String reply, String originalMessage) {
        String title = subject != null && !subject.isBlank() ? subject : "Liên hệ TripNova / TripNova contact";
        List<String> notes = new ArrayList<>();
        if (originalMessage != null && !originalMessage.isBlank()) {
            notes.add("Tin nhắn của bạn / Your message: \"" + abbreviate(originalMessage.trim(), 500) + "\"");
        }
        notes.add("Bạn có thể trả lời trực tiếp email này. / You can reply to this email directly.");
        return build(to, "Re: " + title, List.of(
                new Section(null, "Chào " + nameOr(name, "bạn") + " / Hi " + nameOr(name, "there") + ",",
                        paragraphs(reply), null, null, notes)));
    }

    public Mail invoice(InvoiceDTO invoice, Supplier<byte[]> pdf) {
        String link = frontendUrl + "/invoices";
        String amount = money(invoice.getTotalAmount(), invoice.getCurrencyCode());
        String issued = invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(DATE_TIME) : "";
        String name = invoice.getBillingName() != null ? invoice.getBillingName() : invoice.getCustomerName();
        Mail mail = build(invoice.getCustomerEmail(),
                "Hoá đơn " + invoice.getInvoiceNumber() + " / Invoice " + invoice.getInvoiceNumber(), List.of(
                        new Section("vi", "Chào " + nameOr(name, "bạn") + ",",
                                List.of("Cảm ơn bạn đã đặt dịch vụ tại TripNova. Hoá đơn của đơn hàng " + invoice.getOrderCode()
                                        + " được đính kèm (PDF).",
                                        "Số hoá đơn: " + invoice.getInvoiceNumber() + " · Ngày phát hành: " + issued
                                                + " · Tổng tiền: " + amount),
                                "Xem hoá đơn", link, List.of()),
                        new Section("en", "Hi " + nameOr(name, "there") + ",",
                                List.of("Thank you for booking with TripNova. The invoice for order " + invoice.getOrderCode()
                                        + " is attached (PDF).",
                                        "Invoice no.: " + invoice.getInvoiceNumber() + " · Issued: " + issued
                                                + " · Total: " + amount),
                                "View invoice", link, List.of())));
        return new Mail(mail.to(), mail.subject(), mail.text(), mail.html(),
                List.of(new Attachment(invoice.getInvoiceNumber() + ".pdf", "application/pdf", pdf)));
    }

    private Mail build(String to, String subject, List<Section> sections) {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("sections", sections);
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("supportEmail", supportEmail);
        String html = templateEngine.process("mail/layout", context);
        return new Mail(to, subject, toText(sections), html);
    }

    private String toText(List<Section> sections) {
        StringBuilder text = new StringBuilder();
        for (Section section : sections) {
            if (!text.isEmpty()) {
                text.append("\n----------------------------------------\n\n");
            }
            text.append(section.getGreeting()).append("\n\n");
            section.getParagraphs().forEach(p -> text.append(p).append("\n\n"));
            if (section.getButtonUrl() != null) {
                text.append(section.getButtonLabel()).append(": ").append(section.getButtonUrl()).append("\n\n");
            }
            section.getNotes().forEach(n -> text.append(n).append("\n"));
        }
        text.append("\n— TripNova · ").append(frontendUrl).append(" · ").append(supportEmail).append("\n");
        return text.toString();
    }

    private static List<String> paragraphs(String text) {
        return Arrays.stream(text.trim().split("\\R\\s*\\R"))
                .map(String::strip)
                .filter(p -> !p.isEmpty())
                .toList();
    }

    private static String nameOr(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name.trim();
    }

    private static String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "";
        }
        String code = currency != null ? currency : "USD";
        return ("USD".equals(code) ? "$" : "") + String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP))
                + ("USD".equals(code) ? "" : " " + code);
    }

    /** Một khối ngôn ngữ trong email. Getter để Thymeleaf đọc. */
    public static final class Section {
        private final String lang;
        private final String greeting;
        private final List<String> paragraphs;
        private final String buttonLabel;
        private final String buttonUrl;
        private final List<String> notes;

        public Section(String lang, String greeting, List<String> paragraphs, String buttonLabel, String buttonUrl,
                       List<String> notes) {
            this.lang = lang;
            this.greeting = greeting;
            this.paragraphs = paragraphs;
            this.buttonLabel = buttonLabel;
            this.buttonUrl = buttonUrl;
            this.notes = notes;
        }

        public String getLang() {
            return lang;
        }

        public String getGreeting() {
            return greeting;
        }

        public List<String> getParagraphs() {
            return paragraphs;
        }

        public String getButtonLabel() {
            return buttonLabel;
        }

        public String getButtonUrl() {
            return buttonUrl;
        }

        public List<String> getNotes() {
            return notes;
        }
    }
}
