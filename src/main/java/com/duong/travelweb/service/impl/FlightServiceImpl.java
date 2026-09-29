package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.FlightSearchBuilder;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AirportDTO;
import com.duong.travelweb.model.dto.FlightDTO;
import com.duong.travelweb.model.dto.FlightFilterOptionsDTO;
import com.duong.travelweb.model.dto.FlightRequestDTO;
import com.duong.travelweb.model.dto.FlightSeatDTO;
import com.duong.travelweb.model.dto.FlightSeatRequestDTO;
import com.duong.travelweb.model.dto.SeatBlockRequestDTO;
import com.duong.travelweb.model.dto.SeatMapGenerateRequestDTO;
import com.duong.travelweb.model.entity.FlightEntity;
import com.duong.travelweb.model.entity.FlightSeatEntity;
import com.duong.travelweb.repository.FlightBookingRepository;
import com.duong.travelweb.repository.FlightRepository;
import com.duong.travelweb.repository.FlightSeatRepository;
import com.duong.travelweb.service.FlightService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FlightServiceImpl implements FlightService {
    private static final int MAX_SEATS_PER_FLIGHT = 1000;
    private static final List<String> ADMIN_SEAT_STATUSES = List.of("available", "blocked");

    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final FlightBookingRepository flightBookingRepository;
    private final OrderFactory orderFactory;

    public FlightServiceImpl(FlightRepository flightRepository,
                             FlightSeatRepository flightSeatRepository,
                             FlightBookingRepository flightBookingRepository,
                             OrderFactory orderFactory) {
        this.flightRepository = flightRepository;
        this.flightSeatRepository = flightSeatRepository;
        this.flightBookingRepository = flightBookingRepository;
        this.orderFactory = orderFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightDTO> search(FlightSearchBuilder criteria, int page, int limit) {
        if (criteria.getSeatClass() != null && !SEAT_CLASSES.contains(criteria.getSeatClass())) {
            throw ApiException.badRequest("Hạng ghế phải là một trong: " + String.join(", ", SEAT_CLASSES));
        }
        if (criteria.getTimeOfDay() != null && !List.of("morning", "afternoon", "evening", "night").contains(criteria.getTimeOfDay())) {
            throw ApiException.badRequest("Khung giờ phải là morning, afternoon, evening hoặc night");
        }
        if (criteria.getPassengers() != null && (criteria.getPassengers() < 1 || criteria.getPassengers() > 9)) {
            throw ApiException.badRequest("Số hành khách từ 1 đến 9");
        }
        List<FlightEntity> flights = flightRepository.findFlights(criteria, page, limit);
        long total = flightRepository.countFlights(criteria);
        return new PageImpl<>(toDTOs(flights, criteria.getSeatClass()), PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightDTO getPublic(UUID flightId) {
        FlightEntity flight = flightRepository.findById(flightId)
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));
        return toDTOs(List.of(flight), null).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightSeatDTO> seats(UUID flightId, String seatClass, boolean isAdmin) {
        FlightEntity flight = flightRepository.findById(flightId)
                .filter(f -> isAdmin || Boolean.TRUE.equals(f.getIsActive()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));
        List<FlightSeatDTO> result = new ArrayList<>();
        for (FlightSeatEntity seat : flightSeatRepository.findByFlightOrdered(flight.getId())) {
            if (seatClass != null && !seatClass.equals(seat.getSeatClass())) {
                continue;
            }
            FlightSeatDTO dto = toSeatDTO(seat);
            if (!isAdmin && ("held".equals(seat.getStatus()) || "booked".equals(seat.getStatus()))) {
                dto.setStatus("occupied");
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AirportDTO> airports(String keyword) {
        String q = keyword == null ? null : keyword.trim().toLowerCase();
        List<AirportDTO> airports = new ArrayList<>();
        for (Object[] row : flightRepository.findAirports(LocalDateTime.now())) {
            String code = (String) row[0];
            String city = (String) row[1];
            if (q == null || q.isEmpty() || code.toLowerCase().contains(q) || (city != null && city.toLowerCase().contains(q))) {
                AirportDTO dto = new AirportDTO();
                dto.setCode(code);
                dto.setCity(city);
                airports.add(dto);
            }
        }
        return airports;
    }

    @Override
    @Transactional(readOnly = true)
    public FlightFilterOptionsDTO filterOptions() {
        LocalDateTime now = LocalDateTime.now();
        FlightFilterOptionsDTO dto = new FlightFilterOptionsDTO();
        dto.setAirlines(flightRepository.findActiveAirlines(now));
        dto.setCabinClasses(SEAT_CLASSES);
        List<Object[]> range = flightRepository.findActivePriceRange(now);
        if (!range.isEmpty()) {
            dto.setPriceMin((BigDecimal) range.get(0)[0]);
            dto.setPriceMax((BigDecimal) range.get(0)[1]);
        }
        dto.setCurrencyCode(orderFactory.currencyCode());
        return dto;
    }

    // ================= Admin =================

    @Override
    @Transactional(readOnly = true)
    public FlightDTO get(UUID flightId) {
        return toDTOs(List.of(findFlight(flightId)), null).get(0);
    }

    @Override
    @Transactional
    public FlightDTO create(FlightRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        FlightEntity flight = new FlightEntity();
        flight.setTotalSeats((short) 0);
        flight.setAvailableSeats((short) 0);
        flight.setCreatedAt(now);
        apply(flight, request, now);
        return toDTOs(List.of(flightRepository.save(flight)), null).get(0);
    }

    @Override
    @Transactional
    public FlightDTO update(UUID flightId, FlightRequestDTO request) {
        FlightEntity flight = findFlight(flightId);
        apply(flight, request, LocalDateTime.now());
        return toDTOs(List.of(flight), null).get(0);
    }

    @Override
    @Transactional
    public void deactivate(UUID flightId) {
        FlightEntity flight = findFlight(flightId);
        flight.setIsActive(false);
        flight.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    public FlightSeatDTO addSeat(UUID flightId, FlightSeatRequestDTO request) {
        FlightEntity flight = findFlight(flightId);
        FlightSeatEntity seat = new FlightSeatEntity();
        seat.setFlight(flight);
        applySeat(flight, seat, request);
        seat = flightSeatRepository.save(seat);
        flightRepository.syncSeatCounts(flightId);
        return toSeatDTO(seat);
    }

    @Override
    @Transactional
    public FlightSeatDTO updateSeat(UUID flightId, UUID seatId, FlightSeatRequestDTO request) {
        FlightSeatEntity seat = findSeat(flightId, seatId);
        if ("held".equals(seat.getStatus()) || "booked".equals(seat.getStatus())) {
            throw ApiException.conflict("Ghế " + seat.getSeatNumber() + " đang có người giữ / đã đặt, không thể sửa");
        }
        applySeat(seat.getFlight(), seat, request);
        flightRepository.syncSeatCounts(flightId);
        return toSeatDTO(seat);
    }

    @Override
    @Transactional
    public void deleteSeat(UUID flightId, UUID seatId) {
        FlightSeatEntity seat = findSeat(flightId, seatId);
        if (!"available".equals(seat.getStatus()) && !"blocked".equals(seat.getStatus())) {
            throw ApiException.conflict("Ghế " + seat.getSeatNumber() + " đang có người giữ / đã đặt, không thể xoá");
        }
        // flight_bookings.seat_id là ON DELETE SET NULL: xoá ghế sẽ làm mất thông tin ghế trong lịch sử vé.
        if (flightBookingRepository.existsBySeatId(seatId)) {
            throw ApiException.conflict("Ghế " + seat.getSeatNumber() + " đã từng được đặt, hãy khoá (blocked) thay vì xoá");
        }
        flightSeatRepository.delete(seat);
        flightRepository.syncSeatCounts(flightId);
    }

    @Override
    @Transactional
    public List<FlightSeatDTO> generateSeatMap(UUID flightId, SeatMapGenerateRequestDTO request) {
        FlightEntity flight = findFlight(flightId);
        // Kiểm tra toàn bộ trước khi xoá: không để sơ đồ nửa vời.
        Set<String> planned = new LinkedHashSet<>();
        for (SeatBlockRequestDTO block : request.getBlocks()) {
            String seatClass = block.getSeatClass().trim().toLowerCase();
            if (!SEAT_CLASSES.contains(seatClass)) {
                throw ApiException.badRequest("Hạng ghế phải là một trong: " + String.join(", ", SEAT_CLASSES));
            }
            if (block.getToRow() < block.getFromRow()) {
                throw ApiException.badRequest("Hàng kết thúc phải >= hàng bắt đầu (" + block.getFromRow() + "-" + block.getToRow() + ")");
            }
            for (int row = block.getFromRow(); row <= block.getToRow(); row++) {
                for (char letter : block.getLetters().toCharArray()) {
                    if (!planned.add(row + String.valueOf(letter))) {
                        throw ApiException.badRequest("Ghế " + row + letter + " bị trùng giữa các khối");
                    }
                }
            }
            if (planned.size() > MAX_SEATS_PER_FLIGHT) {
                throw ApiException.badRequest("Tối đa " + MAX_SEATS_PER_FLIGHT + " ghế mỗi chuyến bay");
            }
        }
        flightSeatRepository.deleteUnusedSeats(flightId);
        // Ghế đã có vé (còn lại sau khi xoá) giữ nguyên, không sinh trùng số.
        Set<String> kept = new HashSet<>();
        for (FlightSeatEntity seat : flightSeatRepository.findByFlightOrdered(flightId)) {
            kept.add(seat.getSeatNumber().toUpperCase());
        }
        List<FlightSeatEntity> created = new ArrayList<>();
        for (SeatBlockRequestDTO block : request.getBlocks()) {
            for (int row = block.getFromRow(); row <= block.getToRow(); row++) {
                for (char letter : block.getLetters().toCharArray()) {
                    String number = row + String.valueOf(letter);
                    if (kept.contains(number)) {
                        continue;
                    }
                    FlightSeatEntity seat = new FlightSeatEntity();
                    seat.setFlight(flightRepository.getReferenceById(flight.getId()));
                    seat.setSeatNumber(number);
                    seat.setSeatClass(block.getSeatClass().trim().toLowerCase());
                    seat.setPrice(block.getPrice());
                    seat.setStatus("available");
                    created.add(seat);
                }
            }
        }
        flightSeatRepository.saveAll(created);
        flightRepository.syncSeatCounts(flightId);
        return seats(flightId, null, true);
    }

    // ================= helpers =================

    private void apply(FlightEntity flight, FlightRequestDTO request, LocalDateTime now) {
        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw ApiException.badRequest("Giờ đến phải sau giờ khởi hành");
        }
        String from = request.getDepartureAirportCode().trim().toUpperCase();
        String to = request.getArrivalAirportCode().trim().toUpperCase();
        if (from.equals(to)) {
            throw ApiException.badRequest("Sân bay đi và đến phải khác nhau");
        }
        flight.setFlightNumber(request.getFlightNumber().trim().toUpperCase());
        flight.setAirline(request.getAirline().trim());
        flight.setAirlineLogoUrl(blankToNull(request.getAirlineLogoUrl()));
        flight.setDepartureAirportCode(from);
        flight.setArrivalAirportCode(to);
        flight.setDepartureCity(blankToNull(request.getDepartureCity()));
        flight.setArrivalCity(blankToNull(request.getArrivalCity()));
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setDurationMinutes((int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());
        flight.setAircraftType(blankToNull(request.getAircraftType()));
        flight.setBasePrice(request.getBasePrice());
        flight.setBaggagePolicy(blankToNull(request.getBaggagePolicy()));
        flight.setIsActive(request.getIsActive() == null || request.getIsActive());
        flight.setUpdatedAt(now);
    }

    private void applySeat(FlightEntity flight, FlightSeatEntity seat, FlightSeatRequestDTO request) {
        String number = request.getSeatNumber().trim().toUpperCase();
        if (flightSeatRepository.existsSeatNumber(flight.getId(), number, seat.getId())) {
            throw ApiException.conflict("Ghế " + number + " đã có trên chuyến bay");
        }
        String seatClass = request.getSeatClass().trim().toLowerCase();
        if (!SEAT_CLASSES.contains(seatClass)) {
            throw ApiException.badRequest("Hạng ghế phải là một trong: " + String.join(", ", SEAT_CLASSES));
        }
        String status = request.getStatus() == null || request.getStatus().isBlank() ? "available" : request.getStatus().trim().toLowerCase();
        if (!ADMIN_SEAT_STATUSES.contains(status)) {
            throw ApiException.badRequest("Admin chỉ đặt được trạng thái available hoặc blocked");
        }
        seat.setSeatNumber(number);
        seat.setSeatClass(seatClass);
        seat.setPrice(request.getPrice());
        seat.setStatus(status);
    }

    private FlightEntity findFlight(UUID flightId) {
        return flightRepository.findById(flightId).orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));
    }

    private FlightSeatEntity findSeat(UUID flightId, UUID seatId) {
        return flightSeatRepository.findById(seatId)
                .filter(s -> s.getFlight().getId().equals(flightId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy ghế"));
    }

    /** Gắn giá thấp nhất còn trống ("From") và các hạng còn ghế, theo lô. */
    private List<FlightDTO> toDTOs(List<FlightEntity> flights, String seatClass) {
        if (flights.isEmpty()) {
            return List.of();
        }
        Map<UUID, BigDecimal> fromPrice = new HashMap<>();
        Map<UUID, List<String>> classes = new HashMap<>();
        for (Object[] row : flightSeatRepository.summarizeAvailable(flights.stream().map(FlightEntity::getId).toList())) {
            UUID flightId = (UUID) row[0];
            String cls = (String) row[1];
            BigDecimal min = (BigDecimal) row[2];
            classes.computeIfAbsent(flightId, k -> new ArrayList<>()).add(cls);
            if (seatClass == null || seatClass.equals(cls)) {
                fromPrice.merge(flightId, min, BigDecimal::min);
            }
        }
        List<FlightDTO> result = new ArrayList<>();
        for (FlightEntity flight : flights) {
            FlightDTO dto = new FlightDTO();
            dto.setId(flight.getId());
            dto.setFlightNumber(flight.getFlightNumber());
            dto.setAirline(flight.getAirline());
            dto.setAirlineLogoUrl(flight.getAirlineLogoUrl());
            dto.setDepartureAirportCode(flight.getDepartureAirportCode());
            dto.setArrivalAirportCode(flight.getArrivalAirportCode());
            dto.setDepartureCity(flight.getDepartureCity());
            dto.setArrivalCity(flight.getArrivalCity());
            dto.setDepartureTime(flight.getDepartureTime());
            dto.setArrivalTime(flight.getArrivalTime());
            dto.setDurationMinutes(flight.getDurationMinutes());
            dto.setAircraftType(flight.getAircraftType());
            dto.setBasePrice(flight.getBasePrice());
            dto.setTotalSeats(flight.getTotalSeats() == null ? 0 : flight.getTotalSeats().intValue());
            dto.setAvailableSeats(flight.getAvailableSeats() == null ? 0 : flight.getAvailableSeats().intValue());
            dto.setBaggagePolicy(flight.getBaggagePolicy());
            dto.setIsActive(Boolean.TRUE.equals(flight.getIsActive()));
            dto.setFromPrice(fromPrice.getOrDefault(flight.getId(), flight.getBasePrice()));
            List<String> available = classes.getOrDefault(flight.getId(), List.of());
            dto.setCabinClasses(SEAT_CLASSES.stream().filter(available::contains).toList());
            dto.setCurrencyCode(orderFactory.currencyCode());
            dto.setCreatedAt(flight.getCreatedAt());
            dto.setUpdatedAt(flight.getUpdatedAt());
            result.add(dto);
        }
        return result;
    }

    private FlightSeatDTO toSeatDTO(FlightSeatEntity seat) {
        FlightSeatDTO dto = new FlightSeatDTO();
        dto.setId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatClass(seat.getSeatClass());
        dto.setPrice(seat.getPrice());
        dto.setStatus(seat.getStatus());
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
