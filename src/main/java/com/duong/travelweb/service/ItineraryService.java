package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.ItineraryDTO;
import com.duong.travelweb.model.dto.ItineraryItemRequestDTO;
import com.duong.travelweb.model.dto.ItineraryRequestDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

/** Lịch trình cá nhân. Mọi thao tác chỉ trên lịch trình của chính user (của người khác -> 404). */
public interface ItineraryService {
    /** Danh sách không kèm items, mới cập nhật trước; page bắt đầu từ 1. */
    Page<ItineraryDTO> findMine(UUID userId, int page, int limit);

    ItineraryDTO getMine(UUID userId, UUID itineraryId);

    ItineraryDTO create(UUID userId, ItineraryRequestDTO request);

    /** Ghi đè toàn bộ; 400 nếu rút ngắn chuyến đi mà còn hoạt động ở ngày bị cắt. */
    ItineraryDTO update(UUID userId, UUID itineraryId, ItineraryRequestDTO request);

    void delete(UUID userId, UUID itineraryId);

    /** Thêm hoạt động, trả về lịch trình đầy đủ. */
    ItineraryDTO addItem(UUID userId, UUID itineraryId, ItineraryItemRequestDTO request);

    void deleteItem(UUID userId, UUID itineraryId, UUID itemId);
}
