package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
