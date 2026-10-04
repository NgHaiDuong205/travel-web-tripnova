package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.ItineraryDTO;
import com.duong.travelweb.model.dto.ItineraryItemDTO;
import com.duong.travelweb.model.dto.ItineraryItemRequestDTO;
import com.duong.travelweb.model.dto.ItineraryRequestDTO;
import com.duong.travelweb.model.dto.PlannerSaveRequestDTO;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.ItineraryEntity;
import com.duong.travelweb.model.entity.ItineraryItemEntity;
import com.duong.travelweb.model.entity.LandmarkEntity;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.ItineraryItemRepository;
import com.duong.travelweb.repository.ItineraryRepository;
import com.duong.travelweb.repository.LandmarkRepository;
import com.duong.travelweb.service.EntityReferenceService;
import com.duong.travelweb.service.ItineraryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ItineraryServiceImpl implements ItineraryService {
    /** Độ dài tối đa của một chuyến (cũng là giới hạn dayNumber khi chưa chọn ngày). */
    private static final int MAX_DAYS = 60;
    /** entity_type có tham chiếu thật tới bảng khác -> kiểm tra entityId tồn tại. */
    private static final List<String> LINKED_TYPES = EntityReferenceService.LINKED_TYPES;

    private final ItineraryRepository itineraryRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final DestinationRepository destinationRepository;
    private final EntityReferenceService entityReferenceService;
    private final HotelRepository hotelRepository;
    private final LandmarkRepository landmarkRepository;

    public ItineraryServiceImpl(ItineraryRepository itineraryRepository,
                                ItineraryItemRepository itineraryItemRepository,
                                DestinationRepository destinationRepository,
                                EntityReferenceService entityReferenceService,
                                HotelRepository hotelRepository,
                                LandmarkRepository landmarkRepository) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.destinationRepository = destinationRepository;
        this.entityReferenceService = entityReferenceService;
        this.hotelRepository = hotelRepository;
        this.landmarkRepository = landmarkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItineraryDTO> findMine(UUID userId, int page, int limit) {
        Page<ItineraryEntity> itineraries = itineraryRepository.findByUserId(userId,
                PageRequest.of(Math.max(page, 1) - 1, limit));
        Map<UUID, Object[]> summaries = new HashMap<>();
        List<UUID> ids = itineraries.stream().map(ItineraryEntity::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : itineraryRepository.summarizeItems(ids)) {
                summaries.put((UUID) row[0], row);
            }
        }
        return itineraries.map(itinerary -> {
            ItineraryDTO dto = toDTO(itinerary);
            Object[] summary = summaries.get(itinerary.getId());
            dto.setItemCount(summary == null ? 0 : ((Number) summary[1]).intValue());
            dto.setEstimatedTotal(summary == null ? BigDecimal.ZERO : toBigDecimal(summary[2]));
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryDTO getMine(UUID userId, UUID itineraryId) {
        return toDetailDTO(findOwned(userId, itineraryId));
    }

    @Override
    @Transactional
    public ItineraryDTO create(UUID userId, ItineraryRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        ItineraryEntity itinerary = new ItineraryEntity();
        itinerary.setUserId(userId);
        itinerary.setGeneratedBy("manual");
        itinerary.setCreatedAt(now);
        applyRequest(itinerary, request, now);
        return toDetailDTO(itineraryRepository.save(itinerary));
    }

    @Override
    @Transactional
    public ItineraryDTO createFromPlanner(UUID userId, PlannerSaveRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        ItineraryRequestDTO base = new ItineraryRequestDTO();
        base.setTitle(request.getTitle());
        base.setDestinationId(request.getDestinationId());
        base.setStartDate(request.getStartDate());
        base.setEndDate(request.getEndDate());
        base.setPartySize(request.getPartySize());
        base.setTotalBudget(request.getTotalBudget());
        base.setStatus("draft");

        ItineraryEntity itinerary = new ItineraryEntity();
        itinerary.setUserId(userId);
        itinerary.setGeneratedBy("ai");
        itinerary.setPrompt(blankToNull(request.getPrompt()));
        itinerary.setModelVersion(blankToNull(request.getModelVersion()));
        itinerary.setCreatedAt(now);
        applyRequest(itinerary, base, now);
        itinerary = itineraryRepository.save(itinerary);

        // Thứ tự gửi lên = thứ tự trong ngày (sortOrder 0, 1, 2… theo từng ngày) nếu client không truyền sortOrder.
        Map<Integer, Integer> nextOrder = new HashMap<>();
        for (ItineraryItemRequestDTO itemRequest : request.getItems()) {
            if (itemRequest.getSortOrder() == null && itemRequest.getDayNumber() != null) {
                itemRequest.setSortOrder(nextOrder.merge(itemRequest.getDayNumber(), 1, Integer::sum) - 1);
            }
            ItineraryItemEntity item = new ItineraryItemEntity();
            item.setItinerary(itinerary);
            item.setCreatedAt(now);
            applyItem(item, itinerary, itemRequest);
            itineraryItemRepository.save(item);
        }
        return toDetailDTO(itinerary);
    }

    @Override
    @Transactional
    public ItineraryDTO update(UUID userId, UUID itineraryId, ItineraryRequestDTO request) {
        ItineraryEntity itinerary = findOwned(userId, itineraryId);
        Integer newDays = daysBetween(request);
        if (newDays != null) {
            int lastUsedDay = itineraryItemRepository.findMaxDayNumber(itineraryId);
            if (lastUsedDay > newDays) {
                throw ApiException.badRequest("Chuyến đi còn hoạt động ở ngày " + lastUsedDay
                        + ", hãy xoá hoạt động đó trước khi rút ngắn còn " + newDays + " ngày");
            }
        }
        applyRequest(itinerary, request, LocalDateTime.now());
        return toDetailDTO(itinerary);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID itineraryId) {
        // itinerary_items: ON DELETE CASCADE
        itineraryRepository.delete(findOwned(userId, itineraryId));
    }

    @Override
    @Transactional
    public ItineraryDTO addItem(UUID userId, UUID itineraryId, ItineraryItemRequestDTO request) {
        ItineraryEntity itinerary = findOwned(userId, itineraryId);
        LocalDateTime now = LocalDateTime.now();
        ItineraryItemEntity item = new ItineraryItemEntity();
        item.setItinerary(itinerary);
        item.setCreatedAt(now);
        applyItem(item, itinerary, request);
        itineraryItemRepository.save(item);
        itinerary.setUpdatedAt(now);
        return toDetailDTO(itinerary);
    }

    @Override
    @Transactional
    public ItineraryDTO updateItem(UUID userId, UUID itineraryId, UUID itemId, ItineraryItemRequestDTO request) {
        ItineraryEntity itinerary = findOwned(userId, itineraryId);
        ItineraryItemEntity item = itineraryItemRepository.findInItinerary(itemId, itineraryId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hoạt động trong lịch trình"));
        applyItem(item, itinerary, request);
        itinerary.setUpdatedAt(LocalDateTime.now());
        return toDetailDTO(itinerary);
    }

    /**
     * Kiểm tra + gán dữ liệu hoạt động (dùng cho thêm và sửa — sửa là ghi đè toàn bộ).
     * Không truyền sortOrder: hoạt động mới / chuyển sang ngày khác thì xếp cuối ngày, giữ nguyên nếu vẫn ở ngày cũ.
     */
    private void applyItem(ItineraryItemEntity item, ItineraryEntity itinerary, ItineraryItemRequestDTO request) {
        Integer days = days(itinerary);
        int maxDay = days != null ? days : MAX_DAYS;
        if (request.getDayNumber() > maxDay) {
            throw ApiException.badRequest("Ngày thứ " + request.getDayNumber() + " nằm ngoài lịch trình (" + maxDay + " ngày)");
        }
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw ApiException.badRequest("Giờ kết thúc phải sau giờ bắt đầu");
        }

        String entityType = blankToNull(request.getEntityType());
        String entityName = resolveEntityName(entityType, request.getEntityId());
        String title = blankToNull(request.getTitle());
        if (title == null) {
            if (entityName == null) {
                throw ApiException.badRequest("Vui lòng nhập tiêu đề hoạt động");
            }
            title = entityName;
        }

        short dayNumber = request.getDayNumber().shortValue();
        boolean sameDay = item.getDayNumber() != null && item.getDayNumber() == dayNumber;
        int sortOrder;
        if (request.getSortOrder() != null) {
            sortOrder = request.getSortOrder();
        } else if (sameDay && item.getSortOrder() != null) {
            sortOrder = item.getSortOrder();
        } else {
            sortOrder = itineraryItemRepository.findMaxSortOrder(itinerary.getId(), dayNumber) + 1;
        }
        item.setDayNumber(dayNumber);
        item.setStartTime(request.getStartTime());
        item.setEndTime(request.getEndTime());
        item.setEntityType(entityType);
        item.setEntityId(request.getEntityId());
        item.setTitle(title);
        item.setNotes(blankToNull(request.getNotes()));
        item.setEstimatedCost(request.getEstimatedCost());
        item.setSortOrder((short) Math.min(sortOrder, Short.MAX_VALUE));
    }

    @Override
    @Transactional
    public void deleteItem(UUID userId, UUID itineraryId, UUID itemId) {
        ItineraryEntity itinerary = findOwned(userId, itineraryId);
        ItineraryItemEntity item = itineraryItemRepository.findInItinerary(itemId, itineraryId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hoạt động trong lịch trình"));
        itineraryItemRepository.delete(item);
        itinerary.setUpdatedAt(LocalDateTime.now());
    }

    private void applyRequest(ItineraryEntity itinerary, ItineraryRequestDTO request, LocalDateTime now) {
        daysBetween(request); // kiểm tra khoảng ngày
        itinerary.setTitle(request.getTitle().trim());
        itinerary.setDestination(request.getDestinationId() == null ? null : findActiveDestination(request.getDestinationId()));
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setTotalBudget(request.getTotalBudget());
        itinerary.setPartySize(request.getPartySize() == null ? null : request.getPartySize().shortValue());
        String status = blankToNull(request.getStatus());
        itinerary.setStatus(status != null ? status : itinerary.getStatus() != null ? itinerary.getStatus() : "draft");
        itinerary.setUpdatedAt(now);
    }

    /** Số ngày của chuyến theo request (null nếu chưa đủ ngày đi / về); kiểm tra hợp lệ. */
    private Integer daysBetween(ItineraryRequestDTO request) {
        if (request.getEndDate() != null && request.getStartDate() == null) {
            throw ApiException.badRequest("Vui lòng chọn ngày bắt đầu trước ngày kết thúc");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            return null;
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw ApiException.badRequest("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (days > MAX_DAYS) {
            throw ApiException.badRequest("Lịch trình tối đa " + MAX_DAYS + " ngày");
        }
        return (int) days;
    }

    private Integer days(ItineraryEntity itinerary) {
        if (itinerary.getStartDate() == null || itinerary.getEndDate() == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(itinerary.getStartDate(), itinerary.getEndDate()) + 1;
    }

    /** Kiểm tra tham chiếu và trả về tên để hiển thị; loại không liên kết thì không được kèm entityId. */
    private String resolveEntityName(String entityType, UUID entityId) {
        if (entityId == null) {
            return null;
        }
        String referenceType = referenceType(entityType);
        if (referenceType == null) {
            throw ApiException.badRequest("Chỉ hoạt động loại hotel, landmark, restaurant, destination mới được gắn entityId");
        }
        return entityReferenceService.requireActiveName(referenceType, entityId);
    }

    /** Bảng mà entityId trỏ tới: quán ăn (restaurant) là địa danh loại restaurant; loại không liên kết → null. */
    private static String referenceType(String entityType) {
        if ("restaurant".equals(entityType)) {
            return "landmark";
        }
        return entityType != null && LINKED_TYPES.contains(entityType) ? entityType : null;
    }

    private DestinationEntity findActiveDestination(UUID destinationId) {
        return destinationRepository.findById(destinationId)
                .filter(d -> !Boolean.FALSE.equals(d.getIsActive()))
                .orElseThrow(() -> ApiException.badRequest("Điểm đến không tồn tại"));
    }

    private ItineraryEntity findOwned(UUID userId, UUID itineraryId) {
        return itineraryRepository.findOwned(itineraryId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch trình"));
    }

    private ItineraryDTO toDTO(ItineraryEntity itinerary) {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(itinerary.getId());
        dto.setTitle(itinerary.getTitle());
        DestinationEntity destination = itinerary.getDestination();
        if (destination != null) {
            dto.setDestinationId(destination.getId());
            dto.setDestinationName(destination.getName());
            if (destination.getCountry() != null) {
                dto.setCountryName(destination.getCountry().getName());
            }
        }
        dto.setStartDate(itinerary.getStartDate());
        dto.setEndDate(itinerary.getEndDate());
        dto.setDays(days(itinerary));
        dto.setPartySize(itinerary.getPartySize() == null ? null : itinerary.getPartySize().intValue());
        dto.setTotalBudget(itinerary.getTotalBudget());
        dto.setStatus(itinerary.getStatus());
        dto.setGeneratedBy(itinerary.getGeneratedBy());
        dto.setCreatedAt(itinerary.getCreatedAt());
        dto.setUpdatedAt(itinerary.getUpdatedAt());
        return dto;
    }

    private ItineraryDTO toDetailDTO(ItineraryEntity itinerary) {
        ItineraryDTO dto = toDTO(itinerary);
        List<ItineraryItemEntity> items = itinerary.getId() == null
                ? List.of()
                : itineraryItemRepository.findByItineraryId(itinerary.getId());
        Map<String, Map<UUID, String>> names = resolveNames(items);
        Map<UUID, double[]> points = resolvePoints(items);
        List<ItineraryItemDTO> itemDTOs = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ItineraryItemEntity item : items) {
            ItineraryItemDTO itemDTO = new ItineraryItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setDayNumber(item.getDayNumber());
            if (itinerary.getStartDate() != null) {
                itemDTO.setDate(itinerary.getStartDate().plusDays(item.getDayNumber() - 1L));
            }
            itemDTO.setStartTime(item.getStartTime());
            itemDTO.setEndTime(item.getEndTime());
            itemDTO.setEntityType(item.getEntityType());
            itemDTO.setEntityId(item.getEntityId());
            String referenceType = referenceType(item.getEntityType());
            if (item.getEntityId() != null && referenceType != null && names.containsKey(referenceType)) {
                itemDTO.setEntityName(names.get(referenceType).get(item.getEntityId()));
            }
            itemDTO.setTitle(item.getTitle());
            itemDTO.setNotes(item.getNotes());
            itemDTO.setEstimatedCost(item.getEstimatedCost());
            itemDTO.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            double[] point = item.getEntityId() == null ? null : points.get(item.getEntityId());
            if (point != null) {
                itemDTO.setLatitude(point[0]);
                itemDTO.setLongitude(point[1]);
            }
            itemDTOs.add(itemDTO);
            if (item.getEstimatedCost() != null) {
                total = total.add(item.getEstimatedCost());
            }
        }
        dto.setItems(itemDTOs);
        dto.setItemCount(itemDTOs.size());
        dto.setEstimatedTotal(total);
        return dto;
    }

    /** Tên hiện tại của các entity được tham chiếu, gom theo loại (tối đa 3 query). */
    private Map<String, Map<UUID, String>> resolveNames(List<ItineraryItemEntity> items) {
        Map<String, List<UUID>> idsByType = new HashMap<>();
        for (ItineraryItemEntity item : items) {
            String referenceType = referenceType(item.getEntityType());
            if (item.getEntityId() != null && referenceType != null) {
                idsByType.computeIfAbsent(referenceType, k -> new ArrayList<>()).add(item.getEntityId());
            }
        }
        return entityReferenceService.resolveNames(idsByType);
    }

    /** Toạ độ khách sạn / địa danh được gắn (2 query theo lô), để client vẽ bản đồ. */
    private Map<UUID, double[]> resolvePoints(List<ItineraryItemEntity> items) {
        List<UUID> hotelIds = new ArrayList<>();
        List<UUID> landmarkIds = new ArrayList<>();
        for (ItineraryItemEntity item : items) {
            if (item.getEntityId() == null) {
                continue;
            }
            String referenceType = referenceType(item.getEntityType());
            if ("hotel".equals(referenceType)) {
                hotelIds.add(item.getEntityId());
            } else if ("landmark".equals(referenceType)) {
                landmarkIds.add(item.getEntityId());
            }
        }
        Map<UUID, double[]> points = new HashMap<>();
        if (!hotelIds.isEmpty()) {
            for (HotelEntity hotel : hotelRepository.findAllById(hotelIds)) {
                if (hotel.getLatitude() != null && hotel.getLongitude() != null) {
                    points.put(hotel.getId(), new double[]{hotel.getLatitude().doubleValue(), hotel.getLongitude().doubleValue()});
                }
            }
        }
        if (!landmarkIds.isEmpty()) {
            for (LandmarkEntity landmark : landmarkRepository.findAllById(landmarkIds)) {
                if (landmark.getLatitude() != null && landmark.getLongitude() != null) {
                    points.put(landmark.getId(), new double[]{landmark.getLatitude(), landmark.getLongitude()});
                }
            }
        }
        return points;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
