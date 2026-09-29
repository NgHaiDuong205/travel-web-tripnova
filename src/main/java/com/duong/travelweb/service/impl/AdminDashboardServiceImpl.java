package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AdminDashboardDTO;
import com.duong.travelweb.model.dto.DashboardStatisticsDTO;
import com.duong.travelweb.model.dto.RevenuePointDTO;
import com.duong.travelweb.model.dto.StatisticsPointDTO;
import com.duong.travelweb.model.dto.TopItemDTO;
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
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private static final int REVENUE_DAYS = 30;
    private static final int RECENT_BOOKINGS_LIMIT = 8;
    /** Giới hạn độ dài khoảng thời gian theo kiểu gom nhóm (tránh chuỗi quá nhiều điểm). */
    private static final Map<String, Integer> MAX_RANGE_DAYS = Map.of("day", 366, "week", 3 * 366, "month", 10 * 366);

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
        dto.setRevenueByDay(getRevenue(null, null, "day"));
        dto.setRecentBookings(hotelBookingService.findForAdmin(null, null, 1, RECENT_BOOKINGS_LIMIT));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatisticsDTO getStatistics(LocalDate from, LocalDate to, String granularity) {
        Range range = resolveRange(from, to, granularity);
        LocalDateTime start = range.from().atStartOfDay();
        LocalDateTime end = range.to().plusDays(1).atStartOfDay();
        long days = ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
        LocalDateTime previousStart = start.minusDays(days);

        DashboardStatisticsDTO dto = new DashboardStatisticsDTO();
        dto.setFrom(range.from());
        dto.setTo(range.to());
        dto.setGranularity(range.unit());
        dto.setCurrencyCode(currencyCode);
        dto.setNewUsers(userRepository.countCreatedBetween(start, end));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long totalBookings = 0;
        for (Object[] row : hotelBookingRepository.countGroupByStatusBetween(start, end)) {
            long count = (Long) row[1];
            byStatus.put((String) row[0], count);
            totalBookings += count;
        }
        dto.setBookingsByStatus(byStatus);
        dto.setTotalBookings(totalBookings);

        Object[] revenue = paymentRepository.summarizeSuccessBetween(start, end).get(0);
        dto.setRevenue(toBigDecimal(revenue[0]));
        dto.setTransactions(((Number) revenue[1]).longValue());
        dto.setRefundedAmount(hotelBookingRepository.sumRefundedBetween(start, end));
        dto.setAverageBookingValue(hotelBookingRepository.averagePaidBookingValueBetween(start, end)
                .setScale(2, RoundingMode.HALF_UP));

        dto.setPreviousNewUsers(userRepository.countCreatedBetween(previousStart, start));
        long previousBookings = 0;
        for (Object[] row : hotelBookingRepository.countGroupByStatusBetween(previousStart, start)) {
            previousBookings += (Long) row[1];
        }
        dto.setPreviousBookings(previousBookings);
        dto.setPreviousRevenue(toBigDecimal(paymentRepository.summarizeSuccessBetween(previousStart, start).get(0)[0]));

        Map<LocalDate, StatisticsPointDTO> points = new LinkedHashMap<>();
        for (LocalDate bucket : buckets(range)) {
            StatisticsPointDTO point = new StatisticsPointDTO();
            point.setDate(bucket);
            point.setRevenue(BigDecimal.ZERO);
            points.put(bucket, point);
        }
        for (Object[] row : hotelBookingRepository.countByBucket(range.unit(), start, end)) {
            StatisticsPointDTO point = points.get(toLocalDate(row[0]));
            if (point != null) {
                point.setBookings(((Number) row[1]).longValue());
            }
        }
        for (Object[] row : userRepository.countCreatedByBucket(range.unit(), start, end)) {
            StatisticsPointDTO point = points.get(toLocalDate(row[0]));
            if (point != null) {
                point.setNewUsers(((Number) row[1]).longValue());
            }
        }
        for (Object[] row : paymentRepository.revenueByBucket(range.unit(), start, end)) {
            StatisticsPointDTO point = points.get(toLocalDate(row[0]));
            if (point != null) {
                point.setRevenue(toBigDecimal(row[1]));
            }
        }
        dto.setSeries(new ArrayList<>(points.values()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenuePointDTO> getRevenue(LocalDate from, LocalDate to, String groupBy) {
        Range range = resolveRange(from, to, groupBy);
        Map<LocalDate, RevenuePointDTO> byBucket = new HashMap<>();
        for (Object[] row : paymentRepository.revenueByBucket(range.unit(),
                range.from().atStartOfDay(), range.to().plusDays(1).atStartOfDay())) {
            LocalDate bucket = toLocalDate(row[0]);
            byBucket.put(bucket, new RevenuePointDTO(bucket, toBigDecimal(row[1]), ((Number) row[2]).longValue()));
        }
        return buckets(range).stream()
                .map(bucket -> byBucket.getOrDefault(bucket, new RevenuePointDTO(bucket, BigDecimal.ZERO, 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopItemDTO> getTopHotels(LocalDate from, LocalDate to, int limit) {
        Range range = resolveRange(from, to, null);
        return toTopItems(hotelBookingRepository.topHotels(range.from().atStartOfDay(),
                range.to().plusDays(1).atStartOfDay(), limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopItemDTO> getTopDestinations(LocalDate from, LocalDate to, int limit) {
        Range range = resolveRange(from, to, null);
        return toTopItems(hotelBookingRepository.topDestinations(range.from().atStartOfDay(),
                range.to().plusDays(1).atStartOfDay(), limit));
    }

    private List<TopItemDTO> toTopItems(List<Object[]> rows) {
        return rows.stream().map(row -> {
            TopItemDTO item = new TopItemDTO();
            item.setId((UUID) row[0]);
            item.setName((String) row[1]);
            item.setSubtitle((String) row[2]);
            item.setBookings(((Number) row[3]).longValue());
            item.setNights(((Number) row[4]).longValue());
            item.setRevenue(toBigDecimal(row[5]));
            return item;
        }).toList();
    }

    /** Khoảng thời gian đã chuẩn hoá; unit dùng trực tiếp cho date_trunc. */
    private record Range(LocalDate from, LocalDate to, String unit) {
    }

    private Range resolveRange(LocalDate from, LocalDate to, String granularity) {
        String unit = granularity == null || granularity.isBlank() ? "day" : granularity.trim().toLowerCase();
        if (!MAX_RANGE_DAYS.containsKey(unit)) {
            throw ApiException.badRequest("Kiểu gom nhóm không hợp lệ: " + granularity + " (day | week | month)");
        }
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(REVENUE_DAYS - 1L);
        if (start.isAfter(end)) {
            throw ApiException.badRequest("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS.get(unit)) {
            throw ApiException.badRequest("Khoảng thời gian quá dài cho kiểu gom nhóm " + unit
                    + " (tối đa " + MAX_RANGE_DAYS.get(unit) + " ngày)");
        }
        return new Range(start, end, unit);
    }

    /** Ngày đầu của mọi kỳ giao với [from, to] (khớp date_trunc của Postgres: tuần bắt đầu thứ Hai). */
    private List<LocalDate> buckets(Range range) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate bucket = switch (range.unit()) {
            case "week" -> range.from().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> range.from().withDayOfMonth(1);
            default -> range.from();
        };
        while (!bucket.isAfter(range.to())) {
            result.add(bucket);
            bucket = switch (range.unit()) {
                case "week" -> bucket.plusWeeks(1);
                case "month" -> bucket.plusMonths(1);
                default -> bucket.plusDays(1);
            };
        }
        return result;
    }

    private LocalDate toLocalDate(Object value) {
        return value instanceof Date sqlDate ? sqlDate.toLocalDate() : LocalDate.parse(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
