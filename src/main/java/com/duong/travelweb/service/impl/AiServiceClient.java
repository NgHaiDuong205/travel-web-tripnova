package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gọi dịch vụ AI nội bộ (Python, D:\travel_chatbot_python) qua HTTP + header X-Internal-Key.
 * Lỗi từ dịch vụ được đổi thành ApiException: 503 khi chưa cấu hình / không kết nối được / hết hạn mức,
 * 502 khi dịch vụ lỗi, giữ nguyên 4xx (dữ liệu không hợp lệ, không tìm thấy).
 */
@Component
public class AiServiceClient {
    /** Mã lỗi để lưu vào chat_messages.error_message. */
    public static final String ERR_UNAVAILABLE = "ai_unavailable";
    public static final String ERR_QUOTA = "ai_quota";
    public static final String ERR_NOT_CONFIGURED = "not_configured";

    private final RestClient restClient;
    private final String internalKey;
    private final ObjectMapper objectMapper;

    public AiServiceClient(@Value("${app.ai.service-url:http://127.0.0.1:8000}") String serviceUrl,
                           @Value("${app.ai.internal-key:${AI_INTERNAL_KEY:}}") String internalKey,
                           @Value("${app.ai.timeout-seconds:90}") int timeoutSeconds,
                           ObjectMapper objectMapper) {
        // HttpClient của JDK (HttpURLConnection không hỗ trợ PATCH).
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(serviceUrl).requestFactory(factory).build();
        this.internalKey = internalKey == null ? "" : internalKey.trim();
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return internalKey.length() >= 32;
    }

    /** history: [{role: user|assistant, content}], context: {type, id} hoặc null. */
    public JsonNode chat(String question, List<Map<String, String>> history, String locale, Map<String, Object> context) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("question", question);
        body.put("history", history);
        body.put("locale", locale);
        if (context != null) {
            body.put("context", context);
        }
        return call(() -> restClient.post().uri("/internal/chat")
                .header("X-Internal-Key", internalKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(String.class));
    }

    public JsonNode uploadDocument(String filename, byte[] content, String title, String sourceType, UUID sourceId,
                                   String language, String url) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        parts.add("title", title == null ? "" : title);
        parts.add("sourceType", sourceType);
        parts.add("sourceId", sourceId == null ? "" : sourceId.toString());
        parts.add("language", language);
        parts.add("url", url == null ? "" : url);
        return call(() -> restClient.post().uri("/internal/documents")
                .header("X-Internal-Key", internalKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve().body(String.class));
    }

    public void setDocumentActive(UUID id, boolean active) {
        call(() -> restClient.patch().uri("/internal/documents/{id}", id)
                .header("X-Internal-Key", internalKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("isActive", active))
                .retrieve().body(String.class));
    }

    public void deleteDocument(UUID id) {
        call(() -> {
            restClient.delete().uri("/internal/documents/{id}", id)
                    .header("X-Internal-Key", internalKey)
                    .retrieve().toBodilessEntity();
            return null;
        });
    }

    public JsonNode reindex(boolean full) {
        return call(() -> restClient.post().uri("/internal/documents/reindex?full={full}", full)
                .header("X-Internal-Key", internalKey)
                .retrieve().body(String.class));
    }

    private JsonNode call(java.util.function.Supplier<String> request) {
        if (!isConfigured()) {
            throw new AiException(HttpStatus.SERVICE_UNAVAILABLE, ERR_NOT_CONFIGURED,
                    "Trợ lý AI chưa được cấu hình (app.ai.internal-key).");
        }
        String raw;
        try {
            raw = request.get();
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new AiException(HttpStatus.SERVICE_UNAVAILABLE, ERR_UNAVAILABLE,
                    "Trợ lý AI tạm thời không phản hồi, vui lòng thử lại sau.");
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return objectMapper.readTree(raw);
    }

    private AiException translate(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String code = ERR_UNAVAILABLE;
        String message = null;
        try {
            JsonNode node = objectMapper.readTree(ex.getResponseBodyAsString());
            if (node != null && node.hasNonNull("code")) {
                code = node.get("code").asString();
            }
            if (node != null && node.hasNonNull("message")) {
                message = node.get("message").asString();
            }
        } catch (RuntimeException ignored) {
            // thân lỗi không phải JSON
        }
        if (status == 400 || status == 404 || status == 422) {
            return new AiException(HttpStatus.valueOf(status), code, message != null ? message : "Yêu cầu không hợp lệ.");
        }
        if (ERR_QUOTA.equals(code)) {
            return new AiException(HttpStatus.SERVICE_UNAVAILABLE, ERR_QUOTA,
                    "Trợ lý AI tạm hết hạn mức, vui lòng thử lại sau ít phút.");
        }
        if (status == 401 || status == 503) {
            // 401: khoá nội bộ không khớp -> lỗi cấu hình phía máy chủ, không lộ chi tiết cho khách.
            return new AiException(HttpStatus.SERVICE_UNAVAILABLE, status == 401 ? ERR_NOT_CONFIGURED : code,
                    "Trợ lý AI tạm thời không khả dụng.");
        }
        return new AiException(HttpStatus.BAD_GATEWAY, code, "Trợ lý AI gặp lỗi, vui lòng thử lại.");
    }

    /** ApiException kèm mã lỗi máy đọc được (lưu vào tin nhắn lỗi). */
    public static class AiException extends ApiException {
        private final String code;

        public AiException(HttpStatus status, String code, String message) {
            super(status, message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
