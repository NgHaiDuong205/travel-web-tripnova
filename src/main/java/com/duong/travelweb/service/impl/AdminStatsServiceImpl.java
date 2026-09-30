package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AdminStatsDTO;
import com.duong.travelweb.model.dto.StatPointDTO;
import com.duong.travelweb.model.dto.StatSliceDTO;
import com.duong.travelweb.service.AdminStatsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thống kê cho từng trang admin: KPI + chuỗi thời gian (gom theo date_trunc) + các bảng phân bố.
 * Mọi câu SQL là hằng số; chỉ from/to/unit là tham số (unit đã kiểm tra theo danh sách cho phép).
 * "Doanh thu" của booking = tổng total_price các booking đã thanh toán (BOOKED), tạo trong kỳ;
 * khách sạn trừ thêm refund_amount. Doanh thu theo giao dịch (overview/payments) tính theo payments.
 */
@Service
public class AdminStatsServiceImpl implements AdminStatsService {
    public static final Set<String> SECTIONS = Set.of("overview", "hotels", "bookings", "cars", "flights", "tours",
            "users", "payments", "invoices", "messages", "reviews", "search", "audit");
    private static final int DEFAULT_DAYS = 30;
    private static final Map<String, Integer> MAX_RANGE_DAYS = Map.of("day", 366, "week", 3 * 366, "month", 10 * 366);
    private static final int TOP = 10;
    private static final UUID NO_HOTEL = new UUID(0L, 0L);

    /** Booking đã thanh toán / đã dùng dịch vụ (tính doanh thu). */
    private static final String BOOKED = "status IN ('confirmed', 'checked_in', 'checked_out', 'completed', 'no_show')";
    private static final String BUCKET = "CAST(date_trunc(CAST(:unit AS text), %s) AS date)";
    /** Loại sản phẩm của một order (mỗi order chỉ chứa 1 loại booking). */
    private static final String PRODUCT_OF_ORDER = "CASE"
            + " WHEN EXISTS (SELECT 1 FROM hotel_bookings x WHERE x.order_id = %1$s) THEN 'hotel'"
            + " WHEN EXISTS (SELECT 1 FROM car_bookings x WHERE x.order_id = %1$s) THEN 'car'"
            + " WHEN EXISTS (SELECT 1 FROM flight_bookings x WHERE x.order_id = %1$s) THEN 'flight'"
            + " WHEN EXISTS (SELECT 1 FROM tour_bookings x WHERE x.order_id = %1$s) THEN 'tour'"
            + " ELSE 'other' END";

    @PersistenceContext
    private EntityManager entityManager;

    private final String currencyCode;

    public AdminStatsServiceImpl(@Value("${app.booking.currency:USD}") String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDTO getStats(String section, LocalDate from, LocalDate to, String granularity) {
        String key = section == null ? "" : section.trim().toLowerCase();
        if (!SECTIONS.contains(key)) {
            throw ApiException.notFound("Không có thống kê cho mục: " + section);
        }
        Ctx c = new Ctx(resolveRange(from, to, granularity));
        c.dto.setSection(key);
        switch (key) {
            case "overview" -> overview(c);
            case "hotels" -> hotels(c);
            case "bookings" -> hotelBookings(c);
            case "cars" -> cars(c);
            case "flights" -> flights(c);
            case "tours" -> tours(c);
            case "users" -> users(c);
            case "payments" -> payments(c);
            case "invoices" -> invoices(c);
            case "messages" -> messages(c);
            case "reviews" -> reviews(c);
            case "search" -> search(c);
            default -> audit(c);
        }
        c.dto.setSeries(new ArrayList<>(c.points.values()));
        return c.dto;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDTO getHotelStats(Collection<UUID> hotelIds, LocalDate from, LocalDate to, String granularity) {
        Ctx c = new Ctx(resolveRange(from, to, granularity));
        // IN () rỗng là SQL sai: manager chưa được gán khách sạn → dùng id không tồn tại, mọi số liệu = 0.
        c.hotels = hotelIds == null || hotelIds.isEmpty() ? List.of(NO_HOTEL) : List.copyOf(hotelIds);
        c.dto.setSection("hotel");
        hotelOperations(c);
        c.dto.setSeries(new ArrayList<>(c.points.values()));
        return c.dto;
    }

    // ------------------------------------------------------------------ sections

    private void overview(Ctx c) {
        String typed = "WITH typed AS (SELECT p.paid_at, p.amount, p.payment_method, "
                + PRODUCT_OF_ORDER.formatted("p.order_id") + " AS product FROM payments p"
                + " WHERE p.status = 'success' AND p.paid_at >= :from AND p.paid_at < :to) ";
        series(c, typed + "SELECT " + bucket("paid_at")
                + ", SUM(CASE WHEN product = 'hotel' THEN amount ELSE 0 END)"
                + ", SUM(CASE WHEN product = 'car' THEN amount ELSE 0 END)"
                + ", SUM(CASE WHEN product = 'flight' THEN amount ELSE 0 END)"
                + ", SUM(CASE WHEN product = 'tour' THEN amount ELSE 0 END)"
                + " FROM typed GROUP BY 1", "hotel", "car", "flight", "tour");
        slices(c, "revenueByProduct", typed
                + "SELECT product, product, COUNT(*), SUM(amount) FROM typed GROUP BY product ORDER BY SUM(amount) DESC");
        slices(c, "paymentMethods", typed
                + "SELECT CAST(payment_method AS text), CAST(payment_method AS text), COUNT(*), SUM(amount)"
                + " FROM typed GROUP BY payment_method ORDER BY COUNT(*) DESC");
        slices(c, "bookingsByProduct", "SELECT k, k, COUNT(*), SUM(CASE WHEN booked THEN price ELSE 0 END) FROM ("
                + " SELECT 'hotel' k, total_price - COALESCE(refund_amount, 0) price, " + BOOKED + " booked FROM hotel_bookings"
                + " WHERE created_at >= :from AND created_at < :to"
                + " UNION ALL SELECT 'car', total_price, " + BOOKED + " FROM car_bookings WHERE created_at >= :from AND created_at < :to"
                + " UNION ALL SELECT 'flight', total_price, " + BOOKED + " FROM flight_bookings WHERE created_at >= :from AND created_at < :to"
                + " UNION ALL SELECT 'tour', total_price, " + BOOKED + " FROM tour_bookings WHERE created_at >= :from AND created_at < :to"
                + ") t GROUP BY k ORDER BY COUNT(*) DESC");
        kpi(c, "revenue", "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'success' AND paid_at >= :from AND paid_at < :to");
        kpi(c, "paidOrders", "SELECT COUNT(*) FROM payments WHERE status = 'success' AND paid_at >= :from AND paid_at < :to");
        kpi(c, "averageOrderValue", "SELECT COALESCE(ROUND(AVG(amount), 2), 0) FROM payments"
                + " WHERE status = 'success' AND paid_at >= :from AND paid_at < :to");
    }

    private void hotels(Ctx c) {
        series(c, "SELECT " + bucket("created_at") + ", COUNT(*)"
                + ", COALESCE(SUM(CASE WHEN " + BOOKED + " THEN num_nights ELSE 0 END), 0)"
                + ", COALESCE(SUM(CASE WHEN " + BOOKED + " THEN total_price - COALESCE(refund_amount, 0) ELSE 0 END), 0)"
                + " FROM hotel_bookings WHERE created_at >= :from AND created_at < :to GROUP BY 1", "bookings", "nights", "revenue");
        slices(c, "topHotels", "SELECT CAST(h.id AS text), h.name, COUNT(*), SUM(b.total_price - COALESCE(b.refund_amount, 0))"
                + " FROM hotel_bookings b JOIN hotels h ON h.id = b.hotel_id WHERE b." + BOOKED
                + " AND b.created_at >= :from AND b.created_at < :to GROUP BY h.id, h.name ORDER BY 4 DESC LIMIT " + TOP);
        slices(c, "byStars", "SELECT CAST(COALESCE(star_rating, 0) AS text), CASE WHEN star_rating IS NULL THEN 'Unrated'"
                + " ELSE star_rating || ' star' END, COUNT(*), NULL FROM hotels WHERE is_active GROUP BY star_rating ORDER BY star_rating");
        slices(c, "byDestination", "SELECT CAST(d.id AS text), d.name, COUNT(*), NULL FROM hotels h JOIN destinations d"
                + " ON d.id = h.destination_id WHERE h.is_active GROUP BY d.id, d.name ORDER BY 3 DESC LIMIT " + TOP);
        kpi(c, "activeHotels", "SELECT COUNT(*) FROM hotels WHERE is_active");
        kpi(c, "totalRooms", "SELECT COALESCE(SUM(total_rooms), 0) FROM hotels WHERE is_active");
        kpi(c, "nightsSold", "SELECT COALESCE(SUM(num_nights), 0) FROM hotel_bookings WHERE " + BOOKED
                + " AND created_at >= :from AND created_at < :to");
        kpi(c, "revenue", "SELECT COALESCE(SUM(total_price - COALESCE(refund_amount, 0)), 0) FROM hotel_bookings WHERE "
                + BOOKED + " AND created_at >= :from AND created_at < :to");
    }

    private void hotelBookings(Ctx c) {
        String range = " FROM hotel_bookings WHERE created_at >= :from AND created_at < :to";
        series(c, "SELECT " + bucket("created_at") + ", COUNT(*)"
                + ", COUNT(*) FILTER (WHERE " + BOOKED + ")"
                + ", COUNT(*) FILTER (WHERE status IN ('cancelled', 'refunded'))"
                + range + " GROUP BY 1", "created", "confirmed", "cancelled");
        slices(c, "byStatus", "SELECT CAST(status AS text), CAST(status AS text), COUNT(*), SUM(total_price)"
                + range + " GROUP BY status ORDER BY COUNT(*) DESC");
        slices(c, "byStayLength", stayLengthSql(range));
        slices(c, "byLeadTime", leadTimeSql(range));
        kpi(c, "bookings", "SELECT COUNT(*)" + range);
        kpi(c, "cancellationRate", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE status IN ('cancelled', 'refunded'))"
                + " / NULLIF(COUNT(*), 0), 1), 0)" + range);
        kpi(c, "averageNights", "SELECT COALESCE(ROUND(AVG(num_nights), 1), 0)" + range);
        kpi(c, "averageValue", "SELECT COALESCE(ROUND(AVG(total_price), 2), 0)" + range + " AND " + BOOKED);
    }

    /** Phân bố số đêm; `from` = mệnh đề FROM ... WHERE trên hotel_bookings (một bảng, cột không cần alias). */
    private static String stayLengthSql(String from) {
        return "SELECT k, k, COUNT(*), NULL FROM (SELECT CASE WHEN num_nights <= 1 THEN '1 night'"
                + " WHEN num_nights = 2 THEN '2 nights' WHEN num_nights <= 4 THEN '3-4 nights' WHEN num_nights <= 7 THEN '5-7 nights'"
                + " ELSE '8+ nights' END k, num_nights n" + from + ") t GROUP BY k ORDER BY MIN(n)";
    }

    /** Khoảng cách từ ngày đặt tới ngày nhận phòng. */
    private static String leadTimeSql(String from) {
        return "SELECT k, k, COUNT(*), NULL FROM (SELECT check_in_date - CAST(created_at AS date) d, CASE"
                + " WHEN check_in_date - CAST(created_at AS date) <= 2 THEN '0-2 days'"
                + " WHEN check_in_date - CAST(created_at AS date) <= 7 THEN '3-7 days'"
                + " WHEN check_in_date - CAST(created_at AS date) <= 30 THEN '8-30 days' ELSE '31+ days' END k"
                + from + ") t GROUP BY k ORDER BY MIN(d)";
    }

    /**
     * Vận hành của một nhóm khách sạn (trang quản lý khách sạn). Hai loại mốc thời gian:
     * - "tạo trong kỳ" (created_at): số booking, doanh thu, tỷ lệ huỷ, phân bố;
     * - "ngày lưu trú": đêm phòng đã bán & công suất phòng — mỗi booking đã thanh toán chiếm 1 phòng
     *   cho mỗi đêm trong [check_in, check_out); công suất = đêm đã bán / (số phòng của hạng đang bán × số ngày).
     */
    private void hotelOperations(Ctx c) {
        String booked = "b." + BOOKED;
        String net = "b.total_price - COALESCE(b.refund_amount, 0)";
        String range = " FROM hotel_bookings b WHERE b.hotel_id IN (:hotels) AND b.created_at >= :from AND b.created_at < :to";
        String occupancy = "WITH days AS (SELECT CAST(g AS date) d FROM generate_series(CAST(:from AS date),"
                + " CAST(:to AS date) - 1, interval '1 day') g),"
                + " occ AS (SELECT days.d, COUNT(b.id) nights FROM days LEFT JOIN hotel_bookings b ON b.hotel_id IN (:hotels)"
                + " AND " + booked + " AND b.check_in_date <= days.d AND b.check_out_date > days.d GROUP BY days.d),"
                + " inv AS (SELECT COUNT(*) n FROM rooms r JOIN room_types rt ON rt.id = r.room_type_id"
                + " WHERE rt.hotel_id IN (:hotels) AND rt.is_active) ";
        String occupancyRate = "COALESCE(ROUND(100.0 * SUM(nights) / NULLIF(COUNT(*) * (SELECT n FROM inv), 0), 1), 0)";

        series(c, "SELECT " + bucket("b.created_at") + ", COUNT(*)"
                + ", COALESCE(SUM(CASE WHEN " + booked + " THEN " + net + " ELSE 0 END), 0)"
                + range + " GROUP BY 1", "bookings", "revenue");
        series(c, occupancy + "SELECT " + bucket("d") + ", SUM(nights), " + occupancyRate + " FROM occ GROUP BY 1",
                "roomNights", "occupancy");

        slices(c, "byRoomType", "SELECT CAST(rt.id AS text), rt.name, COUNT(*), SUM(" + net + ") FROM hotel_bookings b"
                + " JOIN room_types rt ON rt.id = b.room_type_id WHERE b.hotel_id IN (:hotels)"
                + " AND b.created_at >= :from AND b.created_at < :to AND " + booked + " GROUP BY rt.id, rt.name ORDER BY 4 DESC");
        slices(c, "byStatus", "SELECT CAST(b.status AS text), CAST(b.status AS text), COUNT(*), SUM(b.total_price)"
                + range + " GROUP BY b.status ORDER BY COUNT(*) DESC");
        slices(c, "byCheckInWeekday", "SELECT CAST(EXTRACT(ISODOW FROM b.check_in_date) AS text),"
                + " MIN(TRIM(to_char(b.check_in_date, 'Day'))), COUNT(*), NULL" + range + " AND " + booked
                + " GROUP BY EXTRACT(ISODOW FROM b.check_in_date) ORDER BY EXTRACT(ISODOW FROM b.check_in_date)");
        slices(c, "byStayLength", stayLengthSql(range));
        slices(c, "byLeadTime", leadTimeSql(range));
        slices(c, "byRating", "SELECT CAST(r AS text), r || ' star', COALESCE(cnt, 0), NULL FROM generate_series(1, 5) r"
                + " LEFT JOIN (SELECT rating, COUNT(*) cnt FROM posts WHERE entity_type = 'hotel' AND entity_id IN (:hotels)"
                + " AND status = 'approved' GROUP BY rating) p ON p.rating = r ORDER BY r DESC");
        slices(c, "byHotel", "SELECT CAST(h.id AS text), h.name, COUNT(b.id),"
                + " COALESCE(SUM(CASE WHEN " + booked + " THEN " + net + " END), 0) FROM hotels h"
                + " LEFT JOIN hotel_bookings b ON b.hotel_id = h.id AND b.created_at >= :from AND b.created_at < :to"
                + " WHERE h.id IN (:hotels) GROUP BY h.id, h.name ORDER BY 4 DESC, 2");

        kpi(c, "revenue", "SELECT COALESCE(SUM(" + net + "), 0)" + range + " AND " + booked);
        kpi(c, "bookings", "SELECT COUNT(*)" + range);
        kpi(c, "roomNights", occupancy + "SELECT COALESCE(SUM(nights), 0) FROM occ");
        kpi(c, "occupancyRate", occupancy + "SELECT " + occupancyRate + " FROM occ");
        // Giá phòng bình quân mỗi đêm đã bán (ADR) của booking đã thanh toán tạo trong kỳ.
        kpi(c, "averageDailyRate", "SELECT COALESCE(ROUND(SUM(" + net + ") / NULLIF(SUM(b.num_nights), 0), 2), 0)"
                + range + " AND " + booked);
        kpi(c, "cancellationRate", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE b.status IN ('cancelled', 'refunded'))"
                + " / NULLIF(COUNT(*), 0), 1), 0)" + range);
        kpi(c, "averageRating", "SELECT COALESCE(ROUND(AVG(rating), 2), 0) FROM posts WHERE entity_type = 'hotel'"
                + " AND entity_id IN (:hotels) AND status = 'approved'");
        kpi(c, "rooms", "SELECT COUNT(*) FROM rooms r JOIN room_types rt ON rt.id = r.room_type_id"
                + " WHERE rt.hotel_id IN (:hotels) AND rt.is_active");
    }

    private void cars(Ctx c) {
        String range = " FROM car_bookings b WHERE b.created_at >= :from AND b.created_at < :to";
        String days = "GREATEST(1, CEIL(EXTRACT(EPOCH FROM (b.return_date - b.pickup_date)) / 86400))";
        series(c, "SELECT " + bucket("b.created_at") + ", COUNT(*)"
                + ", COALESCE(SUM(CASE WHEN b." + BOOKED + " THEN b.total_price ELSE 0 END), 0)"
                + range + " GROUP BY 1", "rentals", "revenue");
        slices(c, "byStatus", "SELECT CAST(b.status AS text), CAST(b.status AS text), COUNT(*), SUM(b.total_price)"
                + range + " GROUP BY b.status ORDER BY COUNT(*) DESC");
        slices(c, "byCarType", "SELECT CAST(c.car_type AS text), CAST(c.car_type AS text), COUNT(*), SUM(b.total_price)"
                + " FROM car_bookings b JOIN cars c ON c.id = b.car_id WHERE b.created_at >= :from AND b.created_at < :to"
                + " AND b." + BOOKED + " GROUP BY c.car_type ORDER BY 4 DESC");
        slices(c, "topCars", "SELECT CAST(c.id AS text), c.name, COUNT(*), SUM(b.total_price)"
                + " FROM car_bookings b JOIN cars c ON c.id = b.car_id WHERE b.created_at >= :from AND b.created_at < :to AND b."
                + BOOKED + " GROUP BY c.id, c.name ORDER BY 4 DESC LIMIT " + TOP);
        slices(c, "byRentalDays", "SELECT k, k, COUNT(*), NULL FROM (SELECT " + days + " d, CASE WHEN " + days + " <= 1 THEN '1 day'"
                + " WHEN " + days + " <= 3 THEN '2-3 days' WHEN " + days + " <= 7 THEN '4-7 days' ELSE '8+ days' END k"
                + range + ") t GROUP BY k ORDER BY MIN(d)");
        kpi(c, "rentals", "SELECT COUNT(*)" + range);
        kpi(c, "revenue", "SELECT COALESCE(SUM(b.total_price), 0)" + range + " AND b." + BOOKED);
        kpi(c, "activeCars", "SELECT COUNT(*) FROM cars WHERE is_active");
        // Ngày-xe đã cho thuê nằm trong kỳ / (số xe đang hoạt động × số ngày của kỳ).
        kpi(c, "utilization", "SELECT COALESCE(ROUND(100.0 * SUM(EXTRACT(EPOCH FROM (LEAST(b.return_date, :to)"
                + " - GREATEST(b.pickup_date, :from))) / 86400) / NULLIF((SELECT COUNT(*) FROM cars WHERE is_active)"
                + " * EXTRACT(EPOCH FROM (CAST(:to AS timestamp) - CAST(:from AS timestamp))) / 86400, 0), 1), 0)"
                + " FROM car_bookings b WHERE b." + BOOKED + " AND b.pickup_date < :to AND b.return_date > :from");
    }

    private void flights(Ctx c) {
        String range = " FROM flight_bookings b WHERE b.created_at >= :from AND b.created_at < :to";
        series(c, "SELECT " + bucket("b.created_at") + ", COUNT(*)"
                + ", COALESCE(SUM(CASE WHEN b." + BOOKED + " THEN b.total_price ELSE 0 END), 0)"
                + range + " GROUP BY 1", "tickets", "revenue");
        slices(c, "byStatus", "SELECT CAST(b.status AS text), CAST(b.status AS text), COUNT(*), SUM(b.total_price)"
                + range + " GROUP BY b.status ORDER BY COUNT(*) DESC");
        slices(c, "byCabinClass", "SELECT CAST(s.seat_class AS text), CAST(s.seat_class AS text), COUNT(*), SUM(b.total_price)"
                + " FROM flight_bookings b JOIN flight_seats s ON s.id = b.seat_id WHERE b.created_at >= :from AND b.created_at < :to"
                + " AND b." + BOOKED + " GROUP BY s.seat_class ORDER BY COUNT(*) DESC");
        slices(c, "byAirline", "SELECT f.airline, f.airline, COUNT(*), SUM(b.total_price) FROM flight_bookings b"
                + " JOIN flights f ON f.id = b.flight_id WHERE b.created_at >= :from AND b.created_at < :to AND b." + BOOKED
                + " GROUP BY f.airline ORDER BY 4 DESC LIMIT " + TOP);
        slices(c, "topRoutes", "SELECT f.departure_airport_code || '-' || f.arrival_airport_code,"
                + " f.departure_airport_code || ' → ' || f.arrival_airport_code, COUNT(*), SUM(b.total_price) FROM flight_bookings b"
                + " JOIN flights f ON f.id = b.flight_id WHERE b.created_at >= :from AND b.created_at < :to AND b." + BOOKED
                + " GROUP BY f.departure_airport_code, f.arrival_airport_code ORDER BY 3 DESC LIMIT " + TOP);
        kpi(c, "tickets", "SELECT COUNT(*)" + range);
        kpi(c, "revenue", "SELECT COALESCE(SUM(b.total_price), 0)" + range + " AND b." + BOOKED);
        kpi(c, "flightsDeparting", "SELECT COUNT(*) FROM flights WHERE departure_time >= :from AND departure_time < :to");
        // Tỷ lệ lấp đầy ghế của các chuyến khởi hành trong kỳ (ghế booked / tổng ghế có sơ đồ).
        kpi(c, "loadFactor", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE s.status = 'booked') / NULLIF(COUNT(*), 0), 1), 0)"
                + " FROM flight_seats s JOIN flights f ON f.id = s.flight_id WHERE f.departure_time >= :from AND f.departure_time < :to");
    }

    private void tours(Ctx c) {
        String range = " FROM tour_bookings b WHERE b.created_at >= :from AND b.created_at < :to";
        series(c, "SELECT " + bucket("b.created_at") + ", COUNT(*)"
                + ", COALESCE(SUM(CASE WHEN b." + BOOKED + " THEN b.num_adults + b.num_children ELSE 0 END), 0)"
                + ", COALESCE(SUM(CASE WHEN b." + BOOKED + " THEN b.total_price ELSE 0 END), 0)"
                + range + " GROUP BY 1", "bookings", "travellers", "revenue");
        slices(c, "byStatus", "SELECT CAST(b.status AS text), CAST(b.status AS text), COUNT(*), SUM(b.total_price)"
                + range + " GROUP BY b.status ORDER BY COUNT(*) DESC");
        slices(c, "topTours", "SELECT CAST(t.id AS text), t.name, COUNT(*), SUM(b.total_price) FROM tour_bookings b"
                + " JOIN tours t ON t.id = b.tour_id WHERE b.created_at >= :from AND b.created_at < :to AND b." + BOOKED
                + " GROUP BY t.id, t.name ORDER BY 4 DESC LIMIT " + TOP);
        slices(c, "byGroupSize", "SELECT k, k, COUNT(*), NULL FROM (SELECT b.num_adults + b.num_children n, CASE"
                + " WHEN b.num_adults + b.num_children <= 1 THEN 'Solo' WHEN b.num_adults + b.num_children = 2 THEN '2 people'"
                + " WHEN b.num_adults + b.num_children <= 4 THEN '3-4 people' ELSE '5+ people' END k" + range + ") t"
                + " GROUP BY k ORDER BY MIN(n)");
        slices(c, "byDestination", "SELECT CAST(d.id AS text), d.name, COUNT(*), SUM(b.total_price) FROM tour_bookings b"
                + " JOIN tours t ON t.id = b.tour_id JOIN destinations d ON d.id = t.destination_id"
                + " WHERE b.created_at >= :from AND b.created_at < :to AND b." + BOOKED
                + " GROUP BY d.id, d.name ORDER BY 4 DESC LIMIT " + TOP);
        kpi(c, "bookings", "SELECT COUNT(*)" + range);
        kpi(c, "travellers", "SELECT COALESCE(SUM(b.num_adults + b.num_children), 0)" + range + " AND b." + BOOKED);
        kpi(c, "revenue", "SELECT COALESCE(SUM(b.total_price), 0)" + range + " AND b." + BOOKED);
        kpi(c, "activeTours", "SELECT COUNT(*) FROM tours WHERE is_active");
    }

    private void users(Ctx c) {
        String range = " FROM users WHERE created_at >= :from AND created_at < :to";
        series(c, "SELECT " + bucket("created_at") + ", COUNT(*), COUNT(*) FILTER (WHERE is_verified)"
                + range + " GROUP BY 1", "newUsers", "verified");
        slices(c, "byStatus", "SELECT k, k, COUNT(*), NULL FROM (SELECT CASE WHEN deleted_at IS NOT NULL THEN 'deleted'"
                + " WHEN NOT is_active THEN 'locked' ELSE 'active' END k FROM users) t GROUP BY k ORDER BY COUNT(*) DESC");
        slices(c, "byRole", "SELECT r.name, r.name, COUNT(DISTINCT ur.user_id), NULL FROM user_roles ur JOIN roles r ON r.id = ur.role_id"
                + " JOIN users u ON u.id = ur.user_id WHERE u.deleted_at IS NULL GROUP BY r.name ORDER BY 3 DESC");
        slices(c, "byGender", "SELECT COALESCE(CAST(gender AS text), 'unknown'), COALESCE(CAST(gender AS text), 'unknown'), COUNT(*), NULL"
                + " FROM users WHERE deleted_at IS NULL GROUP BY gender ORDER BY COUNT(*) DESC");
        slices(c, "topSpenders", "SELECT CAST(u.id AS text), u.email, COUNT(*), SUM(p.amount) FROM payments p"
                + " JOIN orders o ON o.id = p.order_id JOIN users u ON u.id = o.user_id WHERE p.status = 'success'"
                + " AND p.paid_at >= :from AND p.paid_at < :to GROUP BY u.id ORDER BY 4 DESC LIMIT " + TOP);
        kpi(c, "totalUsers", "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL");
        kpi(c, "newUsers", "SELECT COUNT(*)" + range);
        kpi(c, "verifiedRate", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE is_verified) / NULLIF(COUNT(*), 0), 1), 0)"
                + " FROM users WHERE deleted_at IS NULL");
        kpi(c, "activeLast30Days", "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND last_login_at >= now() - interval '30 days'");
    }

    private void payments(Ctx c) {
        String range = " FROM payments WHERE created_at >= :from AND created_at < :to";
        series(c, "SELECT " + bucket("created_at")
                + ", COALESCE(SUM(amount) FILTER (WHERE status = 'success'), 0)"
                + ", COALESCE(SUM(amount) FILTER (WHERE status = 'failed'), 0)"
                + ", COALESCE(SUM(amount) FILTER (WHERE status = 'refunded'), 0)"
                + ", COALESCE(SUM(amount) FILTER (WHERE status = 'pending'), 0)"
                + range + " GROUP BY 1", "success", "failed", "refunded", "pending");
        slices(c, "byStatus", "SELECT CAST(status AS text), CAST(status AS text), COUNT(*), SUM(amount)"
                + range + " GROUP BY status ORDER BY COUNT(*) DESC");
        slices(c, "byMethod", "SELECT CAST(payment_method AS text), CAST(payment_method AS text), COUNT(*), SUM(amount)"
                + range + " AND status = 'success' GROUP BY payment_method ORDER BY 4 DESC");
        kpi(c, "volume", "SELECT COALESCE(SUM(amount), 0)" + range + " AND status = 'success'");
        kpi(c, "transactions", "SELECT COUNT(*)" + range);
        kpi(c, "successRate", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE status IN ('success', 'refunded'))"
                + " / NULLIF(COUNT(*) FILTER (WHERE status <> 'pending'), 0), 1), 0)" + range);
        kpi(c, "refunded", "SELECT COALESCE(SUM(amount), 0)" + range + " AND status = 'refunded'");
    }

    private void invoices(Ctx c) {
        String range = " FROM invoices i WHERE i.issued_at >= :from AND i.issued_at < :to";
        series(c, "SELECT " + bucket("i.issued_at") + ", COUNT(*), COALESCE(SUM(i.total_amount), 0)"
                + range + " GROUP BY 1", "invoices", "amount");
        slices(c, "byProduct", "SELECT k, k, COUNT(*), SUM(total_amount) FROM (SELECT i.total_amount, "
                + PRODUCT_OF_ORDER.formatted("i.order_id") + " k" + range + ") t GROUP BY k ORDER BY 4 DESC");
        slices(c, "byAmount", "SELECT k, k, COUNT(*), SUM(a) FROM (SELECT i.total_amount a, CASE WHEN i.total_amount < 100 THEN '< 100'"
                + " WHEN i.total_amount < 500 THEN '100-499' WHEN i.total_amount < 1000 THEN '500-999' ELSE '1000+' END k"
                + range + ") t GROUP BY k ORDER BY MIN(a)");
        kpi(c, "invoices", "SELECT COUNT(*)" + range);
        kpi(c, "amount", "SELECT COALESCE(SUM(i.total_amount), 0)" + range);
        kpi(c, "averageAmount", "SELECT COALESCE(ROUND(AVG(i.total_amount), 2), 0)" + range);
    }

    private void messages(Ctx c) {
        String range = " FROM contact_messages WHERE created_at >= :from AND created_at < :to";
        series(c, "SELECT b, SUM(received), SUM(replied) FROM ("
                + " SELECT " + bucket("created_at") + " b, 1 received, 0 replied" + range
                + " UNION ALL SELECT " + bucket("replied_at") + ", 0, 1 FROM contact_messages WHERE replied_at >= :from AND replied_at < :to"
                + ") t GROUP BY b", "received", "replied");
        slices(c, "byStatus", "SELECT CAST(status AS text), CAST(status AS text), COUNT(*), NULL"
                + range + " GROUP BY status ORDER BY COUNT(*) DESC");
        slices(c, "bySubject", "SELECT LOWER(TRIM(COALESCE(NULLIF(subject, ''), '(no subject)'))) k,"
                + " MIN(COALESCE(NULLIF(subject, ''), '(no subject)')), COUNT(*), NULL" + range + " GROUP BY k ORDER BY 3 DESC LIMIT 8");
        kpi(c, "received", "SELECT COUNT(*)" + range);
        kpi(c, "unanswered", "SELECT COUNT(*) FROM contact_messages WHERE status = 'new'");
        kpi(c, "averageReplyHours", "SELECT COALESCE(ROUND(CAST(AVG(EXTRACT(EPOCH FROM (replied_at - created_at)) / 3600) AS numeric), 1), 0)"
                + range + " AND replied_at IS NOT NULL");
    }

    private void reviews(Ctx c) {
        String range = " FROM posts WHERE created_at >= :from AND created_at < :to";
        series(c, "SELECT " + bucket("created_at") + ", COUNT(*), COALESCE(ROUND(AVG(rating), 2), 0)"
                + range + " GROUP BY 1", "reviews", "averageRating");
        slices(c, "byRating", "SELECT CAST(r AS text), r || ' star', COALESCE(cnt, 0), NULL FROM generate_series(1, 5) r"
                + " LEFT JOIN (SELECT rating, COUNT(*) cnt FROM posts WHERE status = 'approved' GROUP BY rating) p ON p.rating = r"
                + " ORDER BY r DESC");
        slices(c, "byEntityType", "SELECT entity_type, entity_type, COUNT(*), NULL" + range + " GROUP BY entity_type ORDER BY 3 DESC");
        slices(c, "byStatus", "SELECT CAST(status AS text), CAST(status AS text), COUNT(*), NULL" + range + " GROUP BY status ORDER BY 3 DESC");
        kpi(c, "reviews", "SELECT COUNT(*)" + range);
        kpi(c, "averageRating", "SELECT COALESCE(ROUND(AVG(rating), 2), 0) FROM posts WHERE status = 'approved'");
        kpi(c, "verifiedShare", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE is_verified_booking) / NULLIF(COUNT(*), 0), 1), 0)"
                + range);
        kpi(c, "pending", "SELECT COUNT(*) FROM posts WHERE status = 'pending'");
    }

    private void search(Ctx c) {
        String range = " FROM search_queries WHERE created_at >= :from AND created_at < :to";
        series(c, "SELECT " + bucket("created_at") + ", COUNT(*), COUNT(*) FILTER (WHERE result_count = 0)"
                + range + " GROUP BY 1", "queries", "zeroResults");
        slices(c, "topQueries", "SELECT LOWER(TRIM(query_text)) k, MIN(query_text), COUNT(*), NULL" + range
                + " AND TRIM(query_text) <> '' GROUP BY k ORDER BY 3 DESC LIMIT " + TOP);
        slices(c, "zeroResultQueries", "SELECT LOWER(TRIM(query_text)) k, MIN(query_text), COUNT(*), NULL" + range
                + " AND result_count = 0 AND TRIM(query_text) <> '' GROUP BY k ORDER BY 3 DESC LIMIT " + TOP);
        slices(c, "byUser", "SELECT k, k, COUNT(*), NULL FROM (SELECT CASE WHEN user_id IS NULL THEN 'guest' ELSE 'member' END k"
                + range + ") t GROUP BY k ORDER BY 3 DESC");
        kpi(c, "queries", "SELECT COUNT(*)" + range);
        kpi(c, "zeroResultRate", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE result_count = 0) / NULLIF(COUNT(*), 0), 1), 0)"
                + range);
        kpi(c, "clickRate", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE clicked_entity_id IS NOT NULL)"
                + " / NULLIF(COUNT(*), 0), 1), 0)" + range);
    }

    private void audit(Ctx c) {
        String range = " FROM audit_logs WHERE created_at >= :from AND created_at < :to";
        series(c, "SELECT " + bucket("created_at") + ", COUNT(*), COUNT(*) FILTER (WHERE status_code >= 400)"
                + range + " GROUP BY 1", "actions", "errors");
        slices(c, "byMethod", "SELECT http_method, http_method, COUNT(*), NULL" + range + " GROUP BY http_method ORDER BY 3 DESC");
        slices(c, "byStatusClass", "SELECT k, k, COUNT(*), NULL FROM (SELECT COALESCE(CAST(status_code / 100 AS text) || 'xx', 'n/a') k"
                + range + ") t GROUP BY k ORDER BY k");
        slices(c, "topActions", "SELECT action, action, COUNT(*), NULL" + range + " GROUP BY action ORDER BY 3 DESC LIMIT " + TOP);
        slices(c, "topUsers", "SELECT COALESCE(user_email, '(unknown)'), COALESCE(user_email, '(unknown)'), COUNT(*), NULL"
                + range + " GROUP BY user_email ORDER BY 3 DESC LIMIT " + TOP);
        kpi(c, "actions", "SELECT COUNT(*)" + range);
        kpi(c, "errorRate", "SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE status_code >= 400) / NULLIF(COUNT(*), 0), 1), 0)"
                + range);
        kpi(c, "activeUsers", "SELECT COUNT(DISTINCT user_id)" + range);
    }

    // ------------------------------------------------------------------ helpers

    /** Trạng thái một lần tính: khoảng thời gian + DTO + các điểm của chuỗi (đã có đủ mọi kỳ). */
    private final class Ctx {
        private final Range range;
        private final AdminStatsDTO dto = new AdminStatsDTO();
        private final Map<LocalDate, StatPointDTO> points = new LinkedHashMap<>();
        /** Chỉ dùng cho thống kê khách sạn (tham số :hotels). */
        private List<UUID> hotels = List.of();

        private Ctx(Range range) {
            this.range = range;
            dto.setFrom(range.from());
            dto.setTo(range.to());
            dto.setGranularity(range.unit());
            dto.setCurrencyCode(currencyCode);
            for (LocalDate bucket : buckets(range)) {
                points.put(bucket, new StatPointDTO(bucket));
            }
        }
    }

    private static String bucket(String column) {
        return BUCKET.formatted(column);
    }

    /** Câu SQL trả [kỳ, giá trị 1..n] theo thứ tự `keys`; kỳ không có dữ liệu = 0. */
    private void series(Ctx c, String sql, String... keys) {
        for (StatPointDTO point : c.points.values()) {
            for (String key : keys) {
                point.getValues().put(key, BigDecimal.ZERO);
            }
        }
        for (Object row : query(c, sql).getResultList()) {
            Object[] cols = (Object[]) row;
            StatPointDTO point = c.points.get(toLocalDate(cols[0]));
            if (point == null) {
                continue;
            }
            for (int i = 0; i < keys.length; i++) {
                point.getValues().put(keys[i], toBigDecimal(cols[i + 1]));
            }
        }
    }

    /** Câu SQL trả [key, label, count, amount]. */
    private void slices(Ctx c, String name, String sql) {
        List<StatSliceDTO> result = new ArrayList<>();
        for (Object row : query(c, sql).getResultList()) {
            Object[] cols = (Object[]) row;
            result.add(new StatSliceDTO(String.valueOf(cols[0]), String.valueOf(cols[1]),
                    cols[2] == null ? 0 : ((Number) cols[2]).longValue(), cols[3] == null ? null : toBigDecimal(cols[3])));
        }
        c.dto.getBreakdowns().put(name, result);
    }

    private void kpi(Ctx c, String name, String sql) {
        c.dto.getKpis().put(name, toBigDecimal(query(c, sql).getSingleResult()));
    }

    /** Chỉ gán tham số mà câu SQL thực sự dùng (Hibernate báo lỗi nếu gán tham số không tồn tại). */
    private Query query(Ctx c, String sql) {
        Query query = entityManager.createNativeQuery(sql);
        if (sql.contains(":from")) {
            query.setParameter("from", c.range.from().atStartOfDay());
        }
        if (sql.contains(":to")) {
            query.setParameter("to", c.range.to().plusDays(1).atStartOfDay());
        }
        if (sql.contains(":unit")) {
            query.setParameter("unit", c.range.unit());
        }
        if (sql.contains(":hotels")) {
            query.setParameter("hotels", c.hotels);
        }
        return query;
    }

    private record Range(LocalDate from, LocalDate to, String unit) {
    }

    private Range resolveRange(LocalDate from, LocalDate to, String granularity) {
        String unit = granularity == null || granularity.isBlank() ? "day" : granularity.trim().toLowerCase();
        if (!MAX_RANGE_DAYS.containsKey(unit)) {
            throw ApiException.badRequest("Kiểu gom nhóm không hợp lệ: " + granularity + " (day | week | month)");
        }
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
        if (start.isAfter(end)) {
            throw ApiException.badRequest("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS.get(unit)) {
            throw ApiException.badRequest("Khoảng thời gian quá dài cho kiểu gom nhóm " + unit
                    + " (tối đa " + MAX_RANGE_DAYS.get(unit) + " ngày)");
        }
        return new Range(start, end, unit);
    }

    /** Ngày đầu của mọi kỳ giao với [from, to] (khớp date_trunc: tuần bắt đầu thứ Hai). */
    private static List<LocalDate> buckets(Range range) {
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

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Long || value instanceof Integer || value instanceof Short) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
