package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Query("SELECT COUNT(o) > 0 FROM OrderEntity o WHERE o.orderCode = :orderCode")
    boolean existsByOrderCode(@Param("orderCode") String orderCode);

    /** Order của user có notes bắt đầu bằng prefix (VD "itinerary:<id>" — đặt trọn gói từ lịch trình), mới trước. */
    @Query("SELECT o FROM OrderEntity o WHERE o.user.id = :userId AND o.notes LIKE CONCAT(:prefix, '%') ORDER BY o.createdAt DESC")
    List<OrderEntity> findByUserAndNotesPrefix(@Param("userId") UUID userId, @Param("prefix") String prefix);

    // Chưa có InvoiceEntity -> đếm bằng native query (invoices.order_id là FK RESTRICT).
    @Query(value = "SELECT COUNT(*) FROM invoices WHERE order_id = :orderId", nativeQuery = true)
    long countInvoices(@Param("orderId") UUID orderId);
}
