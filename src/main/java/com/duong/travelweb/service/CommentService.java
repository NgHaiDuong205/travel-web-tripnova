package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.CommentDTO;
import com.duong.travelweb.model.dto.CommentRequestDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Bình luận nhiều tầng dưới bài viết. Xoá của user = xoá mềm (status deleted);
 * bình luận deleted/flagged bị ẩn nội dung, và bị bỏ hẳn nếu không còn trả lời nào hiển thị bên dưới.
 */
public interface CommentService {
    List<CommentDTO> findByPost(UUID postId, UUID viewerId, boolean isAdmin);

    CommentDTO create(UUID userId, boolean isAdmin, UUID postId, CommentRequestDTO request);

    CommentDTO update(UUID userId, UUID commentId, CommentRequestDTO request);

    void delete(UUID userId, boolean isAdmin, UUID commentId);

    Page<CommentDTO> findForAdmin(String status, UUID postId, String keyword, int page, int limit);

    CommentDTO updateStatus(UUID commentId, String status);

    /** Xoá hẳn (kèm mọi trả lời bên dưới — ON DELETE CASCADE). */
    void hardDelete(UUID commentId);
}
