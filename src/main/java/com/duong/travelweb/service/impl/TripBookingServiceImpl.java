package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CarBookingRequestDTO;
import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.TripBookingDTO;
import com.duong.travelweb.model.dto.TripBookingLineDTO;
import com.duong.travelweb.model.dto.TripBookingOrderDTO;
import com.duong.travelweb.model.dto.TripBookingQuoteDTO;
import com.duong.travelweb.model.dto.TripBookingRequestDTO;
import com.duong.travelweb.model.dto.TripCarOptionDTO;
import com.duong.travelweb.model.dto.TripHotelQuoteDTO;
import com.duong.travelweb.model.dto.TripRoomOptionDTO;
import com.duong.travelweb.model.entity.CarBookingEntity;
import com.duong.travelweb.model.entity.CarEntity;
import com.duong.travelweb.model.entity.HotelBookingEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.ItineraryEntity;
import com.duong.travelweb.model.entity.ItineraryItemEntity;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.CarBookingRepository;
import com.duong.travelweb.repository.CarRepository;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.ItineraryItemRepository;
import com.duong.travelweb.repository.ItineraryRepository;
import com.duong.travelweb.repository.OrderRepository;
import com.duong.travelweb.repository.RoomTypeRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.CarBookingService;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.service.PaymentService;
import com.duong.travelweb.service.TripBookingService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Đặt trọn gói từ lịch trình (khách sạn + xe). Không đổi kiến trúc order: mỗi loại một order + một payment, do
 * HotelBookingService / CarBookingService tạo (cùng quy tắc giữ chỗ, giá, chính sách huỷ, hoá đơn như đặt lẻ),
 * gọi trong MỘT transaction → một phần lỗi (hết phòng, xe bị giữ) thì không order nào được tạo.
 * Các order của một lượt đặt mang cùng notes "itinerary:<id> batch:<uuid>" để đọc lại / thanh toán / huỷ cùng nhau.
 */
@Service
public class TripBookingServiceImpl implements TripBookingService {
    private static final String NOTE_PREFIX = "itinerary:";
    private static final LocalTime CAR_PICKUP_TIME = LocalTime.of(8, 0);
    private static final LocalTime CAR_RETURN_TIME = LocalTime.of(20, 0);
    private static final LocalTime DEFAULT_CHECK_IN = LocalTime.of(14, 0);
    private static final int DEFAULT_CANCELLATION_HOURS = 24;
    private static final int DEFAULT_ROOM_OCCUPANCY = 2;
    /** Trạng thái order còn hiệu lực (đang giữ chỗ hoặc đã thanh toán) → không cho đặt thêm lượt mới. */
    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of("pending", "paid", "partially_refunded");

    private final ItineraryRepository itineraryRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CarRepository carRepository;
    private final CarBookingRepository carBookingRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final HotelBookingService hotelBookingService;
    private final CarBookingService carBookingService;
    private final PaymentService paymentService;
    private final OrderFactory orderFactory;
    private final OrderBookingRouter orderBookingRouter;
    private final EntityManager entityManager;
    private final boolean mockPaymentEnabled;
    private final long holdMinutes;

    public TripBookingServiceImpl(ItineraryRepository itineraryRepository,
                                  ItineraryItemRepository itineraryItemRepository,
                                  HotelRepository hotelRepository,
                                  RoomTypeRepository roomTypeRepository,
                                  CarRepository carRepository,
                                  CarBookingRepository carBookingRepository,
                                  HotelBookingRepository hotelBookingRepository,
                                  OrderRepository orderRepository,
                                  UserRepository userRepository,
                                  HotelBookingService hotelBookingService,
                                  CarBookingService carBookingService,
                                  PaymentService paymentService,
                                  OrderFactory orderFactory,
                                  OrderBookingRouter orderBookingRouter,
                                  EntityManager entityManager,
                                  @Value("${app.payment.mock-enabled:false}") boolean mockPaymentEnabled,
                                  @Value("${app.booking.hold-minutes:15}") long holdMinutes) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.carRepository = carRepository;
        this.carBookingRepository = carBookingRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.hotelBookingService = hotelBookingService;
        this.carBookingService = carBookingService;
        this.paymentService = paymentService;
        this.orderFactory = orderFactory;
        this.orderBookingRouter = orderBookingRouter;
        this.entityManager = entityManager;
        this.mockPaymentEnabled = mockPaymentEnabled;
        this.holdMinutes = holdMinutes;
    }

    /** Lịch trình đủ điều kiện đặt: có điểm đến, ngày đi chưa qua. */
    private record Trip(ItineraryEntity itinerary, LocalDate start, LocalDate end, int nights, int partySize) {
    }

    // ================================================================== báo giá

    @Override
    @Transactional(readOnly = true)
    public TripBookingQuoteDTO quote(UUID userId, UUID itineraryId, UUID hotelId) {
        Trip trip = loadTrip(userId, itineraryId);
        ItineraryEntity itinerary = trip.itinerary();
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));

        TripBookingQuoteDTO dto = new TripBookingQuoteDTO();
        dto.setItineraryId(itinerary.getId());
        dto.setItineraryTitle(itinerary.getTitle());
        dto.setItineraryStatus(itinerary.getStatus());
        dto.setDestinationId(itinerary.getDestination().getId());
        dto.setDestinationName(itinerary.getDestination().getName());
        dto.setStartDate(trip.start());
        dto.setEndDate(trip.end());
        dto.setPartySize(trip.partySize());
        dto.setCurrencyCode(orderFactory.currencyCode());
        dto.setHoldMinutes(holdMinutes);
        dto.setMockPaymentEnabled(mockPaymentEnabled);
        dto.setCustomerName(user.getFullName());
        dto.setCustomerEmail(user.getEmail());
        dto.setCustomerPhone(user.getPhone());
        dto.setCarFreeCancellationHours(CarBookingService.FREE_CANCELLATION_HOURS);

        List<ItineraryItemEntity> items = itineraryItemRepository.findByItineraryId(itinerary.getId());
        BigDecimal onSite = BigDecimal.ZERO;
        for (ItineraryItemEntity item : items) {
            if (item.getEstimatedCost() != null && !"hotel".equals(item.getEntityType())) {
                onSite = onSite.add(item.getEstimatedCost());
            }
        }
        dto.setOnSiteEstimate(onSite);

        // Khách sạn
        if (trip.nights() < 1) {
            dto.setHotelUnavailableReason("day_trip");
        } else {
            UUID chosenHotel = hotelId != null ? hotelId : itineraryHotelId(items);
            if (chosenHotel == null) {
                dto.setHotelUnavailableReason("no_hotel_in_itinerary");
            } else {
                HotelEntity hotel = hotelRepository.findById(chosenHotel)
                        .filter(h -> Boolean.TRUE.equals(h.getIsActive()))
                        .orElse(null);
                if (hotel == null) {
                    dto.setHotelUnavailableReason("hotel_inactive");
                } else {
                    dto.setHotel(hotelQuote(hotel, trip));
                    if (dto.getHotel().getRecommendedRoomTypeId() == null) {
                        dto.setHotelUnavailableReason("sold_out");
                    }
                }
            }
        }

        // Xe tại điểm đến
        LocalDateTime[] window = carWindow(trip);
        if (window != null) {
            dto.setCarPickupDate(window[0]);
            dto.setCarReturnDate(window[1]);
            for (CarEntity car : carRepository.findActiveByDestinationId(itinerary.getDestination().getId())) {
                dto.getCars().add(carOption(car, window[0], window[1], trip.partySize()));
            }
            dto.getCars().stream()
                    .filter(c -> c.isAvailable() && c.isFitsParty())
                    // Khách du lịch ưu tiên xe có tài xế, sau đó rẻ nhất.
                    .min(Comparator.comparing((TripCarOptionDTO c) -> !Boolean.TRUE.equals(c.getWithDriver()))
                            .thenComparing(TripCarOptionDTO::getSubtotal))
                    .ifPresent(c -> dto.setRecommendedCarId(c.getCarId()));
        }
        return dto;
    }

    private TripHotelQuoteDTO hotelQuote(HotelEntity hotel, Trip trip) {
        TripHotelQuoteDTO dto = new TripHotelQuoteDTO();
        dto.setHotelId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setAddress(hotel.getAddress());
        dto.setStarRating(hotel.getStarRating());
        dto.setCoverImageUrl(hotel.getCoverImageUrl());
        dto.setCheckIn(trip.start());
        dto.setCheckOut(trip.end());
        dto.setCheckInTime(hotel.getCheckInTime());
        dto.setCheckOutTime(hotel.getCheckOutTime());
        dto.setNights(trip.nights());
        dto.setBreakfastIncluded(hotel.getBreakfastIncluded());
        dto.setCancellationPolicy(hotel.getCancellationPolicy());
        int hours = hotel.getCancellationHours() != null ? hotel.getCancellationHours() : DEFAULT_CANCELLATION_HOURS;
        dto.setCancellationHours(hours);
        // Cùng công thức HotelBookingServiceImpl.isFreeCancellation: giờ nhận phòng ngày đến − cancellation_hours.
        LocalDateTime deadline = trip.start().atTime(parseTime(hotel.getCheckInTime(), DEFAULT_CHECK_IN)).minusHours(hours);
        dto.setFreeCancellationUntil(deadline.isAfter(LocalDateTime.now()) ? deadline : null);

        TripRoomOptionDTO best = null;
        for (RoomTypeEntity roomType : roomTypeRepository.findActiveByHotelId(hotel.getId())) {
            TripRoomOptionDTO option = new TripRoomOptionDTO();
            int capacity = roomType.getMaxOccupancy() != null && roomType.getMaxOccupancy() > 0
                    ? roomType.getMaxOccupancy() : DEFAULT_ROOM_OCCUPANCY;
            int needed = (trip.partySize() + capacity - 1) / capacity;
            int available = hotelBookingService.countBookableRooms(roomType.getId(), trip.start(), trip.end());
            BigDecimal price = roomType.getPricePerNight() != null ? BigDecimal.valueOf(roomType.getPricePerNight()) : BigDecimal.ZERO;
            option.setRoomTypeId(roomType.getId());
            option.setName(roomType.getName());
            option.setBedType(roomType.getBedType());
            option.setMaxOccupancy(capacity);
            option.setAreaSqm(roomType.getAreaSqM());
            option.setCoverImageUrl(roomType.getCoverImageUrl());
            option.setPricePerNight(price);
            option.setAvailableRooms(available);
            option.setRoomsNeeded(needed);
            option.setSubtotal(price.multiply(BigDecimal.valueOf((long) needed * trip.nights())));
            option.setFeasible(needed <= available && price.signum() > 0);
            dto.getRoomOptions().add(option);
            if (option.isFeasible() && (best == null || option.getSubtotal().compareTo(best.getSubtotal()) < 0
                    || (option.getSubtotal().compareTo(best.getSubtotal()) == 0 && needed < best.getRoomsNeeded()))) {
                best = option;
            }
        }
        dto.getRoomOptions().sort(Comparator.comparing(TripRoomOptionDTO::isFeasible).reversed()
                .thenComparing(TripRoomOptionDTO::getSubtotal));
        dto.setRecommendedRoomTypeId(best == null ? null : best.getRoomTypeId());
        return dto;
    }

    private TripCarOptionDTO carOption(CarEntity car, LocalDateTime pickup, LocalDateTime dropOff, int partySize) {
        TripCarOptionDTO dto = new TripCarOptionDTO();
        int days = CarBookingServiceImpl.rentalDays(pickup, dropOff);
        dto.setCarId(car.getId());
        dto.setName(car.getName());
        dto.setBrand(car.getBrand());
        dto.setModel(car.getModel());
        dto.setCarType(car.getCarType());
        dto.setSeats(car.getSeats() == null ? null : car.getSeats().intValue());
        dto.setTransmission(car.getTransmission());
        dto.setFuelType(car.getFuelType());
        dto.setWithDriver(car.getWithDriver());
        dto.setPickupLocation(car.getPickupLocation());
        dto.setCoverImageUrl(car.getCoverImageUrl());
        dto.setPricePerDay(car.getPricePerDay());
        dto.setRentalDays(days);
        dto.setSubtotal(car.getPricePerDay().multiply(BigDecimal.valueOf(days)));
        dto.setAvailable(carBookingRepository.findBlocking(car.getId(), null, pickup, dropOff, orderFactory.holdCutoff()).isEmpty());
        dto.setFitsParty(car.getSeats() == null || car.getSeats() >= partySize);
        LocalDateTime deadline = pickup.minusHours(CarBookingService.FREE_CANCELLATION_HOURS);
        dto.setFreeCancellationUntil(deadline.isAfter(LocalDateTime.now()) ? deadline : null);
        return dto;
    }

    /**
     * Thời gian thuê xe mặc định: 08:00 ngày đi → 20:00 ngày về; nhận xe phải sau hiện tại ≥ 1 giờ (quy tắc thuê xe)
     * → nếu đi hôm nay thì lùi giờ nhận. Không còn khoảng hợp lệ → null.
     */
    private LocalDateTime[] carWindow(Trip trip) {
        LocalDateTime earliest = LocalDateTime.now().plusMinutes(90).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime pickup = trip.start().atTime(CAR_PICKUP_TIME);
        if (pickup.isBefore(earliest)) {
            pickup = earliest;
        }
        LocalDateTime dropOff = trip.end().atTime(CAR_RETURN_TIME);
        return dropOff.isAfter(pickup) ? new LocalDateTime[]{pickup, dropOff} : null;
    }

    // ================================================================== đặt

    @Override
    @Transactional
    public TripBookingDTO book(UUID userId, UUID itineraryId, TripBookingRequestDTO request) {
        Trip trip = loadTrip(userId, itineraryId);
        List<OrderEntity> latest = latestBatch(userId, itineraryId);
        if (latest.stream().anyMatch(o -> ACTIVE_ORDER_STATUSES.contains(o.getStatus()))) {
            boolean pending = latest.stream().anyMatch(o -> "pending".equals(o.getStatus()));
            throw ApiException.conflict(pending
                    ? "Lịch trình đang có đơn chờ thanh toán — hãy thanh toán hoặc huỷ đơn đó trước"
                    : "Lịch trình đã được đặt; huỷ các đơn hiện tại trước khi đặt lại");
        }
        if (request.getRoomTypeId() == null && request.getCarId() == null) {
            throw ApiException.badRequest("Vui lòng chọn ít nhất khách sạn hoặc xe để đặt");
        }

        String note = NOTE_PREFIX + itineraryId + " batch:" + UUID.randomUUID();
        List<UUID> orderIds = new ArrayList<>();

        if (request.getRoomTypeId() != null) {
            if (trip.nights() < 1) {
                throw ApiException.badRequest("Chuyến đi trong ngày không cần đặt phòng khách sạn");
            }
            UUID hotelId = request.getHotelId() != null ? request.getHotelId()
                    : itineraryHotelId(itineraryItemRepository.findByItineraryId(itineraryId));
            if (hotelId == null) {
                throw ApiException.badRequest("Vui lòng chọn khách sạn");
            }
            RoomTypeEntity roomType = roomTypeRepository.findByIdAndHotelId(request.getRoomTypeId(), hotelId)
                    .orElseThrow(() -> ApiException.badRequest("Hạng phòng không thuộc khách sạn đã chọn"));
            int capacity = roomType.getMaxOccupancy() != null && roomType.getMaxOccupancy() > 0
                    ? roomType.getMaxOccupancy() : DEFAULT_ROOM_OCCUPANCY;
            int minRooms = (trip.partySize() + capacity - 1) / capacity;
            int quantity = request.getRoomQuantity() != null ? request.getRoomQuantity() : minRooms;
            if (quantity < minRooms) {
                throw ApiException.badRequest(trip.partySize() + " khách cần ít nhất " + minRooms + " phòng " + roomType.getName());
            }
            if (quantity > trip.partySize()) {
                throw ApiException.badRequest("Mỗi phòng cần ít nhất 1 người lớn — tối đa " + trip.partySize() + " phòng");
            }
            CartCheckoutDTO hotelOrder = hotelBookingService.createOrder(userId, List.of(new HotelBookingService.HotelBookingLine(
                    hotelId, roomType.getId(), trip.start(), trip.end(), quantity, trip.partySize(), 0,
                    request.getSpecialRequests())), request.getPaymentMethod());
            orderIds.add(hotelOrder.getOrderId());
        }

        if (request.getCarId() != null) {
            CarEntity car = carRepository.findById(request.getCarId())
                    .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                    .orElseThrow(() -> ApiException.badRequest("Xe không tồn tại hoặc đã ngừng cho thuê"));
            if (car.getDestination() == null || !car.getDestination().getId().equals(trip.itinerary().getDestination().getId())) {
                throw ApiException.badRequest("Xe không thuộc điểm đến của lịch trình");
            }
            if (car.getSeats() != null && car.getSeats() < trip.partySize()) {
                throw ApiException.badRequest("Xe chỉ có " + car.getSeats() + " chỗ, không đủ cho " + trip.partySize() + " khách");
            }
            LocalDateTime[] window = carWindow(trip);
            LocalDateTime pickup = request.getPickupDate() != null ? request.getPickupDate() : window == null ? null : window[0];
            LocalDateTime dropOff = request.getReturnDate() != null ? request.getReturnDate() : window == null ? null : window[1];
            if (pickup == null || dropOff == null) {
                throw ApiException.badRequest("Không còn thời gian thuê xe hợp lệ trong lịch trình");
            }
            if (pickup.isBefore(trip.start().atStartOfDay()) || dropOff.isAfter(trip.end().atTime(23, 59))) {
                throw ApiException.badRequest("Thời gian thuê xe phải nằm trong ngày đi / về của lịch trình");
            }
            CarBookingRequestDTO carRequest = new CarBookingRequestDTO();
            carRequest.setCarId(car.getId());
            carRequest.setPickupDate(pickup);
            carRequest.setReturnDate(dropOff);
            carRequest.setPickupLocation(request.getPickupLocation());
            carRequest.setReturnLocation(request.getReturnLocation());
            carRequest.setDriverLicenseNo(request.getDriverLicenseNo());
            carRequest.setPaymentMethod(request.getPaymentMethod());
            // Kiểm tra giờ nhận (≥ 1 giờ nữa), xe còn trống, GPLX (xe tự lái), khoá dòng xe — như đặt lẻ.
            CartCheckoutDTO carOrder = carBookingService.createOrder(userId, List.of(carRequest), request.getPaymentMethod());
            orderIds.add(carOrder.getOrderId());
        }

        for (UUID orderId : orderIds) {
            orderRepository.findById(orderId).ifPresent(order -> order.setNotes(note));
        }
        return get(userId, itineraryId);
    }

    // ================================================================== đọc / thanh toán / huỷ

    @Override
    @Transactional(readOnly = true)
    public TripBookingDTO get(UUID userId, UUID itineraryId) {
        ItineraryEntity itinerary = itineraryRepository.findOwned(itineraryId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch trình"));
        List<OrderEntity> orders = latestBatch(userId, itineraryId);
        TripBookingDTO dto = new TripBookingDTO();
        dto.setItineraryId(itineraryId);
        dto.setItineraryStatus(itinerary.getStatus());
        dto.setCurrencyCode(orderFactory.currencyCode());
        dto.setMockPaymentEnabled(mockPaymentEnabled);
        dto.setTotalAmount(BigDecimal.ZERO);
        if (orders.isEmpty()) {
            dto.setState("none");
            return dto;
        }
        dto.setBatchId(batchOfUuid(orders.get(0).getNotes()));
        // Khách sạn trước, xe sau.
        orders.sort(Comparator.comparing((OrderEntity o) -> "hotel".equals(orderBookingRouter.bookingTypeOf(o.getId())) ? 0 : 1));
        int pending = 0;
        int paid = 0;
        for (OrderEntity order : orders) {
            TripBookingOrderDTO orderDTO = toOrderDTO(order);
            dto.getOrders().add(orderDTO);
            dto.setTotalAmount(dto.getTotalAmount().add(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount()));
            if ("pending".equals(order.getStatus())) {
                pending++;
                if (dto.getHoldExpiresAt() == null || orderDTO.getHoldExpiresAt().isBefore(dto.getHoldExpiresAt())) {
                    dto.setHoldExpiresAt(orderDTO.getHoldExpiresAt());
                }
            } else if ("paid".equals(order.getStatus()) || "partially_refunded".equals(order.getStatus())) {
                paid++;
            }
        }
        if (pending > 0) {
            dto.setState("pending");
        } else if (paid == orders.size()) {
            dto.setState("paid");
        } else if (paid > 0) {
            dto.setState("partial");
        } else {
            dto.setState("cancelled");
        }
        return dto;
    }

    /**
     * Không chạy trong transaction: mỗi payment xử lý trong transaction riêng của PaymentService (giống cổng thật gửi
     * kết quả từng giao dịch) — order nào không giữ được chỗ sẽ tự hoàn tiền, order kia vẫn xác nhận.
     */
    @Override
    public TripBookingDTO pay(UUID userId, UUID itineraryId, boolean success) {
        itineraryRepository.findOwned(itineraryId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch trình"));
        List<UUID> paymentIds = new ArrayList<>();
        for (OrderEntity order : latestBatch(userId, itineraryId)) {
            if ("pending".equals(order.getStatus())) {
                PaymentEntity payment = orderFactory.findLatestPayment(order);
                if (payment != null && "pending".equals(payment.getStatus())) {
                    paymentIds.add(payment.getId());
                }
            }
        }
        if (paymentIds.isEmpty()) {
            throw ApiException.badRequest("Không có đơn nào đang chờ thanh toán (có thể đã hết thời gian giữ chỗ)");
        }
        for (UUID paymentId : paymentIds) {
            paymentService.mockPayment(userId, paymentId, success);
        }
        // Open-Session-In-View: entity đã đọc ở trên là bản cũ → xoá persistence context trước khi đọc lại.
        entityManager.clear();
        TripBookingDTO result = get(userId, itineraryId);
        if ("paid".equals(result.getState())) {
            itineraryRepository.findById(itineraryId).ifPresent(itinerary -> {
                itinerary.setStatus("booked");
                itinerary.setUpdatedAt(LocalDateTime.now());
                itineraryRepository.save(itinerary);
            });
            result.setItineraryStatus("booked");
        }
        return result;
    }

    @Override
    @Transactional
    public TripBookingDTO cancel(UUID userId, UUID itineraryId) {
        itineraryRepository.findOwned(itineraryId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch trình"));
        boolean any = false;
        for (OrderEntity order : latestBatch(userId, itineraryId)) {
            if (!"pending".equals(order.getStatus())) {
                continue;
            }
            any = true;
            String type = orderBookingRouter.bookingTypeOf(order.getId());
            // Huỷ 1 booking pending = huỷ cả order (nhả phòng / xe, payment cancelled) — dùng lại luồng huỷ của từng loại.
            if ("hotel".equals(type)) {
                List<HotelBookingEntity> bookings = hotelBookingRepository.findByOrderId(order.getId());
                if (!bookings.isEmpty()) {
                    hotelBookingService.cancelBooking(userId, bookings.get(0).getId(), "Khách huỷ đặt trọn gói từ lịch trình");
                }
            } else if ("car".equals(type)) {
                List<CarBookingEntity> bookings = carBookingRepository.findByOrderId(order.getId());
                if (!bookings.isEmpty()) {
                    carBookingService.cancel(userId, bookings.get(0).getId(), "Khách huỷ đặt trọn gói từ lịch trình");
                }
            }
        }
        if (!any) {
            throw ApiException.badRequest("Không có đơn nào chờ thanh toán để huỷ");
        }
        return get(userId, itineraryId);
    }

    // ================================================================== tiện ích

    private Trip loadTrip(UUID userId, UUID itineraryId) {
        ItineraryEntity itinerary = itineraryRepository.findOwned(itineraryId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch trình"));
        if (itinerary.getDestination() == null || itinerary.getStartDate() == null || itinerary.getEndDate() == null) {
            throw ApiException.badRequest("Lịch trình cần có điểm đến, ngày đi và ngày về để đặt dịch vụ");
        }
        if (itinerary.getStartDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Ngày đi của lịch trình đã qua, hãy sửa lại ngày trước khi đặt");
        }
        int nights = (int) ChronoUnit.DAYS.between(itinerary.getStartDate(), itinerary.getEndDate());
        int party = itinerary.getPartySize() == null || itinerary.getPartySize() < 1 ? 1 : itinerary.getPartySize();
        return new Trip(itinerary, itinerary.getStartDate(), itinerary.getEndDate(), nights, party);
    }

    /** Khách sạn đầu tiên gắn trong lịch trình (mục nhận phòng của AI Planner). */
    private UUID itineraryHotelId(List<ItineraryItemEntity> items) {
        return items.stream()
                .filter(i -> "hotel".equals(i.getEntityType()) && i.getEntityId() != null)
                .map(ItineraryItemEntity::getEntityId)
                .findFirst()
                .orElse(null);
    }

    /** Các order của lượt đặt gần nhất (cùng batch với order mới nhất). */
    private List<OrderEntity> latestBatch(UUID userId, UUID itineraryId) {
        List<OrderEntity> all = orderRepository.findByUserAndNotesPrefix(userId, NOTE_PREFIX + itineraryId);
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        String batch = batchOf(all.get(0));
        List<OrderEntity> result = new ArrayList<>();
        for (OrderEntity order : all) {
            if (batch != null && batch.equals(batchOf(order))) {
                result.add(order);
            }
        }
        return result;
    }

    private static UUID batchOfUuid(String notes) {
        int i = notes == null ? -1 : notes.indexOf("batch:");
        if (i < 0) {
            return null;
        }
        try {
            return UUID.fromString(notes.substring(i + 6).trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String batchOf(OrderEntity order) {
        UUID batch = batchOfUuid(order.getNotes());
        return batch == null ? null : batch.toString();
    }

    private TripBookingOrderDTO toOrderDTO(OrderEntity order) {
        TripBookingOrderDTO dto = new TripBookingOrderDTO();
        String type = orderBookingRouter.bookingTypeOf(order.getId());
        dto.setBookingType(type);
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderStatus(order.getStatus());
        dto.setAmount(order.getTotalAmount());
        dto.setHoldExpiresAt(orderFactory.holdExpiresAt(order.getCreatedAt()));
        PaymentEntity payment = orderFactory.findLatestPayment(order);
        if (payment != null) {
            dto.setPaymentId(payment.getId());
            dto.setPaymentStatus(payment.getStatus());
            dto.setPaymentMethod(payment.getPaymentMethod());
            dto.setTransactionId(payment.getTransactionId());
            dto.setPaidAt(payment.getPaidAt());
        }
        if ("hotel".equals(type)) {
            // Gộp theo hạng phòng: N phòng cùng hạng = 1 dòng số lượng N.
            Map<UUID, TripBookingLineDTO> byRoomType = new LinkedHashMap<>();
            for (HotelBookingEntity booking : hotelBookingRepository.findByOrderId(order.getId())) {
                HotelEntity hotel = booking.getHotel();
                TripBookingLineDTO line = byRoomType.computeIfAbsent(booking.getRoomType().getId(), id -> {
                    TripBookingLineDTO l = new TripBookingLineDTO();
                    l.setTitle(hotel.getName());
                    l.setSubtitle(booking.getRoomType().getName());
                    l.setStartAt(booking.getCheckInDate().atTime(parseTime(hotel.getCheckInTime(), DEFAULT_CHECK_IN)));
                    l.setEndAt(booking.getCheckOutDate().atTime(parseTime(hotel.getCheckOutTime(), LocalTime.NOON)));
                    l.setUnits(booking.getNumNights() != null ? booking.getNumNights()
                            : (int) ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()));
                    l.setUnitPrice(booking.getRoomType().getPricePerNight() == null ? BigDecimal.ZERO
                            : BigDecimal.valueOf(booking.getRoomType().getPricePerNight()));
                    l.setAmount(BigDecimal.ZERO);
                    l.setStatus(booking.getStatus());
                    return l;
                });
                line.setQuantity(line.getQuantity() + 1);
                line.setAmount(line.getAmount().add(booking.getTotalPrice()));
            }
            dto.getLines().addAll(byRoomType.values());
        } else if ("car".equals(type)) {
            for (CarBookingEntity booking : carBookingRepository.findByOrderId(order.getId())) {
                TripBookingLineDTO line = new TripBookingLineDTO();
                line.setTitle(booking.getCar().getName());
                line.setSubtitle(booking.getPickupLocation());
                line.setStartAt(booking.getPickupDate());
                line.setEndAt(booking.getReturnDate());
                line.setQuantity(1);
                line.setUnits(CarBookingServiceImpl.rentalDays(booking.getPickupDate(), booking.getReturnDate()));
                line.setUnitPrice(booking.getCar().getPricePerDay());
                line.setAmount(booking.getTotalPrice());
                line.setStatus(booking.getStatus());
                dto.getLines().add(line);
            }
        }
        return dto;
    }

    private static LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value.length() > 5 ? value.substring(0, 5) : value);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
