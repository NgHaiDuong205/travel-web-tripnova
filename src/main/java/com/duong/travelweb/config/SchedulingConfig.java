package com.duong.travelweb.config;

import com.duong.travelweb.service.HotelBookingService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    private final HotelBookingService hotelBookingService;

    public SchedulingConfig(HotelBookingService hotelBookingService) {
        this.hotelBookingService = hotelBookingService;
    }

    /** Mỗi phút huỷ các booking pending đã quá hạn giữ phòng. */
    @Scheduled(fixedDelayString = "${app.booking.expire-job-delay-ms:60000}", initialDelay = 30000)
    public void expirePendingBookings() {
        hotelBookingService.expirePendingBookings();
    }
}
