package com.duong.travelweb.service;

import com.duong.travelweb.builder.TourSearchBuilder;
import com.duong.travelweb.model.dto.TourAvailabilityDTO;
import com.duong.travelweb.model.dto.TourCarDTO;
import com.duong.travelweb.model.dto.TourDTO;
import com.duong.travelweb.model.dto.TourDayDTO;
import com.duong.travelweb.model.dto.TourFlightDTO;
import com.duong.travelweb.model.dto.TourHotelDTO;
import com.duong.travelweb.model.dto.TourLinkRequestDTO;
import com.duong.travelweb.model.dto.TourRequestDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tour trọn gói (public) và quản trị tour. tours.itinerary lưu JSON [{label, title, description}];
 * highlights (jsonb) là mảng chuỗi; included / excluded là text, mỗi dòng một mục.
 */
public interface TourService {
    Page<TourDTO> search(TourSearchBuilder criteria, int page, int limit);

    /** Tour đang bán, ngược lại 404. */
    TourDTO getPublic(UUID tourId);

    List<TourDayDTO> itinerary(UUID tourId);

    /** Số chỗ còn lại theo ngày khởi hành (null = ngày khởi hành cố định của tour). */
    TourAvailabilityDTO availability(UUID tourId, LocalDate departureDate);

    List<TourHotelDTO> hotels(UUID tourId);

    List<TourCarDTO> cars(UUID tourId);

    List<TourFlightDTO> flights(UUID tourId);

    // ---- Admin ----
    TourDTO get(UUID tourId);

    TourDTO create(TourRequestDTO request);

    TourDTO update(UUID tourId, TourRequestDTO request);

    /** Xoá mềm: ngừng bán, đơn đã đặt vẫn giữ. */
    void deactivate(UUID tourId);

    List<TourHotelDTO> linkHotel(UUID tourId, UUID hotelId, TourLinkRequestDTO request);

    void unlinkHotel(UUID tourId, UUID hotelId);

    List<TourCarDTO> linkCar(UUID tourId, UUID carId, TourLinkRequestDTO request);

    void unlinkCar(UUID tourId, UUID carId);

    List<TourFlightDTO> linkFlight(UUID tourId, UUID flightId, TourLinkRequestDTO request);

    void unlinkFlight(UUID tourId, UUID flightId);
}
