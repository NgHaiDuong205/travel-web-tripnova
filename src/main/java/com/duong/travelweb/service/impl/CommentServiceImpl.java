package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CommentDTO;
import com.duong.travelweb.model.dto.CommentRequestDTO;
import com.duong.travelweb.model.entity.CommentEntity;
import com.duong.travelweb.model.entity.PostEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.CommentRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.NotificationService;
import com.duong.travelweb.service.CommentService;
import com.duong.travelweb.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CommentServiceImpl implements CommentService {
    public static final List<String> STATUSES = List.of("active", "flagged", "deleted");

    private final NotificationService notificationService;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    public CommentServiceImpl(CommentRepository commentRepository,
                              UserRepository userRepository,
                              PostService postService,
                              NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postService = postService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> findByPost(UUID postId, UUID viewerId, boolean isAdmin) {
        postService.requireVisible(postId, viewerId, isAdmin);
        List<CommentEntity> comments = commentRepository.findByPostId(postId);
        if (isAdmin) {
            return comments.stream().map(c -> toDTO(c, true)).toList();
        }
        // Giữ bình luận đang hiển thị và các "tổ tiên" của nó (để cây không bị đứt).
        Set<UUID> keep = new HashSet<>();
        for (CommentEntity comment : comments) {
            if (!"active".equals(comment.getStatus())) {
                continue;
            }
            CommentEntity current = comment;
            while (current != null && keep.add(current.getId())) {
                current = current.getParentComment();
            }
        }
        List<CommentDTO> result = new ArrayList<>();
        for (CommentEntity comment : comments) {
            if (keep.contains(comment.getId())) {
                result.add(toDTO(comment, false));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public CommentDTO create(UUID userId, boolean isAdmin, UUID postId, CommentRequestDTO request) {
        PostEntity post = postService.requireVisible(postId, userId, isAdmin);
        if (!"approved".equals(post.getStatus())) {
            throw ApiException.badRequest("Chỉ bình luận được bài viết đã duyệt");
        }
        CommentEntity parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findWithUserAndPost(request.getParentId())
                    .filter(c -> c.getPost().getId().equals(postId))
                    .orElseThrow(() -> ApiException.badRequest("Bình luận được trả lời không thuộc bài viết này"));
            if (!"active".equals(parent.getStatus())) {
                throw ApiException.badRequest("Không thể trả lời bình luận đã bị xoá hoặc ẩn");
            }
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Tài khoản không tồn tại"));
        LocalDateTime now = LocalDateTime.now();
        CommentEntity comment = new CommentEntity();
        comment.setPost(post);
        comment.setUser(user);
        comment.setParentComment(parent);
        comment.setContent(request.getContent().trim());
        comment.setStatus("active");
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        CommentEntity saved = commentRepository.save(comment);
        UserEntity recipient = parent != null ? parent.getUser() : post.getUser();
        if (!recipient.getId().equals(userId)) {
            String link = "hotel".equals(post.getEntityType()) ? "/hotel/" + post.getEntityId() : "/my-reviews";
            String preview = saved.getContent().length() > 200 ? saved.getContent().substring(0, 200) + "…" : saved.getContent();
            notificationService.notify(recipient.getId(), parent != null ? "comment_reply" : "post_comment",
                    user.getFullName() + (parent != null ? " replied to your comment" : " commented on your review"),
                    preview, link, "post", post.getId());
        }
        return toDTO(saved, false);
    }

    @Override
    @Transactional
    public CommentDTO update(UUID userId, UUID commentId, CommentRequestDTO request) {
        CommentEntity comment = findComment(commentId);
        if (!comment.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Bạn chỉ được sửa bình luận của mình");
        }
        if (!"active".equals(comment.getStatus())) {
            throw ApiException.badRequest("Bình luận đã bị xoá hoặc ẩn");
        }
        comment.setContent(request.getContent().trim());
        comment.setUpdatedAt(LocalDateTime.now());
        return toDTO(comment, false);
    }

    @Override
    @Transactional
    public void delete(UUID userId, boolean isAdmin, UUID commentId) {
        CommentEntity comment = findComment(commentId);
        if (!isAdmin && !comment.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Bạn chỉ được xoá bình luận của mình");
        }
        if ("deleted".equals(comment.getStatus())) {
            return;
        }
        comment.setStatus("deleted");
        comment.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> findForAdmin(String status, UUID postId, String keyword, int page, int limit) {
        if (status != null && !STATUSES.contains(status)) {
            throw ApiException.badRequest("Trạng thái không hợp lệ: " + status);
        }
        List<CommentDTO> comments = commentRepository.findForAdmin(status, postId, keyword, page, limit)
                .stream().map(c -> toDTO(c, true)).toList();
        long total = commentRepository.countForAdmin(status, postId, keyword);
        return new PageImpl<>(comments, PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional
    public CommentDTO updateStatus(UUID commentId, String status) {
        String newStatus = status == null ? null : status.trim().toLowerCase();
        if (!STATUSES.contains(newStatus)) {
            throw ApiException.badRequest("Trạng thái phải là một trong: " + String.join(", ", STATUSES));
        }
        CommentEntity comment = findComment(commentId);
        comment.setStatus(newStatus);
        comment.setUpdatedAt(LocalDateTime.now());
        return toDTO(comment, true);
    }

    @Override
    @Transactional
    public void hardDelete(UUID commentId) {
        commentRepository.delete(findComment(commentId));
    }

    private CommentEntity findComment(UUID commentId) {
        return commentRepository.findWithUserAndPost(commentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bình luận"));
    }

    /** showHidden = false: bình luận không active bị ẩn nội dung và tác giả. */
    private CommentDTO toDTO(CommentEntity comment, boolean showHidden) {
        boolean active = "active".equals(comment.getStatus());
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPost().getId());
        if (showHidden) {
            dto.setPostTitle(comment.getPost().getTitle());
        }
        dto.setParentId(comment.getParentComment() == null ? null : comment.getParentComment().getId());
        dto.setStatus(comment.getStatus());
        if (active || showHidden) {
            UserEntity author = comment.getUser();
            dto.setContent(comment.getContent());
            dto.setAuthorId(author.getId());
            dto.setAuthorName(author.getDeletedAt() != null ? "Người dùng đã xoá" : author.getFullName());
            dto.setAuthorAvatar(author.getDeletedAt() != null ? null : author.getAvatarUrl());
        }
        dto.setIsEdited(active && comment.getCreatedAt() != null && comment.getUpdatedAt() != null
                && comment.getUpdatedAt().isAfter(comment.getCreatedAt().plusSeconds(1)));
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        return dto;
    }
}
