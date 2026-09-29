package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.TourSearchBuilder;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.TourAvailabilityDTO;
import com.duong.travelweb.model.dto.TourCarDTO;
import com.duong.travelweb.model.dto.TourDTO;
import com.duong.travelweb.model.dto.TourDayDTO;
import com.duong.travelweb.model.dto.TourFlightDTO;
import com.duong.travelweb.model.dto.TourHotelDTO;
import com.duong.travelweb.model.dto.TourLinkRequestDTO;
import com.duong.travelweb.model.dto.TourRequestDTO;
import com.duong.travelweb.model.entity.CarEntity;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.model.entity.FlightEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.TourEntity;
import com.duong.travelweb.repository.CarRepository;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.repository.FlightRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.TourBookingRepository;
import com.duong.travelweb.repository.TourRepository;
import com.duong.travelweb.service.TourService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class TourServiceImpl implements TourService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<TourDayDTO>> DAY_LIST = new TypeReference<>() {
    };

    private final TourRepository tourRepository;
    private final TourBookingRepository tourBookingRepository;
    private final DestinationRepository destinationRepository;
    private final HotelRepository hotelRepository;
    private final CarRepository carRepository;
    private final FlightRepository flightRepository;
    private final OrderFactory orderFactory;
    private final ObjectMapper objectMapper;

    public TourServiceImpl(TourRepository tourRepository,
                           TourBookingRepository tourBookingRepository,
                           DestinationRepository destinationRepository,
                           HotelRepository hotelRepository,
                           CarRepository carRepository,
                           FlightRepository flightRepository,
                           OrderFactory orderFactory,
                           ObjectMapper objectMapper) {
        this.tourRepository = tourRepository;
        this.tourBookingRepository = tourBookingRepository;
        this.destinationRepository = destinationRepository;
        this.hotelRepository = hotelRepository;
        this.carRepository = carRepository;
        this.flightRepository = flightRepository;
        this.orderFactory = orderFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourDTO> search(TourSearchBuilder criteria, int page, int limit) {
        List<TourEntity> tours = tourRepository.findTours(criteria, page, limit);
        long total = tourRepository.countTours(criteria);
        Map<UUID, Integer> hotelCounts = new HashMap<>();
        if (!tours.isEmpty()) {
            for (Object[] row : tourRepository.countHotels(tours.stream().map(TourEntity::getId).toList())) {
                hotelCounts.put((UUID) row[0], ((Number) row[1]).intValue());
            }
        }
        List<TourDTO> dtos = tours.stream().map(t -> toDTO(t, hotelCounts.getOrDefault(t.getId(), 0), false)).toList();
        return new PageImpl<>(dtos, PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional(readOnly = true)
    public TourDTO getPublic(UUID tourId) {
        TourEntity tour = findActive(tourId);
        return toDTO(tour, tourRepository.findHotelLinks(tourId).size(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourDayDTO> itinerary(UUID tourId) {
        return readItinerary(findActive(tourId).getItinerary());
    }

    @Override
    @Transactional(readOnly = true)
    public TourAvailabilityDTO availability(UUID tourId, LocalDate departureDate) {
        TourEntity tour = findActive(tourId);
        LocalDate date = departureDate != null ? departureDate : tour.getDepartureDate();
        if (date == null) {
            throw ApiException.badRequest("Vui lòng chọn ngày khởi hành");
        }
        long booked = tourBookingRepository.countBookedGuests(tourId, date, null, orderFactory.holdCutoff());
        TourAvailabilityDTO dto = new TourAvailabilityDTO();
        dto.setTourId(tourId);
        dto.setDepartureDate(date);
        dto.setBooked((int) booked);
        if (tour.getMaxParticipants() != null) {
            int spots = (int) Math.max(0, tour.getMaxParticipants() - booked);
            dto.setMaxParticipants(tour.getMaxParticipants().intValue());
            dto.setSpotsLeft(spots);
            dto.setAvailable(spots > 0);
        } else {
            dto.setAvailable(true);
        }
        dto.setPriceAdult(tour.getPriceAdult());
        dto.setPriceChild(tour.getPriceChild() != null ? tour.getPriceChild() : tour.getPriceAdult());
        dto.setCurrencyCode(orderFactory.currencyCode());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourHotelDTO> hotels(UUID tourId) {
        findTour(tourId);
        List<Object[]> links = tourRepository.findHotelLinks(tourId);
        Map<UUID, HotelEntity> hotels = byId(hotelRepository.findAllById(links.stream().map(r -> (UUID) r[0]).toList()), HotelEntity::getId);
        List<TourHotelDTO> result = new ArrayList<>();
        for (Object[] link : links) {
            HotelEntity hotel = hotels.get((UUID) link[0]);
            if (hotel == null) {
                continue;
            }
            TourHotelDTO dto = new TourHotelDTO();
            dto.setHotelId(hotel.getId());
            dto.setName(hotel.getName());
            dto.setDestinationName(hotel.getDestination() != null ? hotel.getDestination().getName() : null);
            dto.setStarRating(hotel.getStarRating());
            dto.setImageUrl(hotel.getCoverImageUrl());
            dto.setDescription(hotel.getDescription());
            dto.setCheckInDay(toInteger(link[1]));
            dto.setNights(toInteger(link[2]));
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourCarDTO> cars(UUID tourId) {
        findTour(tourId);
        List<Object[]> links = tourRepository.findCarLinks(tourId);
        Map<UUID, CarEntity> cars = byId(carRepository.findAllById(links.stream().map(r -> (UUID) r[0]).toList()), CarEntity::getId);
        List<TourCarDTO> result = new ArrayList<>();
        for (Object[] link : links) {
            CarEntity car = cars.get((UUID) link[0]);
            if (car == null) {
                continue;
            }
            TourCarDTO dto = new TourCarDTO();
            dto.setCarId(car.getId());
            dto.setName(car.getName());
            dto.setCarType(car.getCarType());
            dto.setSeats(car.getSeats() == null ? null : car.getSeats().intValue());
            dto.setWithDriver(Boolean.TRUE.equals(car.getWithDriver()));
            dto.setImageUrl(car.getCoverImageUrl());
            dto.setUsageDay(toInteger(link[1]));
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourFlightDTO> flights(UUID tourId) {
        findTour(tourId);
        List<Object[]> links = tourRepository.findFlightLinks(tourId);
        Map<UUID, FlightEntity> flights = byId(flightRepository.findAllById(links.stream().map(r -> (UUID) r[0]).toList()), FlightEntity::getId);
        List<TourFlightDTO> result = new ArrayList<>();
        for (Object[] link : links) {
            FlightEntity flight = flights.get((UUID) link[0]);
            if (flight == null) {
                continue;
            }
            TourFlightDTO dto = new TourFlightDTO();
            dto.setFlightId(flight.getId());
            dto.setFlightNumber(flight.getFlightNumber());
            dto.setAirline(flight.getAirline());
            dto.setDepartureAirportCode(flight.getDepartureAirportCode());
            dto.setArrivalAirportCode(flight.getArrivalAirportCode());
            dto.setDepartureTime(flight.getDepartureTime());
            dto.setArrivalTime(flight.getArrivalTime());
            dto.setLeg((String) link[1]);
            result.add(dto);
        }
        result.sort((a, b) -> a.getDepartureTime().compareTo(b.getDepartureTime()));
        return result;
    }

    // ================= Admin =================

    @Override
    @Transactional(readOnly = true)
    public TourDTO get(UUID tourId) {
        TourEntity tour = findTour(tourId);
        return toDTO(tour, tourRepository.findHotelLinks(tourId).size(), true);
    }

    @Override
    @Transactional
    public TourDTO create(TourRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        TourEntity tour = new TourEntity();
        tour.setCreatedAt(now);
        apply(tour, request, now);
        return toDTO(tourRepository.save(tour), 0, true);
    }

    @Override
    @Transactional
    public TourDTO update(UUID tourId, TourRequestDTO request) {
        TourEntity tour = findTour(tourId);
        apply(tour, request, LocalDateTime.now());
        return toDTO(tour, tourRepository.findHotelLinks(tourId).size(), true);
    }

    @Override
    @Transactional
    public void deactivate(UUID tourId) {
        TourEntity tour = findTour(tourId);
        tour.setIsActive(false);
        tour.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    public List<TourHotelDTO> linkHotel(UUID tourId, UUID hotelId, TourLinkRequestDTO request) {
        TourEntity tour = findTour(tourId);
        hotelRepository.findById(hotelId).orElseThrow(() -> ApiException.badRequest("Khách sạn không tồn tại"));
        Integer checkInDay = request == null ? null : request.getCheckInDay();
        Integer nights = request == null ? null : request.getNights();
        if (checkInDay != null && tour.getDurationDays() != null && checkInDay > tour.getDurationDays()) {
            throw ApiException.badRequest("Ngày nhận phòng vượt quá số ngày của tour (" + tour.getDurationDays() + ")");
        }
        tourRepository.upsertHotel(tourId, hotelId, toShort(checkInDay), toShort(nights));
        return hotels(tourId);
    }

    @Override
    @Transactional
    public void unlinkHotel(UUID tourId, UUID hotelId) {
        findTour(tourId);
        if (tourRepository.deleteHotel(tourId, hotelId) == 0) {
            throw ApiException.notFound("Khách sạn không thuộc tour");
        }
    }

    @Override
    @Transactional
    public List<TourCarDTO> linkCar(UUID tourId, UUID carId, TourLinkRequestDTO request) {
        TourEntity tour = findTour(tourId);
        carRepository.findById(carId).orElseThrow(() -> ApiException.badRequest("Xe không tồn tại"));
        Integer usageDay = request == null ? null : request.getUsageDay();
        if (usageDay != null && tour.getDurationDays() != null && usageDay > tour.getDurationDays()) {
            throw ApiException.badRequest("Ngày dùng xe vượt quá số ngày của tour (" + tour.getDurationDays() + ")");
        }
        tourRepository.upsertCar(tourId, carId, toShort(usageDay));
        return cars(tourId);
    }

    @Override
    @Transactional
    public void unlinkCar(UUID tourId, UUID carId) {
        findTour(tourId);
        if (tourRepository.deleteCar(tourId, carId) == 0) {
            throw ApiException.notFound("Xe không thuộc tour");
        }
    }

    @Override
    @Transactional
    public List<TourFlightDTO> linkFlight(UUID tourId, UUID flightId, TourLinkRequestDTO request) {
        findTour(tourId);
        flightRepository.findById(flightId).orElseThrow(() -> ApiException.badRequest("Chuyến bay không tồn tại"));
        String leg = request == null || request.getLeg() == null || request.getLeg().isBlank() ? null : request.getLeg().trim().toLowerCase();
        tourRepository.upsertFlight(tourId, flightId, leg);
        return flights(tourId);
    }

    @Override
    @Transactional
    public void unlinkFlight(UUID tourId, UUID flightId) {
        findTour(tourId);
        if (tourRepository.deleteFlight(tourId, flightId) == 0) {
            throw ApiException.notFound("Chuyến bay không thuộc tour");
        }
    }

    // ================= helpers =================

    private void apply(TourEntity tour, TourRequestDTO request, LocalDateTime now) {
        if (request.getDepartureDate() != null && request.getReturnDate() != null
                && request.getReturnDate().isBefore(request.getDepartureDate())) {
            throw ApiException.badRequest("Ngày về phải sau hoặc bằng ngày khởi hành");
        }
        if (request.getMinParticipants() != null && request.getMaxParticipants() != null
                && request.getMinParticipants() > request.getMaxParticipants()) {
            throw ApiException.badRequest("Số khách tối thiểu không được lớn hơn số chỗ tối đa");
        }
        DestinationEntity destination = null;
        if (request.getDestinationId() != null) {
            destination = destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> ApiException.badRequest("Điểm đến không tồn tại"));
        }
        String name = request.getName().trim();
        tour.setName(name);
        tour.setSlug(uniqueSlug(name, tour.getId()));
        tour.setDestination(destination);
        tour.setDescription(blankToNull(request.getDescription()));
        tour.setDurationDays(request.getDurationDays().shortValue());
        tour.setDurationNights(request.getDurationNights() != null ? request.getDurationNights().shortValue()
                : (short) Math.max(0, request.getDurationDays() - 1));
        tour.setMinParticipants(request.getMinParticipants() != null ? request.getMinParticipants().shortValue() : (short) 1);
        tour.setMaxParticipants(request.getMaxParticipants() == null ? null : request.getMaxParticipants().shortValue());
        tour.setPriceAdult(request.getPriceAdult());
        tour.setPriceChild(request.getPriceChild());
        tour.setDepartureDate(request.getDepartureDate());
        tour.setReturnDate(request.getDepartureDate() != null && request.getReturnDate() == null
                ? request.getDepartureDate().plusDays(request.getDurationDays() - 1L) : request.getReturnDate());
        tour.setDepartureLocation(blankToNull(request.getDepartureLocation()));
        tour.setCoverImageUrl(blankToNull(request.getCoverImageUrl()));
        tour.setHighlights(writeJson(cleanList(request.getHighlights())));
        tour.setIncluded(joinLines(request.getIncluded()));
        tour.setExcluded(joinLines(request.getExcluded()));
        List<TourDayDTO> days = request.getItinerary() == null ? List.of() : request.getItinerary();
        tour.setItinerary(days.isEmpty() ? null : writeJson(days));
        tour.setIsActive(request.getIsActive() == null || request.getIsActive());
        tour.setUpdatedAt(now);
    }

    /** Slug không dấu, duy nhất (thêm -2, -3... nếu trùng). */
    private String uniqueSlug(String name, UUID excludeId) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isEmpty()) {
            base = "tour";
        }
        if (base.length() > 240) {
            base = base.substring(0, 240);
        }
        String slug = base;
        for (int i = 2; tourRepository.existsBySlug(slug, excludeId); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    private TourEntity findTour(UUID tourId) {
        return tourRepository.findDetailById(tourId).orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
    }

    private TourEntity findActive(UUID tourId) {
        return tourRepository.findDetailById(tourId)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
    }

    private TourDTO toDTO(TourEntity tour, int hotelCount, boolean withDetails) {
        TourDTO dto = new TourDTO();
        dto.setId(tour.getId());
        dto.setName(tour.getName());
        dto.setSlug(tour.getSlug());
        dto.setDescription(tour.getDescription());
        DestinationEntity destination = tour.getDestination();
        if (destination != null) {
            dto.setDestinationId(destination.getId());
            dto.setDestinationName(destination.getName());
            if (destination.getCountry() != null) {
                dto.setCountryName(destination.getCountry().getName());
            }
        }
        dto.setDurationDays(toInteger(tour.getDurationDays()));
        dto.setDurationNights(toInteger(tour.getDurationNights()));
        dto.setMinParticipants(toInteger(tour.getMinParticipants()));
        dto.setMaxParticipants(toInteger(tour.getMaxParticipants()));
        dto.setPriceAdult(tour.getPriceAdult());
        dto.setPriceChild(tour.getPriceChild());
        dto.setDepartureDate(tour.getDepartureDate());
        dto.setReturnDate(tour.getReturnDate());
        dto.setDepartureLocation(tour.getDepartureLocation());
        dto.setCoverImageUrl(tour.getCoverImageUrl());
        dto.setHighlights(readStringList(tour.getHighlights()));
        dto.setIsActive(Boolean.TRUE.equals(tour.getIsActive()));
        dto.setHotelCount(hotelCount);
        dto.setCurrencyCode(orderFactory.currencyCode());
        if (withDetails) {
            dto.setIncluded(splitLines(tour.getIncluded()));
            dto.setExcluded(splitLines(tour.getExcluded()));
            dto.setItinerary(readItinerary(tour.getItinerary()));
            if (tour.getDepartureDate() != null && tour.getMaxParticipants() != null) {
                long booked = tourBookingRepository.countBookedGuests(tour.getId(), tour.getDepartureDate(), null, orderFactory.holdCutoff());
                dto.setSpotsLeft((int) Math.max(0, tour.getMaxParticipants() - booked));
            }
        }
        dto.setCreatedAt(tour.getCreatedAt());
        dto.setUpdatedAt(tour.getUpdatedAt());
        return dto;
    }

    /** JSON [{label,title,description}]; dữ liệu cũ dạng text thường thì trả về một chặng duy nhất. */
    private List<TourDayDTO> readItinerary(String itinerary) {
        if (itinerary == null || itinerary.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(itinerary, DAY_LIST);
        } catch (RuntimeException e) {
            TourDayDTO day = new TourDayDTO();
            day.setLabel("Overview");
            day.setTitle("Itinerary");
            day.setDescription(itinerary);
            return List.of(day);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        if (value instanceof List<?> list && list.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    private static List<String> cleanList(List<String> values) {
        return values == null ? List.of() : values.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    private static String joinLines(List<String> values) {
        List<String> clean = cleanList(values);
        return clean.isEmpty() ? null : String.join("\n", clean);
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static <T> Map<UUID, T> byId(Iterable<T> entities, Function<T, UUID> id) {
        Map<UUID, T> map = new HashMap<>();
        entities.forEach(e -> map.put(id.apply(e), e));
        return map;
    }

    private static Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
