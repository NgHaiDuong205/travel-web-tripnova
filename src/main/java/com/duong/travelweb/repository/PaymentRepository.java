package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    @Query("SELECT p FROM PaymentEntity p JOIN FETCH p.order o JOIN FETCH o.user WHERE p.id = :paymentId")
    Optional<PaymentEntity> findDetailById(@Param("paymentId") UUID paymentId);

    /** Khoá dòng payment khi xử lý kết quả cổng thanh toán, tránh xử lý trùng callback. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :paymentId")
    Optional<PaymentEntity> lockById(@Param("paymentId") UUID paymentId);

    @Query("SELECT p FROM PaymentEntity p WHERE p.order.id IN :orderIds ORDER BY p.createdAt DESC")
    List<PaymentEntity> findByOrderIds(@Param("orderIds") List<UUID> orderIds);

    // Không dùng "(:status IS NULL OR ...)" với tham số String: stringtype=unspecified khiến Postgres
    // không suy ra được kiểu tham số -> lỗi "could not determine data type". Tách 2 query.
    @Query(value = "SELECT p FROM PaymentEntity p JOIN FETCH p.order o WHERE o.user.id = :userId ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM PaymentEntity p WHERE p.order.user.id = :userId")
    Page<PaymentEntity> findByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT p FROM PaymentEntity p JOIN FETCH p.order o " +
                   "WHERE o.user.id = :userId AND p.status = :status ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM PaymentEntity p WHERE p.order.user.id = :userId AND p.status = :status")
    Page<PaymentEntity> findByUserAndStatus(@Param("userId") UUID userId, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT p FROM PaymentEntity p JOIN FETCH p.order o JOIN FETCH o.user ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM PaymentEntity p")
    Page<PaymentEntity> findAllForAdmin(Pageable pageable);

    @Query(value = "SELECT p FROM PaymentEntity p JOIN FETCH p.order o JOIN FETCH o.user WHERE p.status = :status ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM PaymentEntity p WHERE p.status = :status")
    Page<PaymentEntity> findAllForAdminByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.status = 'success'")
    BigDecimal sumSuccessfulAmount();

    /** Doanh thu theo ngày: [ngày (java.sql.Date), tổng tiền, số giao dịch]. */
    @Query(value = "SELECT CAST(paid_at AS date) AS day, SUM(amount), COUNT(*) FROM payments " +
                   "WHERE status = 'success' AND paid_at >= :from GROUP BY CAST(paid_at AS date) ORDER BY day",
           nativeQuery = true)
    List<Object[]> revenueByDaySince(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(p), " +
           "COALESCE(SUM(CASE WHEN p.status = 'success' THEN p.amount ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN p.status = 'refunded' THEN p.amount ELSE 0 END), 0) " +
           "FROM PaymentEntity p WHERE p.order.user.id = :userId")
    List<Object[]> summarizeByUser(@Param("userId") UUID userId);
}
