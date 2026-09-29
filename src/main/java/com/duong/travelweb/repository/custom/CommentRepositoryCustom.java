package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.CommentEntity;

import java.util.List;
import java.util.UUID;

public interface CommentRepositoryCustom {
    /**
     * Danh sách cho admin, mới nhất trước, kèm sẵn tác giả và bài viết.
     * @param status  active | flagged | deleted (null = tất cả)
     * @param postId  lọc theo bài (null = tất cả)
     * @param keyword tìm trong nội dung / tên tác giả (null = bỏ qua)
     */
    List<CommentEntity> findForAdmin(String status, UUID postId, String keyword, int page, int limit);

    long countForAdmin(String status, UUID postId, String keyword);
}
