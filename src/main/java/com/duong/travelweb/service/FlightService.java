package com.duong.travelweb.service;

import com.duong.travelweb.builder.FlightSearchBuilder;
import com.duong.travelweb.model.dto.AirportDTO;
import com.duong.travelweb.model.dto.FlightDTO;
import com.duong.travelweb.model.dto.FlightFilterOptionsDTO;
import com.duong.travelweb.model.dto.FlightRequestDTO;
import com.duong.travelweb.model.dto.FlightSeatDTO;
import com.duong.travelweb.model.dto.FlightSeatRequestDTO;
import com.duong.travelweb.model.dto.SeatMapGenerateRequestDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/** Chuyến bay, sơ đồ ghế (public) và quản trị chuyến bay / ghế. */
public interface FlightService {
    List<String> SEAT_CLASSES = List.of("economy", "premium_economy", "business", "first");

    Page<FlightDTO> search(FlightSearchBuilder criteria, int page, int limit);

    /** Chuyến đang bán, ngược lại 404. */
    FlightDTO getPublic(UUID flightId);

    /**
     * Sơ đồ ghế. Với khách: status chỉ còn available | occupied | blocked (không lộ held / booked).
     * @param seatClass null = mọi hạng
     */
    List<FlightSeatDTO> seats(UUID flightId, String seatClass, boolean isAdmin);

    List<AirportDTO> airports(String keyword);

    FlightFilterOptionsDTO filterOptions();

    // ---- Admin ----
    FlightDTO get(UUID flightId);

    FlightDTO create(FlightRequestDTO request);

    FlightDTO update(UUID flightId, FlightRequestDTO request);

    /** Xoá mềm: ngừng bán, vé đã đặt vẫn giữ. */
    void deactivate(UUID flightId);

    FlightSeatDTO addSeat(UUID flightId, FlightSeatRequestDTO request);

    /** Không sửa được ghế đang có người giữ / đã đặt (409). */
    FlightSeatDTO updateSeat(UUID flightId, UUID seatId, FlightSeatRequestDTO request);

    /** Chỉ xoá được ghế chưa từng gắn với vé nào (409). */
    void deleteSeat(UUID flightId, UUID seatId);

    /** Xoá mọi ghế chưa từng có vé rồi sinh lại theo các khối; ghế đã có vé được giữ nguyên. */
    List<FlightSeatDTO> generateSeatMap(UUID flightId, SeatMapGenerateRequestDTO request);
}
