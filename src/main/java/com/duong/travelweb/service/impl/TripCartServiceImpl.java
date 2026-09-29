package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CarBookingRequestDTO;
import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.FlightBookingRequestDTO;
import com.duong.travelweb.model.dto.TourBookingRequestDTO;
import com.duong.travelweb.model.dto.TripCartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.TripCartItemDTO;
import com.duong.travelweb.model.dto.TripCartItemRequestDTO;
import com.duong.travelweb.model.entity.CartEntity;
import com.duong.travelweb.model.entity.CartItemEntity;
import com.duong.travelweb.repository.CarRepository;
import com.duong.travelweb.repository.CartItemRepository;
import com.duong.travelweb.repository.CartRepository;
import com.duong.travelweb.repository.FlightRepository;
import com.duong.travelweb.repository.TourRepository;
import com.duong.travelweb.service.CarBookingService;
import com.duong.travelweb.service.FlightBookingService;
import com.duong.travelweb.service.TourBookingService;
import com.duong.travelweb.service.TripCartService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TripCartServiceImpl implements TripCartService {
    private static final int MAX_ITEMS = 20;
    /** Giá trị giả để qua validate DTO đặt chỗ khi thêm vào giỏ (phương thức thật chọn lúc thanh toán). */
    private static final String PLACEHOLDER_PAYMENT = "credit_card";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final TourRepository tourRepository;
    private final CarRepository carRepository;
    private final FlightRepository flightRepository;
    private final TourBookingService tourBookingService;
    private final CarBookingService carBookingService;
    private final FlightBookingService flightBookingService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public TripCartServiceImpl(CartRepository cartRepository,
                               CartItemRepository cartItemRepository,
                               TourRepository tourRepository,
                               CarRepository carRepository,
                               FlightRepository flightRepository,
                               TourBookingService tourBookingService,
                               CarBookingService carBookingService,
                               FlightBookingService flightBookingService,
                               ObjectMapper objectMapper,
                               Validator validator) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.tourRepository = tourRepository;
        this.carRepository = carRepository;
        this.flightRepository = flightRepository;
        this.tourBookingService = tourBookingService;
        this.carBookingService = carBookingService;
        this.flightBookingService = flightBookingService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripCartItemDTO> getItems(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> toDTOs(userId, cartItemRepository.findTripItems(cart.getId())))
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public List<TripCartItemDTO> addItem(UUID userId, TripCartItemRequestDTO request) {
        String type = request.getItemType();
        Object booking = parse(type, request.getBooking());
        BigDecimal price = quote(userId, type, booking); // không đặt được -> lỗi ngay, không thêm vào giỏ

        CartEntity cart = cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));
        if (cartItemRepository.findTripItems(cart.getId()).size() >= MAX_ITEMS) {
            throw ApiException.badRequest("Giỏ chuyến đi tối đa " + MAX_ITEMS + " dòng");
        }
        ObjectNode snapshot = objectMapper.valueToTree(booking);
        snapshot.remove("paymentMethod");

        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setItemType(type);
        item.setQuantity((short) 1);
        item.setUnitPrice(price);
        item.setTotalPrice(price);
        item.setItemSnapshot(objectMapper.writeValueAsString(snapshot));
        item.setAddedAt(LocalDateTime.now());
        switch (booking) {
            case TourBookingRequestDTO tour -> {
                item.setItemId(tour.getTourId());
                item.setDepartureDate(tour.getDepartureDate());
            }
            case CarBookingRequestDTO car -> {
                item.setItemId(car.getCarId());
                item.setCheckInDate(car.getPickupDate().toLocalDate());
                item.setCheckOutDate(car.getReturnDate().toLocalDate());
            }
            case FlightBookingRequestDTO flight -> item.setItemId(flight.getFlightId());
            default -> throw new IllegalStateException();
        }
        cartItemRepository.save(item);
        cart.setUpdatedAt(LocalDateTime.now());
        return toDTOs(userId, cartItemRepository.findTripItems(cart.getId()));
    }

    @Override
    @Transactional
    public List<TripCartItemDTO> removeItem(UUID userId, UUID itemId) {
        CartEntity cart = findCart(userId);
        CartItemEntity item = cartItemRepository.findTripItem(itemId, cart.getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy dòng trong giỏ"));
        cartItemRepository.delete(item);
        cartItemRepository.flush();
        cart.setUpdatedAt(LocalDateTime.now());
        return toDTOs(userId, cartItemRepository.findTripItems(cart.getId()));
    }

    @Override
    @Transactional
    public CartCheckoutDTO checkout(UUID userId, TripCartCheckoutRequestDTO request) {
        CartEntity cart = findCart(userId);
        String type = request.getItemType();
        List<CartItemEntity> ofType = cartItemRepository.findTripItems(cart.getId()).stream()
                .filter(i -> type.equals(i.getItemType()))
                .toList();
        List<CartItemEntity> selected = ofType;
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            Set<UUID> wanted = new HashSet<>(request.getItemIds());
            selected = ofType.stream().filter(i -> wanted.contains(i.getId())).toList();
            if (selected.size() != wanted.size()) {
                throw ApiException.badRequest("Có dòng không thuộc giỏ hoặc không cùng loại " + type);
            }
        }
        if (selected.isEmpty()) {
            throw ApiException.badRequest("Giỏ không có dòng " + type + " nào để thanh toán");
        }

        String method = request.getPaymentMethod();
        // Kiểm tra lại + giữ chỗ nguyên tử trong service đặt chỗ; lỗi dòng nào -> rollback, giỏ giữ nguyên.
        CartCheckoutDTO result = switch (type) {
            case "tour" -> tourBookingService.createOrder(userId, selected.stream()
                    .map(i -> withPayment((TourBookingRequestDTO) parse(type, readSnapshot(i)), method)).toList(), method);
            case "car" -> carBookingService.createOrder(userId, selected.stream()
                    .map(i -> withPayment((CarBookingRequestDTO) parse(type, readSnapshot(i)), method)).toList(), method);
            default -> flightBookingService.createOrder(userId, selected.stream()
                    .map(i -> withPayment((FlightBookingRequestDTO) parse(type, readSnapshot(i)), method)).toList(), method);
        };
        cartItemRepository.deleteAll(selected);
        cart.setUpdatedAt(LocalDateTime.now());
        return result;
    }

    // ================= helpers =================

    /** JSON -> DTO đặt chỗ tương ứng + validate như khi gọi API đặt chỗ trực tiếp. */
    private Object parse(String type, JsonNode node) {
        Class<?> target = switch (type) {
            case "tour" -> TourBookingRequestDTO.class;
            case "car" -> CarBookingRequestDTO.class;
            case "flight" -> FlightBookingRequestDTO.class;
            default -> throw ApiException.badRequest("Loại sản phẩm phải là tour, car hoặc flight");
        };
        Object dto;
        try {
            dto = objectMapper.treeToValue(node, target);
        } catch (RuntimeException e) {
            throw ApiException.badRequest("Thông tin đặt chỗ không hợp lệ");
        }
        switch (dto) {
            case TourBookingRequestDTO tour -> tour.setPaymentMethod(PLACEHOLDER_PAYMENT);
            case CarBookingRequestDTO car -> car.setPaymentMethod(PLACEHOLDER_PAYMENT);
            case FlightBookingRequestDTO flight -> flight.setPaymentMethod(PLACEHOLDER_PAYMENT);
            default -> {
            }
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw ApiException.badRequest(violations.iterator().next().getMessage());
        }
        return dto;
    }

    private BigDecimal quote(UUID userId, String type, Object booking) {
        return switch (type) {
            case "tour" -> tourBookingService.quote(userId, (TourBookingRequestDTO) booking);
            case "car" -> carBookingService.quote(userId, (CarBookingRequestDTO) booking);
            default -> flightBookingService.quote(userId, (FlightBookingRequestDTO) booking);
        };
    }

    private static <T> T withPayment(T dto, String method) {
        switch (dto) {
            case TourBookingRequestDTO tour -> tour.setPaymentMethod(method);
            case CarBookingRequestDTO car -> car.setPaymentMethod(method);
            case FlightBookingRequestDTO flight -> flight.setPaymentMethod(method);
            default -> {
            }
        }
        return dto;
    }

    private List<TripCartItemDTO> toDTOs(UUID userId, List<CartItemEntity> items) {
        List<TripCartItemDTO> result = new ArrayList<>();
        for (CartItemEntity item : items) {
            TripCartItemDTO dto = new TripCartItemDTO();
            JsonNode snapshot = readSnapshot(item);
            dto.setId(item.getId());
            dto.setItemType(item.getItemType());
            dto.setItemId(item.getItemId());
            dto.setBooking(snapshot);
            dto.setAddedPrice(item.getTotalPrice());
            dto.setAddedAt(item.getAddedAt());
            describe(dto, item, snapshot);
            try {
                BigDecimal current = quote(userId, item.getItemType(), parse(item.getItemType(), snapshot));
                dto.setCurrentPrice(current);
                dto.setPriceChanged(item.getTotalPrice().compareTo(current) != 0);
            } catch (ApiException e) {
                dto.setIssue(e.getMessage());
                dto.setPriceChanged(false);
            }
            result.add(dto);
        }
        return result;
    }

    /** Tiêu đề / mô tả / ảnh hiển thị; đối tượng đã bị xoá -> tiêu đề chung. */
    private void describe(TripCartItemDTO dto, CartItemEntity item, JsonNode snapshot) {
        switch (item.getItemType()) {
            case "tour" -> tourRepository.findById(item.getItemId()).ifPresentOrElse(tour -> {
                int adults = snapshot.path("numAdults").asInt(0);
                int children = snapshot.path("numChildren").asInt(0);
                dto.setTitle(tour.getName());
                dto.setSubtitle("Departs " + (item.getDepartureDate() == null ? "?" : item.getDepartureDate().format(DATE))
                        + " · " + adults + (adults == 1 ? " adult" : " adults")
                        + (children > 0 ? ", " + children + (children == 1 ? " child" : " children") : ""));
                dto.setImageUrl(tour.getCoverImageUrl());
            }, () -> dto.setTitle("Tour"));
            case "car" -> carRepository.findById(item.getItemId()).ifPresentOrElse(car -> {
                dto.setTitle(car.getName() != null && !car.getName().isBlank() ? car.getName() : car.getBrand() + " " + car.getModel());
                dto.setSubtitle(formatDateTime(snapshot.path("pickupDate").asText(null)) + " → "
                        + formatDateTime(snapshot.path("returnDate").asText(null)));
                dto.setImageUrl(car.getCoverImageUrl());
            }, () -> dto.setTitle("Car rental"));
            default -> flightRepository.findById(item.getItemId()).ifPresentOrElse(flight -> {
                int passengers = snapshot.path("passengers").size();
                dto.setTitle(flight.getAirline() + " " + flight.getFlightNumber());
                dto.setSubtitle(flight.getDepartureCity() + " → " + flight.getArrivalCity() + " · "
                        + flight.getDepartureTime().format(DATE_TIME) + " · " + passengers
                        + (passengers == 1 ? " passenger" : " passengers"));
                dto.setImageUrl(flight.getAirlineLogoUrl());
            }, () -> dto.setTitle("Flight"));
        }
    }

    private static String formatDateTime(String iso) {
        if (iso == null) {
            return "?";
        }
        try {
            return LocalDateTime.parse(iso).format(DATE_TIME);
        } catch (RuntimeException e) {
            return iso;
        }
    }

    private JsonNode readSnapshot(CartItemEntity item) {
        try {
            return item.getItemSnapshot() == null ? objectMapper.createObjectNode() : objectMapper.readTree(item.getItemSnapshot());
        } catch (RuntimeException e) {
            return objectMapper.createObjectNode();
        }
    }

    private CartEntity createCart(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        CartEntity cart = new CartEntity();
        cart.setUserId(userId);
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);
        return cartRepository.save(cart);
    }

    private CartEntity findCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Giỏ hàng trống"));
    }
}
