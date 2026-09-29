package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.service.OrderBookingHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tìm handler theo loại booking của order (mỗi order chỉ có một loại). */
@Component
public class OrderBookingRouter {
    private final List<OrderBookingHandler> handlers;

    public OrderBookingRouter(List<OrderBookingHandler> handlers) {
        this.handlers = handlers;
    }

    public OrderBookingHandler handlerFor(OrderEntity order) {
        return handlerFor(order.getId());
    }

    public OrderBookingHandler handlerFor(UUID orderId) {
        return handlers.stream()
                .filter(handler -> handler.handles(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Order " + orderId + " không có booking nào"));
    }

    /** Loại booking của order, null nếu order không có booking. */
    public String bookingTypeOf(UUID orderId) {
        return handlers.stream()
                .filter(handler -> handler.handles(orderId))
                .map(OrderBookingHandler::bookingType)
                .findFirst()
                .orElse(null);
    }

    public List<OrderBookingHandler> all() {
        return handlers;
    }

    /** orderId -> tổng tiền đã hoàn, gộp từ mọi loại booking. */
    public Map<UUID, BigDecimal> refundedAmounts(Collection<UUID> orderIds) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (orderIds.isEmpty()) {
            return result;
        }
        for (OrderBookingHandler handler : handlers) {
            handler.refundedAmounts(orderIds).forEach((id, amount) -> result.merge(id, amount, BigDecimal::add));
        }
        return result;
    }
}
