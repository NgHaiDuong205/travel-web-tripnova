package com.duong.travelweb.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tham chiếu "polymorphic" (entity_type + entity_id, không có FK cứng) tới hotel / landmark / destination,
 * dùng chung cho itinerary_items, posts...
 */
public interface EntityReferenceService {
    List<String> LINKED_TYPES = List.of("hotel", "landmark", "destination");

    /** Loại đối tượng được viết review (posts.entity_type). */
    List<String> REVIEWABLE_TYPES = List.of("hotel", "landmark", "destination", "tour", "car", "flight");

    /** Tên của entity đang hoạt động; ném 400 nếu loại không hỗ trợ hoặc entity không tồn tại / đã ẩn. */
    String requireActiveName(String entityType, UUID entityId);

    /** Tên hiện tại theo loại → (id → tên), mỗi loại 1 query; id không còn tồn tại thì không có trong map. */
    Map<String, Map<UUID, String>> resolveNames(Map<String, ? extends Collection<UUID>> idsByType);
}
