package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AdminChatMessageDTO;
import com.duong.travelweb.model.dto.AdminChatSessionDTO;
import com.duong.travelweb.model.dto.KnowledgeDocumentDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quản trị trợ lý AI (A13): kho tài liệu RAG và xem lại các phiên chat. */
public interface AdminAiService {
    Page<KnowledgeDocumentDTO> searchDocuments(String q, String sourceType, String language, Boolean active,
                                               int page, int limit);

    KnowledgeDocumentDTO getDocument(UUID id);

    /** Kiểm tra rồi chuyển tệp sang dịch vụ AI để cắt đoạn + embedding. Trả tài liệu đã lập chỉ mục. */
    KnowledgeDocumentDTO uploadDocument(MultipartFile file, String title, String sourceType, UUID sourceId,
                                        String language, String url);

    KnowledgeDocumentDTO setDocumentActive(UUID id, boolean active);

    void deleteDocument(UUID id);

    /** full = false: chỉ tài liệu lập chỉ mục bằng model embedding khác model hiện tại. */
    Map<String, Object> reindex(boolean full);

    Page<AdminChatSessionDTO> searchSessions(UUID userId, String q, LocalDate from, LocalDate to, int page, int limit);

    List<AdminChatMessageDTO> getSessionMessages(UUID sessionId);
}
