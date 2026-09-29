package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.HotelImageDTO;
import com.duong.travelweb.model.dto.HotelImageRequestDTO;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.HotelImageEntity;
import com.duong.travelweb.repository.HotelImageRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.service.HotelImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class HotelImageServiceImpl implements HotelImageService {
    private static final int MAX_IMAGES_PER_HOTEL = 30;
    /** URL tuyệt đối http(s) hoặc file đã upload lên server (/uploads/...). */
    private static final Pattern IMAGE_URL = Pattern.compile("^(https?://\\S+|/uploads/\\S+)$");

    private final HotelImageRepository hotelImageRepository;
    private final HotelRepository hotelRepository;

    public HotelImageServiceImpl(HotelImageRepository hotelImageRepository, HotelRepository hotelRepository) {
        this.hotelImageRepository = hotelImageRepository;
        this.hotelRepository = hotelRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelImageDTO> findByHotel(UUID hotelId, boolean activeOnly) {
        HotelEntity hotel = findHotel(hotelId);
        if (activeOnly && !Boolean.TRUE.equals(hotel.getIsActive())) {
            throw ApiException.notFound("Không tìm thấy khách sạn");
        }
        return hotelImageRepository.findByHotelId(hotelId).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public HotelImageDTO add(UUID adminId, UUID hotelId, HotelImageRequestDTO request) {
        // Khoá dòng hotels để 2 request thêm ảnh đồng thời không vượt giới hạn / trùng sortOrder.
        hotelRepository.lockById(hotelId).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách sạn"));
        if (hotelImageRepository.countByHotelId(hotelId) >= MAX_IMAGES_PER_HOTEL) {
            throw ApiException.badRequest("Mỗi khách sạn tối đa " + MAX_IMAGES_PER_HOTEL + " ảnh");
        }
        HotelImageEntity image = new HotelImageEntity();
        image.setHotelId(hotelId);
        image.setUrl(validUrl(request.getUrl()));
        image.setCaption(blankToNull(request.getCaption()));
        image.setSortOrder(request.getSortOrder() != null
                ? Math.max(request.getSortOrder(), 0)
                : hotelImageRepository.maxSortOrder(hotelId) + 1);
        image.setCreatedBy(adminId);
        image.setCreatedAt(LocalDateTime.now());
        return toDTO(hotelImageRepository.save(image));
    }

    @Override
    @Transactional
    public HotelImageDTO update(UUID hotelId, UUID imageId, HotelImageRequestDTO request) {
        HotelImageEntity image = findImage(hotelId, imageId);
        image.setUrl(validUrl(request.getUrl()));
        image.setCaption(blankToNull(request.getCaption()));
        if (request.getSortOrder() != null) {
            image.setSortOrder(Math.max(request.getSortOrder(), 0));
        }
        return toDTO(image);
    }

    @Override
    @Transactional
    public void delete(UUID hotelId, UUID imageId) {
        hotelImageRepository.delete(findImage(hotelId, imageId));
    }

    @Override
    @Transactional
    public HotelImageDTO setCover(UUID hotelId, UUID imageId) {
        HotelImageEntity image = findImage(hotelId, imageId);
        HotelEntity hotel = findHotel(hotelId);
        hotel.setCoverImageUrl(image.getUrl());
        hotel.setUpdatedAt(LocalDateTime.now());
        return toDTO(image);
    }

    private HotelEntity findHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách sạn"));
    }

    /** Ảnh không thuộc khách sạn trong path -> 404 (không cho sửa chéo). */
    private HotelImageEntity findImage(UUID hotelId, UUID imageId) {
        return hotelImageRepository.findById(imageId)
                .filter(image -> image.getHotelId().equals(hotelId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy ảnh"));
    }

    private String validUrl(String url) {
        String value = url == null ? "" : url.trim();
        if (!IMAGE_URL.matcher(value).matches()) {
            throw ApiException.badRequest("URL ảnh phải bắt đầu bằng http://, https:// hoặc /uploads/");
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private HotelImageDTO toDTO(HotelImageEntity entity) {
        HotelImageDTO dto = new HotelImageDTO();
        dto.setId(entity.getId());
        dto.setHotelId(entity.getHotelId());
        dto.setUrl(entity.getUrl());
        dto.setCaption(entity.getCaption());
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
