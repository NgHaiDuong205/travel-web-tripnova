package com.duong.travelweb.config;

import com.duong.travelweb.service.OrderBookingHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    private final List<OrderBookingHandler> handlers;

    public SchedulingConfig(List<OrderBookingHandler> handlers) {
        this.handlers = handlers;
    }

    /** Mỗi phút huỷ các booking pending đã quá hạn giữ chỗ (mọi loại: hotel, car...). */
    @Scheduled(fixedDelayString = "${app.booking.expire-job-delay-ms:60000}", initialDelay = 30000)
    public void expirePendingBookings() {
        handlers.forEach(OrderBookingHandler::expirePendingBookings);
    }
}
