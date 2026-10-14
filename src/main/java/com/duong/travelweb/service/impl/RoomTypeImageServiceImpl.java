package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.RoomTypeImageDTO;
import com.duong.travelweb.model.dto.RoomTypeImageRequestDTO;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import com.duong.travelweb.model.entity.RoomTypeImageEntity;
import com.duong.travelweb.repository.RoomTypeImageRepository;
import com.duong.travelweb.repository.RoomTypeRepository;
import com.duong.travelweb.service.RoomTypeImageService;
import com.duong.travelweb.util.ImageUrlUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RoomTypeImageServiceImpl implements RoomTypeImageService {
    private static final int MAX_IMAGES_PER_ROOM_TYPE = 20;

    private final RoomTypeImageRepository roomTypeImageRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeImageServiceImpl(RoomTypeImageRepository roomTypeImageRepository,
                                    RoomTypeRepository roomTypeRepository) {
        this.roomTypeImageRepository = roomTypeImageRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeImageDTO> findByRoomType(UUID hotelId, UUID roomTypeId, boolean activeOnly) {
        RoomTypeEntity roomType = findRoomType(hotelId, roomTypeId);
        if (activeOnly && (!Boolean.TRUE.equals(roomType.getIsActive())
                || !Boolean.TRUE.equals(roomType.getHotel().getIsActive()))) {
            throw ApiException.notFound("Không tìm thấy hạng phòng");
        }
        return roomTypeImageRepository.findByRoomTypeId(roomTypeId).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public RoomTypeImageDTO add(UUID userId, UUID hotelId, UUID roomTypeId, RoomTypeImageRequestDTO request) {
        // Khoá dòng room_types để 2 request thêm ảnh đồng thời không vượt giới hạn / trùng sortOrder.
        roomTypeRepository.lockByIdAndHotelId(roomTypeId, hotelId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hạng phòng"));
        if (roomTypeImageRepository.countByRoomTypeId(roomTypeId) >= MAX_IMAGES_PER_ROOM_TYPE) {
            throw ApiException.badRequest("Mỗi hạng phòng tối đa " + MAX_IMAGES_PER_ROOM_TYPE + " ảnh");
        }
        RoomTypeImageEntity image = new RoomTypeImageEntity();
        image.setRoomTypeId(roomTypeId);
        image.setUrl(ImageUrlUtil.requireValid(request.getUrl()));
        image.setCaption(blankToNull(request.getCaption()));
        image.setSortOrder(request.getSortOrder() != null
                ? Math.max(request.getSortOrder(), 0)
                : roomTypeImageRepository.maxSortOrder(roomTypeId) + 1);
        image.setCreatedBy(userId);
        image.setCreatedAt(LocalDateTime.now());
        return toDTO(roomTypeImageRepository.save(image));
    }

    @Override
    @Transactional
    public RoomTypeImageDTO update(UUID hotelId, UUID roomTypeId, UUID imageId, RoomTypeImageRequestDTO request) {
        RoomTypeImageEntity image = findImage(hotelId, roomTypeId, imageId);
        image.setUrl(ImageUrlUtil.requireValid(request.getUrl()));
        image.setCaption(blankToNull(request.getCaption()));
        if (request.getSortOrder() != null) {
            image.setSortOrder(Math.max(request.getSortOrder(), 0));
        }
        return toDTO(image);
    }

    @Override
    @Transactional
    public void delete(UUID hotelId, UUID roomTypeId, UUID imageId) {
        roomTypeImageRepository.delete(findImage(hotelId, roomTypeId, imageId));
    }

    @Override
    @Transactional
    public RoomTypeImageDTO setCover(UUID hotelId, UUID roomTypeId, UUID imageId) {
        RoomTypeImageEntity image = findImage(hotelId, roomTypeId, imageId);
        findRoomType(hotelId, roomTypeId).setCoverImageUrl(image.getUrl());
        return toDTO(image);
    }

    private RoomTypeEntity findRoomType(UUID hotelId, UUID roomTypeId) {
        return roomTypeRepository.findByIdAndHotelId(roomTypeId, hotelId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hạng phòng"));
    }

    /** Ảnh không thuộc hạng phòng / khách sạn trong path -> 404 (không cho sửa chéo). */
    private RoomTypeImageEntity findImage(UUID hotelId, UUID roomTypeId, UUID imageId) {
        findRoomType(hotelId, roomTypeId);
        return roomTypeImageRepository.findById(imageId)
                .filter(image -> image.getRoomTypeId().equals(roomTypeId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy ảnh"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RoomTypeImageDTO toDTO(RoomTypeImageEntity entity) {
        RoomTypeImageDTO dto = new RoomTypeImageDTO();
        dto.setId(entity.getId());
        dto.setRoomTypeId(entity.getRoomTypeId());
        dto.setUrl(entity.getUrl());
        dto.setCaption(entity.getCaption());
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
