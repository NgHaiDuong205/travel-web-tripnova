package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.AdminDashboardDTO;
import com.duong.travelweb.model.dto.RevenuePointDTO;
import com.duong.travelweb.repository.ContactMessageRepository;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.AdminDashboardService;
import com.duong.travelweb.service.HotelBookingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private static final int REVENUE_DAYS = 30;
    private static final int RECENT_BOOKINGS_LIMIT = 8;

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final PaymentRepository paymentRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final HotelBookingService hotelBookingService;
    private final String currencyCode;

    public AdminDashboardServiceImpl(UserRepository userRepository,
                                     HotelRepository hotelRepository,
                                     HotelBookingRepository hotelBookingRepository,
                                     PaymentRepository paymentRepository,
                                     ContactMessageRepository contactMessageRepository,
                                     HotelBookingService hotelBookingService,
                                     @Value("${app.booking.currency:USD}") String currencyCode) {
        this.userRepository = userRepository;
        this.hotelRepository = hotelRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.paymentRepository = paymentRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.hotelBookingService = hotelBookingService;
        this.currencyCode = currencyCode;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboard() {
        AdminDashboardDTO dto = new AdminDashboardDTO();
        dto.setTotalUsers(userRepository.countActiveAccounts());
        dto.setTotalHotels(hotelRepository.count());
        dto.setActiveHotels(hotelRepository.countActive());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long totalBookings = 0;
        for (Object[] row : hotelBookingRepository.countGroupByStatus()) {
            long count = (Long) row[1];
            byStatus.put((String) row[0], count);
            totalBookings += count;
        }
        dto.setBookingsByStatus(byStatus);
        dto.setTotalBookings(totalBookings);

        dto.setTotalRevenue(paymentRepository.sumSuccessfulAmount());
        dto.setCurrencyCode(currencyCode);
        dto.setNewContactMessages(contactMessageRepository.countByStatus("new"));
        dto.setRevenueByDay(revenueByDay());
        dto.setRecentBookings(hotelBookingService.findForAdmin(null, null, 1, RECENT_BOOKINGS_LIMIT));
        return dto;
    }

    /** Doanh thu từng ngày trong 30 ngày gần nhất, điền 0 cho ngày trống để biểu đồ liền mạch. */
    private List<RevenuePointDTO> revenueByDay() {
        LocalDate from = LocalDate.now().minusDays(REVENUE_DAYS - 1L);
        Map<LocalDate, RevenuePointDTO> byDate = new HashMap<>();
        for (Object[] row : paymentRepository.revenueByDaySince(from.atStartOfDay())) {
            LocalDate day = row[0] instanceof Date sqlDate ? sqlDate.toLocalDate() : LocalDate.parse(row[0].toString());
            BigDecimal revenue = row[1] instanceof BigDecimal decimal ? decimal : new BigDecimal(row[1].toString());
            byDate.put(day, new RevenuePointDTO(day, revenue, ((Number) row[2]).longValue()));
        }
        List<RevenuePointDTO> points = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(LocalDate.now()); day = day.plusDays(1)) {
            points.add(byDate.getOrDefault(day, new RevenuePointDTO(day, BigDecimal.ZERO, 0L)));
        }
        return points;
    }
}
