package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.entity.CarEntity;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.model.entity.FlightEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.LandmarkEntity;
import com.duong.travelweb.model.entity.TourEntity;
import com.duong.travelweb.repository.CarRepository;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.repository.FlightRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.LandmarkRepository;
import com.duong.travelweb.repository.TourRepository;
import com.duong.travelweb.service.EntityReferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class EntityReferenceServiceImpl implements EntityReferenceService {
    private final HotelRepository hotelRepository;
    private final LandmarkRepository landmarkRepository;
    private final DestinationRepository destinationRepository;
    private final TourRepository tourRepository;
    private final CarRepository carRepository;
    private final FlightRepository flightRepository;

    public EntityReferenceServiceImpl(HotelRepository hotelRepository,
                                      LandmarkRepository landmarkRepository,
                                      DestinationRepository destinationRepository,
                                      TourRepository tourRepository,
                                      CarRepository carRepository,
                                      FlightRepository flightRepository) {
        this.hotelRepository = hotelRepository;
        this.landmarkRepository = landmarkRepository;
        this.destinationRepository = destinationRepository;
        this.tourRepository = tourRepository;
        this.carRepository = carRepository;
        this.flightRepository = flightRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String requireActiveName(String entityType, UUID entityId) {
        if (entityType == null || !REVIEWABLE_TYPES.contains(entityType)) {
            throw ApiException.badRequest("Loại đối tượng phải là một trong: " + String.join(", ", REVIEWABLE_TYPES));
        }
        if (entityId == null) {
            throw ApiException.badRequest("Thiếu entityId");
        }
        return switch (entityType) {
            case "hotel" -> hotelRepository.findById(entityId)
                    .filter(h -> Boolean.TRUE.equals(h.getIsActive()))
                    .map(HotelEntity::getName)
                    .orElseThrow(() -> ApiException.badRequest("Khách sạn không tồn tại"));
            case "landmark" -> landmarkRepository.findById(entityId)
                    .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                    .map(LandmarkEntity::getName)
                    .orElseThrow(() -> ApiException.badRequest("Địa danh không tồn tại"));
            case "tour" -> tourRepository.findById(entityId)
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .map(TourEntity::getName)
                    .orElseThrow(() -> ApiException.badRequest("Tour không tồn tại"));
            case "car" -> carRepository.findById(entityId)
                    .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                    .map(EntityReferenceServiceImpl::carName)
                    .orElseThrow(() -> ApiException.badRequest("Xe không tồn tại"));
            case "flight" -> flightRepository.findById(entityId)
                    .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                    .map(EntityReferenceServiceImpl::flightName)
                    .orElseThrow(() -> ApiException.badRequest("Chuyến bay không tồn tại"));
            default -> destinationRepository.findById(entityId)
                    .filter(d -> !Boolean.FALSE.equals(d.getIsActive()))
                    .map(DestinationEntity::getName)
                    .orElseThrow(() -> ApiException.badRequest("Điểm đến không tồn tại"));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<UUID, String>> resolveNames(Map<String, ? extends Collection<UUID>> idsByType) {
        Map<String, Map<UUID, String>> names = new HashMap<>();
        idsByType.forEach((type, ids) -> {
            if (ids.isEmpty() || !REVIEWABLE_TYPES.contains(type)) {
                return;
            }
            names.put(type, switch (type) {
                case "hotel" -> toNameMap(hotelRepository.findAllById(ids), HotelEntity::getId, HotelEntity::getName);
                case "landmark" -> toNameMap(landmarkRepository.findAllById(ids), LandmarkEntity::getId, LandmarkEntity::getName);
                case "tour" -> toNameMap(tourRepository.findAllById(ids), TourEntity::getId, TourEntity::getName);
                case "car" -> toNameMap(carRepository.findAllById(ids), CarEntity::getId, EntityReferenceServiceImpl::carName);
                case "flight" -> toNameMap(flightRepository.findAllById(ids), FlightEntity::getId, EntityReferenceServiceImpl::flightName);
                default -> toNameMap(destinationRepository.findAllById(ids), DestinationEntity::getId, DestinationEntity::getName);
            });
        });
        return names;
    }

    private static String carName(CarEntity car) {
        return car.getName() != null && !car.getName().isBlank() ? car.getName() : car.getBrand() + " " + car.getModel();
    }

    /** VD "Vietnam Airlines VN123 (HAN → SGN)". */
    private static String flightName(FlightEntity flight) {
        return flight.getAirline() + " " + flight.getFlightNumber() + " (" + flight.getDepartureAirportCode() + " → "
                + flight.getArrivalAirportCode() + ")";
    }

    private <T> Map<UUID, String> toNameMap(Iterable<T> entities, Function<T, UUID> id, Function<T, String> name) {
        Map<UUID, String> map = new HashMap<>();
        for (T entity : entities) {
            map.put(id.apply(entity), name.apply(entity));
        }
        return map;
    }
}
