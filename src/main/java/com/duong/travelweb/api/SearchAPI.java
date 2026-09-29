package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.SearchQueryDTO;
import com.duong.travelweb.model.dto.SearchQueryStatDTO;
import com.duong.travelweb.model.dto.SearchResponseDTO;
import com.duong.travelweb.model.dto.SearchResultDTO;
import com.duong.travelweb.service.SearchService;
import com.duong.travelweb.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tìm kiếm toàn trang (public) và thống kê truy vấn cho admin. */
@RestController
public class SearchAPI {
    private static final int MAX_LIMIT = 50;

    private final SearchService searchService;

    public SearchAPI(SearchService searchService) {
        this.searchService = searchService;
    }

    /** ?q&type=all|destination|hotel|tour|car|flight&page&limit (all: limit mỗi loại, mặc định 5; 1 loại: mặc định 20). */
    @GetMapping("/api/search/")
    public ResponseEntity<SearchResponseDTO> search(@RequestParam(value = "q", required = false) String q,
                                                    @RequestParam(value = "type", defaultValue = "all") String type,
                                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                                    @RequestParam(value = "limit", required = false) Integer limit) {
        int size = limit != null ? limit : ("all".equalsIgnoreCase(type) ? 5 : 20);
        return ResponseEntity.ok(searchService.search(q, type, page, clamp(size), SecurityUtil.findCurrentUserId()));
    }

    @GetMapping("/api/search/suggest/")
    public ResponseEntity<List<SearchResultDTO>> suggest(@RequestParam(value = "q", required = false) String q,
                                                         @RequestParam(value = "limit", defaultValue = "8") int limit) {
        return ResponseEntity.ok(searchService.suggest(q, Math.min(Math.max(limit, 1), 20)));
    }

    /** Body {searchId, entityType, entityId}; public (khách chưa đăng nhập cũng tìm được). */
    @PostMapping("/api/search/click/")
    public ResponseEntity<Void> click(@RequestBody Map<String, String> body) {
        Long searchId;
        UUID entityId;
        try {
            searchId = body.get("searchId") == null ? null : Long.valueOf(body.get("searchId"));
            entityId = body.get("entityId") == null ? null : UUID.fromString(body.get("entityId"));
        } catch (IllegalArgumentException e) {
            searchId = null;
            entityId = null;
        }
        searchService.recordClick(searchId, body.get("entityType"), entityId, SecurityUtil.findCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /** ?userId&q&from&to (yyyy-MM-dd, tính cả 2 đầu); mới nhất trước. */
    @GetMapping("/api/admin/search-queries/")
    public ResponseEntity<List<SearchQueryDTO>> getQueries(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<SearchQueryDTO> result = searchService.findQueries(userId, q, from, to, page, Math.min(Math.max(limit, 1), 100));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    /** Truy vấn phổ biến trong kỳ (mặc định 30 ngày gần nhất). */
    @GetMapping("/api/admin/search-queries/top/")
    public ResponseEntity<List<SearchQueryStatDTO>> getTopQueries(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.topQueries(from, to, Math.min(Math.max(limit, 1), 100)));
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
