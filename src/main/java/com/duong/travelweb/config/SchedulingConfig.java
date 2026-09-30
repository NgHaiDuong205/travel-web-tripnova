package com.duong.travelweb.config;

import com.duong.travelweb.service.CartService;
import com.duong.travelweb.service.OrderBookingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);
    private static final int GUEST_CART_TTL_DAYS = 30;

    private final List<OrderBookingHandler> handlers;
    private final CartService cartService;

    public SchedulingConfig(List<OrderBookingHandler> handlers, CartService cartService) {
        this.handlers = handlers;
        this.cartService = cartService;
    }

    /** Mỗi phút huỷ các booking pending đã quá hạn giữ chỗ (mọi loại: hotel, car...). */
    @Scheduled(fixedDelayString = "${app.booking.expire-job-delay-ms:60000}", initialDelay = 30000)
    public void expirePendingBookings() {
        handlers.forEach(OrderBookingHandler::expirePendingBookings);
    }

    /** 3 giờ sáng mỗi ngày xoá giỏ khách (chưa đăng nhập) không hoạt động quá 30 ngày. */
    @Scheduled(cron = "${app.cart.guest-purge-cron:0 0 3 * * *}")
    public void purgeStaleGuestCarts() {
        int removed = cartService.purgeStaleGuestCarts(GUEST_CART_TTL_DAYS);
        if (removed > 0) {
            log.info("Purged {} stale guest carts", removed);
        }
    }
}
