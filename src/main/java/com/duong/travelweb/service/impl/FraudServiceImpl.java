package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.FraudScoreDTO;
import com.duong.travelweb.service.FraudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quy tắc (mốc = lúc tạo giao dịch), mỗi quy tắc một trọng số w, điểm = 1 − ∏(1 − w) (noisy-OR, 0..1):
 * tài khoản mới (< 1 giờ 0.25, < 24 giờ 0.15) · số tiền lớn (≥ high-amount 0.2, ≥ very-high-amount 0.35) ·
 * tăng đột biến (≥ 2 đơn đã trả trước đó và gấp ≥ 5 lần trung bình 0.2) · đặt dồn dập (≥ 4 đơn / giờ 0.25, ≥ 8 0.45) ·
 * thanh toán lỗi nhiều (≥ 2 / 24 giờ 0.25, ≥ 5 0.45) · huỷ / bỏ đơn nhiều (≥ 3 / 7 ngày 0.15, ≥ 6 0.3) ·
 * email chưa xác thực 0.1 · cùng IP công khai với ≥ 4 tài khoản / 24 giờ 0.2.
 * Mức: < 0.3 low (allow) · < 0.6 medium · còn lại high (review). Không tự chặn khách.
 */
@Service
public class FraudServiceImpl implements FraudService {
    private static final String MODEL_VERSION = "rules-v1";
    private static final String SELECT = "SELECT f.id, f.entity_id, o.id AS order_id, o.order_code, CAST(o.status AS text), "
            + "CAST(p.status AS text), p.amount, CAST(p.currency_code AS text), u.id AS user_id, u.email, u.full_name, "
            + "f.risk_score, f.risk_level, CAST(f.reasons AS text), f.action_taken, f.model_version, f.created_at "
            + "FROM fraud_scores f JOIN payments p ON p.id = f.entity_id JOIN orders o ON o.id = p.order_id "
            + "JOIN users u ON u.id = o.user_id ";
    private static final String WHERE = "WHERE f.entity_type = 'payment' "
            + "AND (CAST(:level AS text) IS NULL OR f.risk_level = CAST(:level AS text)) "
            + "AND (CAST(:action AS text) IS NULL OR f.action_taken = CAST(:action AS text)) "
            + "AND (CAST(:q AS text) IS NULL OR o.order_code ILIKE '%' || CAST(:q AS text) || '%' "
            + "OR u.email ILIKE '%' || CAST(:q AS text) || '%' OR u.full_name ILIKE '%' || CAST(:q AS text) || '%') ";

    private final ObjectMapper objectMapper;
    private final BigDecimal highAmount;
    private final BigDecimal veryHighAmount;
    @PersistenceContext
    private EntityManager entityManager;

    public FraudServiceImpl(ObjectMapper objectMapper,
                            @Value("${app.fraud.high-amount:1500}") BigDecimal highAmount,
                            @Value("${app.fraud.very-high-amount:5000}") BigDecimal veryHighAmount) {
        this.objectMapper = objectMapper;
        this.highAmount = highAmount;
        this.veryHighAmount = veryHighAmount;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FraudScoreDTO scorePayment(UUID paymentId, String clientIp) {
        entityManager.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(CAST(:k AS text)))) x")
                .setParameter("k", "fraud:" + paymentId)
                .getResultList();
        FraudScoreDTO existing = findByPayment(paymentId);
        if (existing != null) {
            return existing;
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("SELECT p.amount, "
                        + "EXTRACT(EPOCH FROM (p.created_at - u.created_at)) / 3600.0, u.is_verified, "
                        + "(SELECT COUNT(*) FROM orders o2 WHERE o2.user_id = u.id "
                        + "AND o2.created_at BETWEEN p.created_at - interval '1 hour' AND p.created_at), "
                        + "(SELECT COUNT(*) FROM payments p2 JOIN orders o3 ON o3.id = p2.order_id WHERE o3.user_id = u.id "
                        + "AND p2.id <> p.id AND p2.status = 'failed' "
                        + "AND p2.created_at BETWEEN p.created_at - interval '24 hours' AND p.created_at), "
                        + "(SELECT COUNT(*) FROM orders o4 WHERE o4.user_id = u.id AND o4.id <> o.id AND o4.status = 'cancelled' "
                        + "AND o4.created_at BETWEEN p.created_at - interval '7 days' AND p.created_at), "
                        + "(SELECT COUNT(*) FROM orders o5 WHERE o5.user_id = u.id AND o5.status IN ('paid', 'partially_refunded') "
                        + "AND o5.created_at < p.created_at), "
                        + "(SELECT AVG(o6.total_amount) FROM orders o6 WHERE o6.user_id = u.id "
                        + "AND o6.status IN ('paid', 'partially_refunded') AND o6.created_at < p.created_at), "
                        + "p.created_at "
                        + "FROM payments p JOIN orders o ON o.id = p.order_id JOIN users u ON u.id = o.user_id WHERE p.id = :p")
                .setParameter("p", paymentId)
                .getResultList();
        if (rows.isEmpty()) {
            throw ApiException.notFound("Không tìm thấy giao dịch thanh toán");
        }
        Object[] r = rows.get(0);
        BigDecimal amount = r[0] == null ? BigDecimal.ZERO : (BigDecimal) r[0];
        double accountAgeHours = r[1] == null ? Double.MAX_VALUE : ((Number) r[1]).doubleValue();
        boolean verified = Boolean.TRUE.equals(r[2]);
        long orders1h = ((Number) r[3]).longValue();
        long failed24h = ((Number) r[4]).longValue();
        long cancelled7d = ((Number) r[5]).longValue();
        long paidBefore = ((Number) r[6]).longValue();
        BigDecimal avgPaid = r[7] == null ? null : new BigDecimal(r[7].toString());

        List<Map<String, Object>> reasons = new ArrayList<>();
        if (accountAgeHours < 1) {
            reasons.add(reason("new_account", 0.25, round(accountAgeHours * 60) + " min"));
        } else if (accountAgeHours < 24) {
            reasons.add(reason("new_account", 0.15, round(accountAgeHours) + " h"));
        }
        if (amount.compareTo(veryHighAmount) >= 0) {
            reasons.add(reason("high_amount", 0.35, amount.toPlainString()));
        } else if (amount.compareTo(highAmount) >= 0) {
            reasons.add(reason("high_amount", 0.2, amount.toPlainString()));
        }
        if (paidBefore >= 2 && avgPaid != null && avgPaid.signum() > 0
                && amount.compareTo(avgPaid.multiply(BigDecimal.valueOf(5))) >= 0) {
            reasons.add(reason("amount_spike", 0.2, "avg " + avgPaid.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
        }
        if (orders1h >= 8) {
            reasons.add(reason("order_velocity", 0.45, orders1h + " / 1h"));
        } else if (orders1h >= 4) {
            reasons.add(reason("order_velocity", 0.25, orders1h + " / 1h"));
        }
        if (failed24h >= 5) {
            reasons.add(reason("failed_payments", 0.45, failed24h + " / 24h"));
        } else if (failed24h >= 2) {
            reasons.add(reason("failed_payments", 0.25, failed24h + " / 24h"));
        }
        if (cancelled7d >= 6) {
            reasons.add(reason("cancellations", 0.3, cancelled7d + " / 7d"));
        } else if (cancelled7d >= 3) {
            reasons.add(reason("cancellations", 0.15, cancelled7d + " / 7d"));
        }
        if (!verified) {
            reasons.add(reason("unverified_email", 0.1, null));
        }
        if (isPublicIp(clientIp)) {
            Number sharedUsers = (Number) entityManager.createNativeQuery("SELECT COUNT(DISTINCT user_id) FROM refresh_tokens "
                            + "WHERE ip_address = CAST(:ip AS inet) AND created_at > :at - interval '24 hours'")
                    .setParameter("ip", clientIp)
                    .setParameter("at", toLocalDateTime(r[8]))
                    .getSingleResult();
            if (sharedUsers.longValue() >= 4) {
                reasons.add(reason("shared_ip", 0.2, sharedUsers + " accounts / 24h"));
            }
        }

        double keep = 1;
        for (Map<String, Object> item : reasons) {
            keep *= 1 - (Double) item.get("weight");
        }
        double score = Math.round((1 - keep) * 1000) / 1000.0;
        String level = score >= 0.6 ? "high" : score >= 0.3 ? "medium" : "low";
        entityManager.createNativeQuery("INSERT INTO fraud_scores (entity_type, entity_id, risk_score, risk_level, reasons, "
                        + "action_taken, model_version, created_at) VALUES ('payment', :p, :s, :l, CAST(:r AS jsonb), :a, :m, now())")
                .setParameter("p", paymentId)
                .setParameter("s", (float) score)
                .setParameter("l", level)
                .setParameter("r", objectMapper.writeValueAsString(reasons))
                .setParameter("a", "low".equals(level) ? "allow" : "review")
                .setParameter("m", MODEL_VERSION)
                .executeUpdate();
        return findByPayment(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Page<FraudScoreDTO> search(String level, String action, String q, int page, int limit) {
        String lvl = blankToNull(level);
        String act = blankToNull(action);
        if (lvl != null && !LEVELS.contains(lvl)) {
            throw ApiException.badRequest("level phải là low, medium hoặc high");
        }
        if (act != null && !ACTIONS.contains(act)) {
            throw ApiException.badRequest("action phải là một trong: " + String.join(", ", ACTIONS));
        }
        String query = q == null || q.isBlank() ? null : q.trim();
        int pageIndex = Math.max(page, 1) - 1;
        List<Object[]> rows = entityManager.createNativeQuery(SELECT + WHERE + "ORDER BY f.created_at DESC, f.id LIMIT :limit OFFSET :offset")
                .setParameter("level", lvl)
                .setParameter("action", act)
                .setParameter("q", query)
                .setParameter("limit", limit)
                .setParameter("offset", pageIndex * limit)
                .getResultList();
        Number total = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM fraud_scores f JOIN payments p "
                        + "ON p.id = f.entity_id JOIN orders o ON o.id = p.order_id JOIN users u ON u.id = o.user_id " + WHERE)
                .setParameter("level", lvl)
                .setParameter("action", act)
                .setParameter("q", query)
                .getSingleResult();
        return new PageImpl<>(rows.stream().map(this::toDTO).toList(), PageRequest.of(pageIndex, limit), total.longValue());
    }

    @Override
    @Transactional(readOnly = true)
    public FraudScoreDTO get(UUID id) {
        FraudScoreDTO dto = findOne("f.id = :id", id);
        if (dto == null) {
            throw ApiException.notFound("Không tìm thấy điểm rủi ro");
        }
        return dto;
    }

    @Override
    @Transactional
    public FraudScoreDTO updateAction(UUID id, String action) {
        String act = blankToNull(action);
        if (!List.of("review", "cleared", "confirmed_fraud").contains(act)) {
            throw ApiException.badRequest("action phải là review, cleared hoặc confirmed_fraud");
        }
        int updated = entityManager.createNativeQuery("UPDATE fraud_scores SET action_taken = :a WHERE id = :id AND entity_type = 'payment'")
                .setParameter("a", act)
                .setParameter("id", id)
                .executeUpdate();
        if (updated == 0) {
            throw ApiException.notFound("Không tìm thấy điểm rủi ro");
        }
        return findOne("f.id = :id", id);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Long> stats() {
        Map<String, Long> counts = new LinkedHashMap<>();
        LEVELS.forEach(l -> counts.put(l, 0L));
        counts.put("toReview", 0L);
        List<Object[]> rows = entityManager.createNativeQuery("SELECT risk_level, COUNT(*), "
                        + "COUNT(*) FILTER (WHERE action_taken = 'review') FROM fraud_scores WHERE entity_type = 'payment' "
                        + "GROUP BY risk_level")
                .getResultList();
        long review = 0;
        for (Object[] r : rows) {
            if (r[0] != null) {
                counts.put((String) r[0], ((Number) r[1]).longValue());
            }
            review += ((Number) r[2]).longValue();
        }
        counts.put("toReview", review);
        return counts;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public int backfill(int limit) {
        List<UUID> ids = entityManager.createNativeQuery("SELECT p.id FROM payments p WHERE NOT EXISTS (SELECT 1 FROM fraud_scores f "
                        + "WHERE f.entity_type = 'payment' AND f.entity_id = p.id) ORDER BY p.created_at DESC LIMIT :n")
                .setParameter("n", Math.max(1, limit))
                .getResultList();
        // Gọi trực tiếp (cùng transaction): chấm bù không có IP của request gốc.
        for (UUID id : ids) {
            scorePayment(id, null);
        }
        return ids.size();
    }

    private FraudScoreDTO findByPayment(UUID paymentId) {
        return findOne("f.entity_id = :id", paymentId);
    }

    @SuppressWarnings("unchecked")
    private FraudScoreDTO findOne(String condition, UUID id) {
        List<Object[]> rows = entityManager.createNativeQuery(SELECT + "WHERE f.entity_type = 'payment' AND " + condition
                        + " ORDER BY f.created_at DESC LIMIT 1")
                .setParameter("id", id)
                .getResultList();
        return rows.isEmpty() ? null : toDTO(rows.get(0));
    }

    private FraudScoreDTO toDTO(Object[] r) {
        FraudScoreDTO dto = new FraudScoreDTO();
        dto.setId((UUID) r[0]);
        dto.setPaymentId((UUID) r[1]);
        dto.setOrderId((UUID) r[2]);
        dto.setOrderCode((String) r[3]);
        dto.setOrderStatus((String) r[4]);
        dto.setPaymentStatus((String) r[5]);
        dto.setAmount((BigDecimal) r[6]);
        dto.setCurrencyCode(r[7] == null ? null : r[7].toString().trim());
        dto.setUserId((UUID) r[8]);
        dto.setUserEmail((String) r[9]);
        dto.setUserName((String) r[10]);
        dto.setRiskScore(Math.round(((Number) r[11]).doubleValue() * 1000) / 1000.0);
        dto.setRiskLevel((String) r[12]);
        dto.setReasons(r[13] == null ? objectMapper.createArrayNode() : objectMapper.readTree((String) r[13]));
        dto.setActionTaken((String) r[14]);
        dto.setModelVersion((String) r[15]);
        dto.setCreatedAt(toLocalDateTime(r[16]));
        return dto;
    }

    private static Map<String, Object> reason(String code, double weight, String value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("weight", weight);
        item.put("value", value);
        return item;
    }

    /** IP công khai thật (bỏ loopback / mạng nội bộ — máy dev, proxy nội bộ — để không gắn cờ nhầm mọi người). */
    private static boolean isPublicIp(String ip) {
        if (ip == null || ip.isBlank() || !ip.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return !(address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress());
        } catch (Exception ex) {
            return false;
        }
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        throw new IllegalArgumentException("Kiểu thời gian không hỗ trợ: " + value.getClass());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    private static long round(double value) {
        return Math.round(value);
    }
}
