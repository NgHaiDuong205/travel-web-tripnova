package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Query("SELECT COUNT(o) > 0 FROM OrderEntity o WHERE o.orderCode = :orderCode")
    boolean existsByOrderCode(@Param("orderCode") String orderCode);

    // Chưa có InvoiceEntity -> đếm bằng native query (invoices.order_id là FK RESTRICT).
    @Query(value = "SELECT COUNT(*) FROM invoices WHERE order_id = :orderId", nativeQuery = true)
    long countInvoices(@Param("orderId") UUID orderId);
}
