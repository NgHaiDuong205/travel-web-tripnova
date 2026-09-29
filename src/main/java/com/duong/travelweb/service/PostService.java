package com.duong.travelweb.service;

import com.duong.travelweb.builder.PostSearchBuilder;
import com.duong.travelweb.model.dto.PostDTO;
import com.duong.travelweb.model.dto.PostRequestDTO;
import com.duong.travelweb.model.dto.RatingSummaryDTO;
import com.duong.travelweb.model.entity.PostEntity;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Bài viết / đánh giá (bảng posts) về hotel, landmark, destination.
 * Bài mới và bài vừa sửa ở trạng thái pending, chỉ bài approved mới hiện công khai.
 * viewerId = user đang xem (null nếu khách), dùng để trả myReaction và cho phép tác giả xem bài chưa duyệt.
 */
public interface PostService {
    Page<PostDTO> search(PostSearchBuilder criteria, UUID viewerId, int page, int limit);

    PostDTO get(UUID postId, UUID viewerId, boolean isAdmin);

    RatingSummaryDTO ratingSummary(String entityType, UUID entityId);

    PostDTO create(UUID userId, boolean isAdmin, PostRequestDTO request);

    PostDTO update(UUID userId, boolean isAdmin, UUID postId, PostRequestDTO request);

    void delete(UUID userId, boolean isAdmin, UUID postId);

    PostDTO react(UUID userId, UUID postId, String type);

    PostDTO removeReaction(UUID userId, UUID postId);

    PostDTO updateStatus(UUID postId, String status);

    /** Bài mà viewer được phép xem (approved, hoặc là tác giả / admin), ngược lại 404. */
    PostEntity requireVisible(UUID postId, UUID viewerId, boolean isAdmin);
}
