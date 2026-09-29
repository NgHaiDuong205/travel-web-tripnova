package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.InvoiceEntity;
import com.duong.travelweb.repository.custom.InvoiceRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>, InvoiceRepositoryCustom {

    @Query("SELECT COUNT(i) > 0 FROM InvoiceEntity i WHERE i.order.id = :orderId")
    boolean existsByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT i FROM InvoiceEntity i JOIN FETCH i.order JOIN FETCH i.user LEFT JOIN FETCH i.payment WHERE i.id = :id")
    Optional<InvoiceEntity> findDetailById(@Param("id") UUID id);

    @Query(value = "SELECT i FROM InvoiceEntity i JOIN FETCH i.order JOIN FETCH i.user LEFT JOIN FETCH i.payment " +
                   "WHERE i.user.id = :userId ORDER BY i.issuedAt DESC",
           countQuery = "SELECT COUNT(i) FROM InvoiceEntity i WHERE i.user.id = :userId")
    Page<InvoiceEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /** Order đã thanh toán (kể cả hoàn một phần) nhưng chưa có hoá đơn — dùng để bổ sung hoá đơn cho dữ liệu cũ. */
    @Query(value = "SELECT o.id FROM orders o WHERE o.status IN ('paid', 'partially_refunded') " +
                   "AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.order_id = o.id)", nativeQuery = true)
    List<UUID> findPaidOrderIdsWithoutInvoice();
}
