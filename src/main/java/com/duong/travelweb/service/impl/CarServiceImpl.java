package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.CarSearchBuilder;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CarDTO;
import com.duong.travelweb.model.dto.CarFilterOptionsDTO;
import com.duong.travelweb.model.dto.CarRequestDTO;
import com.duong.travelweb.model.entity.CarEntity;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.repository.CarRepository;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.service.CarService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final DestinationRepository destinationRepository;
    private final OrderFactory orderFactory;

    public CarServiceImpl(CarRepository carRepository,
                          DestinationRepository destinationRepository,
                          OrderFactory orderFactory) {
        this.carRepository = carRepository;
        this.destinationRepository = destinationRepository;
        this.orderFactory = orderFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarDTO> search(CarSearchBuilder criteria, int page, int limit) {
        if (criteria.getCarType() != null && !CAR_TYPES.contains(criteria.getCarType())) {
            throw ApiException.badRequest("Loại xe phải là một trong: " + String.join(", ", CAR_TYPES));
        }
        if (criteria.getAvailableFrom() != null && criteria.getAvailableTo() != null
                && !criteria.getAvailableTo().isAfter(criteria.getAvailableFrom())) {
            throw ApiException.badRequest("Thời gian trả xe phải sau thời gian nhận xe");
        }
        LocalDateTime holdCutoff = orderFactory.holdCutoff();
        List<CarDTO> cars = carRepository.findCars(criteria, holdCutoff, page, limit).stream().map(this::toDTO).toList();
        long total = carRepository.countCars(criteria, holdCutoff);
        return new PageImpl<>(cars, PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional(readOnly = true)
    public CarDTO getPublic(UUID carId) {
        return carRepository.findDetailById(carId)
                .filter(car -> Boolean.TRUE.equals(car.getIsActive()))
                .map(this::toDTO)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy xe"));
    }

    @Override
    @Transactional(readOnly = true)
    public CarFilterOptionsDTO filterOptions() {
        CarFilterOptionsDTO dto = new CarFilterOptionsDTO();
        dto.setCarTypes(CAR_TYPES);
        dto.setBrands(carRepository.findActiveBrands());
        dto.setTransmissions(carRepository.findActiveTransmissions());
        dto.setFuelTypes(carRepository.findActiveFuelTypes());
        List<Object[]> range = carRepository.findActivePriceRange();
        if (!range.isEmpty()) {
            dto.setPriceMin((BigDecimal) range.get(0)[0]);
            dto.setPriceMax((BigDecimal) range.get(0)[1]);
        }
        dto.setCurrencyCode(orderFactory.currencyCode());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CarDTO get(UUID carId) {
        return toDTO(findCar(carId));
    }

    @Override
    @Transactional
    public CarDTO create(CarRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        CarEntity car = new CarEntity();
        car.setCreatedAt(now);
        apply(car, request, now);
        return toDTO(carRepository.save(car));
    }

    @Override
    @Transactional
    public CarDTO update(UUID carId, CarRequestDTO request) {
        CarEntity car = findCar(carId);
        apply(car, request, LocalDateTime.now());
        return toDTO(car);
    }

    @Override
    @Transactional
    public void deactivate(UUID carId) {
        CarEntity car = findCar(carId);
        car.setIsActive(false);
        car.setUpdatedAt(LocalDateTime.now());
    }

    private void apply(CarEntity car, CarRequestDTO request, LocalDateTime now) {
        String carType = request.getCarType().trim().toLowerCase();
        if (!CAR_TYPES.contains(carType)) {
            throw ApiException.badRequest("Loại xe phải là một trong: " + String.join(", ", CAR_TYPES));
        }
        String plate = blankToNull(request.getLicensePlate());
        if (plate != null) {
            plate = plate.toUpperCase();
            if (carRepository.existsByPlate(plate.toLowerCase(), car.getId())) {
                throw ApiException.conflict("Biển số " + plate + " đã có xe khác sử dụng");
            }
        }
        DestinationEntity destination = null;
        if (request.getDestinationId() != null) {
            destination = destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> ApiException.badRequest("Điểm đến không tồn tại"));
        }
        car.setDestination(destination);
        car.setName(request.getName().trim());
        car.setBrand(blankToNull(request.getBrand()));
        car.setModel(blankToNull(request.getModel()));
        car.setLicensePlate(plate);
        car.setCarType(carType);
        car.setSeats(request.getSeats().shortValue());
        car.setTransmission(lowerOrNull(request.getTransmission()));
        car.setFuelType(lowerOrNull(request.getFuelType()));
        car.setPricePerDay(request.getPricePerDay());
        car.setWithDriver(Boolean.TRUE.equals(request.getWithDriver()));
        car.setPickupLocation(blankToNull(request.getPickupLocation()));
        car.setDescription(blankToNull(request.getDescription()));
        car.setCoverImageUrl(blankToNull(request.getCoverImageUrl()));
        car.setIsActive(request.getIsActive() == null || request.getIsActive());
        car.setUpdatedAt(now);
    }

    private CarEntity findCar(UUID carId) {
        return carRepository.findDetailById(carId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy xe"));
    }

    private CarDTO toDTO(CarEntity car) {
        CarDTO dto = new CarDTO();
        dto.setId(car.getId());
        dto.setName(car.getName());
        dto.setBrand(car.getBrand());
        dto.setModel(car.getModel());
        dto.setLicensePlate(car.getLicensePlate());
        dto.setCarType(car.getCarType());
        dto.setSeats(car.getSeats() == null ? null : car.getSeats().intValue());
        dto.setTransmission(car.getTransmission());
        dto.setFuelType(car.getFuelType());
        dto.setPricePerDay(car.getPricePerDay());
        dto.setWithDriver(Boolean.TRUE.equals(car.getWithDriver()));
        dto.setPickupLocation(car.getPickupLocation());
        dto.setDescription(car.getDescription());
        dto.setCoverImageUrl(car.getCoverImageUrl());
        dto.setIsActive(Boolean.TRUE.equals(car.getIsActive()));
        DestinationEntity destination = car.getDestination();
        if (destination != null) {
            dto.setDestinationId(destination.getId());
            dto.setDestinationName(destination.getName());
            if (destination.getCountry() != null) {
                dto.setCountryName(destination.getCountry().getName());
            }
        }
        dto.setCurrencyCode(orderFactory.currencyCode());
        dto.setCreatedAt(car.getCreatedAt());
        dto.setUpdatedAt(car.getUpdatedAt());
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String lowerOrNull(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }
}
