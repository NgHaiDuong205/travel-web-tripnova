package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.CartCheckoutRequestDTO;
import com.duong.travelweb.model.dto.CartDTO;
import com.duong.travelweb.model.dto.CartItemDTO;
import com.duong.travelweb.model.dto.CartItemRequestDTO;
import com.duong.travelweb.model.entity.CartEntity;
import com.duong.travelweb.model.entity.CartItemEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import com.duong.travelweb.repository.CartItemRepository;
import com.duong.travelweb.repository.CartRepository;
import com.duong.travelweb.repository.RoomTypeRepository;
import com.duong.travelweb.service.CartService;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.service.HotelBookingService.HotelBookingLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CartServiceImpl implements CartService {
    private static final int MAX_ITEMS = 20;
    private static final int MAX_ROOMS_PER_ITEM = 5;
    private static final int MAX_NIGHTS = 30; // khớp HotelBookingServiceImpl.MAX_NIGHTS

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HotelBookingService hotelBookingService;
    private final ObjectMapper objectMapper;
    private final String currencyCode;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           RoomTypeRepository roomTypeRepository,
                           HotelBookingService hotelBookingService,
                           ObjectMapper objectMapper,
                           @Value("${app.booking.currency:USD}") String currencyCode) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.hotelBookingService = hotelBookingService;
        this.objectMapper = objectMapper;
        this.currencyCode = currencyCode;
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCart(UUID userId) {
        return cartRepository.findByUserId(userId).map(this::toDTO).orElseGet(this::emptyCart);
    }

    @Override
    @Transactional
    public CartDTO addItem(UUID userId, CartItemRequestDTO request) {
        String itemType = request.getItemType() == null || request.getItemType().isBlank() ? "hotel" : request.getItemType();
        if (!"hotel".equals(itemType)) {
            throw ApiException.badRequest("Hiện giỏ hàng chỉ hỗ trợ đặt phòng khách sạn");
        }
        if (request.getHotelId() == null || request.getRoomTypeId() == null) {
            throw ApiException.badRequest("Thiếu khách sạn hoặc loại phòng");
        }
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndHotelId(request.getRoomTypeId(), request.getHotelId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy loại phòng của khách sạn"));
        CartEntity cart = cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));
        List<CartItemEntity> items = cart.getId() == null ? List.of() : cartItemRepository.findByCartId(cart.getId());

        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        int adults = request.getAdults();
        int children = request.getChildren() != null ? request.getChildren() : 0;
        String specialRequests = request.getSpecialRequests();

        CartItemEntity item = items.stream()
                .filter(i -> i.getRoomType() != null && i.getRoomType().getId().equals(roomType.getId())
                        && request.getCheckIn().equals(i.getCheckInDate()) && request.getCheckOut().equals(i.getCheckOutDate()))
                .findFirst()
                .orElse(null);
        if (item != null) {
            JsonNode snapshot = readSnapshot(item);
            quantity += item.getQuantity();
            adults += snapshot.path("adults").asInt(0);
            children += snapshot.path("children").asInt(0);
            if (specialRequests == null || specialRequests.isBlank()) {
                specialRequests = textOrNull(snapshot, "specialRequests");
            }
            if (quantity > MAX_ROOMS_PER_ITEM) {
                throw ApiException.badRequest("Tối đa " + MAX_ROOMS_PER_ITEM + " phòng mỗi loại phòng trong giỏ");
            }
        } else {
            if (items.size() >= MAX_ITEMS) {
                throw ApiException.badRequest("Giỏ hàng tối đa " + MAX_ITEMS + " dòng");
            }
            item = new CartItemEntity();
            item.setCart(cart);
            item.setItemType("hotel");
            item.setItemId(roomType.getHotel().getId());
            item.setRoomType(roomType);
            item.setAddedAt(LocalDateTime.now());
        }
        applyHotelItem(item, roomType, request.getCheckIn(), request.getCheckOut(), quantity, adults, children, specialRequests);
        cartItemRepository.save(item);
        touch(cart);
        return toDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO updateItem(UUID userId, UUID itemId, CartItemRequestDTO request) {
        CartEntity cart = findCart(userId);
        CartItemEntity item = findItem(cart, itemId);
        RoomTypeEntity roomType = item.getRoomType();
        if (roomType == null) {
            throw ApiException.badRequest("Loại phòng của dòng này không còn tồn tại, hãy xoá khỏi giỏ");
        }
        int quantity = request.getQuantity() != null ? request.getQuantity() : item.getQuantity();
        int children = request.getChildren() != null ? request.getChildren() : 0;
        applyHotelItem(item, roomType, request.getCheckIn(), request.getCheckOut(), quantity, request.getAdults(),
                children, request.getSpecialRequests());
        touch(cart);
        return toDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO removeItem(UUID userId, UUID itemId) {
        CartEntity cart = findCart(userId);
        cartItemRepository.delete(findItem(cart, itemId));
        cartItemRepository.flush();
        touch(cart);
        return toDTO(cart);
    }

    @Override
    @Transactional
    public void clear(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            touch(cart);
        });
    }

    @Override
    @Transactional
    public CartCheckoutDTO checkout(UUID userId, CartCheckoutRequestDTO request) {
        CartEntity cart = findCart(userId);
        List<CartItemEntity> all = cartItemRepository.findByCartId(cart.getId());
        List<CartItemEntity> selected;
        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            selected = all;
        } else {
            Set<UUID> wanted = new HashSet<>(request.getItemIds());
            selected = all.stream().filter(i -> wanted.contains(i.getId())).toList();
            if (selected.size() != wanted.size()) {
                throw ApiException.badRequest("Có dòng không thuộc giỏ hàng của bạn");
            }
        }
        if (selected.isEmpty()) {
            throw ApiException.badRequest("Giỏ hàng trống");
        }

        List<HotelBookingLine> lines = new ArrayList<>();
        for (CartItemEntity item : selected) {
            if (item.getRoomType() == null) {
                throw ApiException.badRequest("Có loại phòng trong giỏ không còn tồn tại, hãy xoá dòng đó");
            }
            if (item.getCheckInDate().isBefore(LocalDate.now())) {
                throw ApiException.badRequest("Ngày nhận phòng của " + item.getRoomType().getName() + " đã qua, hãy cập nhật ngày");
            }
            JsonNode snapshot = readSnapshot(item);
            lines.add(new HotelBookingLine(item.getRoomType().getHotel().getId(), item.getRoomType().getId(),
                    item.getCheckInDate(), item.getCheckOutDate(), item.getQuantity(),
                    snapshot.path("adults").asInt(1), snapshot.path("children").asInt(0),
                    textOrNull(snapshot, "specialRequests")));
        }
        // Kiểm tra phòng trống + giữ phòng nguyên tử; lỗi -> rollback, giỏ giữ nguyên
        CartCheckoutDTO result = hotelBookingService.createOrder(userId, lines, request.getPaymentMethod());
        cartItemRepository.deleteAll(selected);
        touch(cart);
        return result;
    }

    /** Kiểm tra + ghi các trường của dòng hotel (giá chốt tại thời điểm thêm/sửa để so sánh sau). */
    private void applyHotelItem(CartItemEntity item, RoomTypeEntity roomType, LocalDate checkIn, LocalDate checkOut,
                                int quantity, int adults, int children, String specialRequests) {
        HotelEntity hotel = roomType.getHotel();
        if (!Boolean.TRUE.equals(roomType.getIsActive()) || !Boolean.TRUE.equals(hotel.getIsActive())) {
            throw ApiException.badRequest("Loại phòng này hiện không nhận đặt");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Ngày nhận phòng không được ở quá khứ");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.badRequest("Ngày trả phòng phải sau ngày nhận phòng");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw ApiException.badRequest("Chỉ được đặt tối đa " + MAX_NIGHTS + " đêm");
        }
        if (adults < quantity) {
            throw ApiException.badRequest("Mỗi phòng cần ít nhất 1 người lớn");
        }
        Integer maxOccupancy = roomType.getMaxOccupancy();
        if (maxOccupancy != null && ceilDiv(adults, quantity) + ceilDiv(children, quantity) > maxOccupancy) {
            throw ApiException.badRequest("Mỗi phòng tối đa " + maxOccupancy + " khách, hãy tăng số phòng");
        }
        int available = hotelBookingService.countBookableRooms(roomType.getId(), checkIn, checkOut);
        if (available < quantity) {
            throw ApiException.conflict("Chỉ còn " + available + " phòng " + roomType.getName() + " trống trong thời gian này");
        }

        BigDecimal pricePerNight = priceOf(roomType);
        BigDecimal unitPrice = pricePerNight.multiply(BigDecimal.valueOf(nights));
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("adults", adults);
        snapshot.put("children", children);
        snapshot.put("specialRequests", specialRequests == null || specialRequests.isBlank() ? null : specialRequests.trim());
        snapshot.put("hotelName", hotel.getName());
        snapshot.put("roomTypeName", roomType.getName());
        snapshot.put("pricePerNight", pricePerNight);

        item.setQuantity((short) quantity);
        item.setCheckInDate(checkIn);
        item.setCheckOutDate(checkOut);
        item.setItemSnapshot(objectMapper.writeValueAsString(snapshot));
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    /** Tính lại giá và tình trạng từng dòng tại thời điểm đọc. */
    private CartDTO toDTO(CartEntity cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setCurrencyCode(currencyCode);
        BigDecimal total = BigDecimal.ZERO;
        boolean hasIssues = false;
        List<CartItemDTO> itemDTOs = new ArrayList<>();
        for (CartItemEntity item : cartItemRepository.findByCartId(cart.getId())) {
            CartItemDTO itemDTO = toItemDTO(item);
            itemDTOs.add(itemDTO);
            if (itemDTO.getIssue() != null) {
                hasIssues = true;
            } else {
                total = total.add(itemDTO.getTotalPrice());
            }
        }
        dto.setItems(itemDTOs);
        dto.setItemCount(itemDTOs.size());
        dto.setTotalAmount(total);
        dto.setHasIssues(hasIssues);
        return dto;
    }

    private CartItemDTO toItemDTO(CartItemEntity item) {
        JsonNode snapshot = readSnapshot(item);
        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        dto.setItemType(item.getItemType());
        dto.setHotelId(item.getItemId());
        dto.setCheckIn(item.getCheckInDate());
        dto.setCheckOut(item.getCheckOutDate());
        int nights = (int) ChronoUnit.DAYS.between(item.getCheckInDate(), item.getCheckOutDate());
        dto.setNights(nights);
        dto.setQuantity(item.getQuantity());
        dto.setAdults(snapshot.path("adults").asInt(1));
        dto.setChildren(snapshot.path("children").asInt(0));
        dto.setSpecialRequests(textOrNull(snapshot, "specialRequests"));
        dto.setSnapshotUnitPrice(item.getUnitPrice());
        dto.setAddedAt(item.getAddedAt());

        RoomTypeEntity roomType = item.getRoomType();
        if (roomType == null) {
            dto.setHotelName(textOrNull(snapshot, "hotelName"));
            dto.setRoomTypeName(textOrNull(snapshot, "roomTypeName"));
            dto.setUnitPrice(item.getUnitPrice());
            dto.setTotalPrice(item.getTotalPrice());
            dto.setIssue("inactive");
            return dto;
        }
        HotelEntity hotel = roomType.getHotel();
        dto.setHotelName(hotel.getName());
        dto.setHotelCoverImageUrl(hotel.getCoverImageUrl());
        dto.setRoomTypeId(roomType.getId());
        dto.setRoomTypeName(roomType.getName());
        BigDecimal unitPrice = priceOf(roomType).multiply(BigDecimal.valueOf(nights));
        dto.setUnitPrice(unitPrice);
        dto.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        dto.setPriceChanged(item.getUnitPrice() != null && unitPrice.compareTo(item.getUnitPrice()) != 0);

        if (item.getCheckInDate().isBefore(LocalDate.now())) {
            dto.setIssue("expired");
        } else if (!Boolean.TRUE.equals(roomType.getIsActive()) || !Boolean.TRUE.equals(hotel.getIsActive())) {
            dto.setIssue("inactive");
        } else {
            int available = hotelBookingService.countBookableRooms(roomType.getId(), item.getCheckInDate(), item.getCheckOutDate());
            dto.setAvailableRooms(available);
            if (available < item.getQuantity()) {
                dto.setIssue("unavailable");
            }
        }
        return dto;
    }

    private JsonNode readSnapshot(CartItemEntity item) {
        if (item.getItemSnapshot() == null || item.getItemSnapshot().isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(item.getItemSnapshot());
    }

    /** Jackson 3: asString() của NullNode trả "" -> tự kiểm tra null. */
    private String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asString() : null;
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

    private CartItemEntity findItem(CartEntity cart, UUID itemId) {
        return cartItemRepository.findInCart(itemId, cart.getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy dòng trong giỏ hàng"));
    }

    private void touch(CartEntity cart) {
        cart.setUpdatedAt(LocalDateTime.now());
    }

    private CartDTO emptyCart() {
        CartDTO dto = new CartDTO();
        dto.setCurrencyCode(currencyCode);
        dto.setTotalAmount(BigDecimal.ZERO);
        return dto;
    }

    private BigDecimal priceOf(RoomTypeEntity roomType) {
        return roomType.getPricePerNight() != null ? BigDecimal.valueOf(roomType.getPricePerNight()) : BigDecimal.ZERO;
    }

    private int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
