package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.SearchQueryDTO;
import com.duong.travelweb.model.dto.SearchQueryStatDTO;
import com.duong.travelweb.model.dto.SearchResponseDTO;
import com.duong.travelweb.model.dto.SearchResultDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tìm kiếm toàn trang theo từ khoá (không AI): mọi từ phải khớp, xếp hạng theo độ giống tên (pg_trgm). */
public interface SearchService {
    List<String> TYPES = List.of("destination", "hotel", "tour", "car", "flight");

    /**
     * type = all | destination | hotel | tour | car | flight. all: tối đa limit kết quả MỖI loại; 1 loại: phân trang.
     * Trang đầu được ghi vào search_queries (userId có thể null).
     */
    SearchResponseDTO search(String q, String type, int page, int limit, UUID userId);

    /** Gợi ý khi gõ (tên hotel / điểm đến / tour / thành phố có chuyến bay); không ghi log. */
    List<SearchResultDTO> suggest(String q, int limit);

    /** Ghi kết quả user bấm vào (chỉ lần bấm đầu tiên của mỗi lượt tìm). */
    void recordClick(Long searchId, String entityType, UUID entityId, UUID userId);

    Page<SearchQueryDTO> findQueries(UUID userId, String q, LocalDate from, LocalDate to, int page, int limit);

    List<SearchQueryStatDTO> topQueries(LocalDate from, LocalDate to, int limit);
}
