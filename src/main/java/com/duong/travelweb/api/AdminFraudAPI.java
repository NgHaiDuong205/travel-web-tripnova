package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.FraudScoreDTO;
import com.duong.travelweb.service.FraudService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A13: điểm rủi ro gian lận của giao dịch (ROLE_ADMIN). */
@RestController
public class AdminFraudAPI {
    private static final int MAX_LIMIT = 100;

    private final FraudService fraudService;

    public AdminFraudAPI(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    /** ?level=low|medium|high&action=allow|review|cleared|confirmed_fraud&q (mã đơn / email / tên)&page&limit. */
    @GetMapping("/api/admin/ai/fraud-scores/")
    public ResponseEntity<List<FraudScoreDTO>> search(@RequestParam(value = "level", required = false) String level,
                                                      @RequestParam(value = "action", required = false) String action,
                                                      @RequestParam(value = "q", required = false) String q,
                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                      @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<FraudScoreDTO> result = fraudService.search(level, action, q, page, Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    /** {low, medium, high, toReview} */
    @GetMapping("/api/admin/ai/fraud-scores/stats/")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(fraudService.stats());
    }

    @GetMapping("/api/admin/ai/fraud-scores/{id}/")
    public ResponseEntity<FraudScoreDTO> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(fraudService.get(id));
    }

    /** {actionTaken: review | cleared | confirmed_fraud} */
    @PutMapping("/api/admin/ai/fraud-scores/{id}/")
    public ResponseEntity<FraudScoreDTO> updateAction(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(fraudService.updateAction(id, body == null ? null : body.get("actionTaken")));
    }

    /** Chấm bù giao dịch chưa có điểm (tối đa 500, mới nhất trước) → {scored}. */
    @PostMapping("/api/admin/ai/fraud-scores/backfill/")
    public ResponseEntity<Map<String, Integer>> backfill() {
        return ResponseEntity.ok(Map.of("scored", fraudService.backfill(500)));
    }
}
