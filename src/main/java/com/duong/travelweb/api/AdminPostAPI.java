package com.duong.travelweb.api;

import com.duong.travelweb.builder.PostSearchBuilder;
import com.duong.travelweb.model.dto.CommentDTO;
import com.duong.travelweb.model.dto.PostDTO;
import com.duong.travelweb.model.dto.StatusUpdateRequestDTO;
import com.duong.travelweb.service.CommentService;
import com.duong.travelweb.service.PostService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Kiểm duyệt bài viết / bình luận. /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminPostAPI {
    private static final int MAX_LIMIT = 100;

    private final PostService postService;
    private final CommentService commentService;

    public AdminPostAPI(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping("/api/admin/posts/")
    public ResponseEntity<List<PostDTO>> getPosts(@RequestParam(value = "status", required = false) String status,
                                                  @RequestParam(value = "entityType", required = false) String entityType,
                                                  @RequestParam(value = "entityId", required = false) UUID entityId,
                                                  @RequestParam(value = "userId", required = false) UUID userId,
                                                  @RequestParam(value = "q", required = false) String q,
                                                  @RequestParam(value = "sort", required = false) String sort,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PostSearchBuilder criteria = new PostSearchBuilder.Builder()
                .status(blankToNull(status))
                .entityType(blankToNull(entityType))
                .entityId(entityId)
                .userId(userId)
                .keyword(blankToNull(q))
                .sort(sort)
                .build();
        Page<PostDTO> result = postService.search(criteria, null, page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/api/admin/posts/{postId}/")
    public ResponseEntity<PostDTO> getPost(@PathVariable("postId") UUID postId) {
        return ResponseEntity.ok(postService.get(postId, null, true));
    }

    /** status: pending | approved | rejected */
    @PutMapping("/api/admin/posts/{postId}/status/")
    public ResponseEntity<PostDTO> updatePostStatus(@PathVariable("postId") UUID postId,
                                                    @Valid @RequestBody StatusUpdateRequestDTO request) {
        return ResponseEntity.ok(postService.updateStatus(postId, request.getStatus()));
    }

    /** Xoá hẳn bài (kèm bình luận, reaction). */
    @DeleteMapping("/api/admin/posts/{postId}/")
    public ResponseEntity<Void> deletePost(@PathVariable("postId") UUID postId) {
        postService.delete(SecurityUtil.getCurrentUserId(), true, postId);
        return ResponseEntity.noContent().build();
    }

    /** Toàn bộ bình luận của một bài, kể cả đã xoá / bị ẩn (có nội dung). */
    @GetMapping("/api/admin/posts/{postId}/comments/")
    public ResponseEntity<List<CommentDTO>> getPostComments(@PathVariable("postId") UUID postId) {
        return ResponseEntity.ok(commentService.findByPost(postId, null, true));
    }

    @GetMapping("/api/admin/comments/")
    public ResponseEntity<List<CommentDTO>> getComments(@RequestParam(value = "status", required = false) String status,
                                                        @RequestParam(value = "postId", required = false) UUID postId,
                                                        @RequestParam(value = "q", required = false) String q,
                                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Page<CommentDTO> result = commentService.findForAdmin(blankToNull(status), postId, blankToNull(q), page, clamp(limit));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    /** status: active | flagged | deleted */
    @PutMapping("/api/admin/comments/{commentId}/status/")
    public ResponseEntity<CommentDTO> updateCommentStatus(@PathVariable("commentId") UUID commentId,
                                                          @Valid @RequestBody StatusUpdateRequestDTO request) {
        return ResponseEntity.ok(commentService.updateStatus(commentId, request.getStatus()));
    }

    /** Xoá hẳn bình luận và mọi trả lời bên dưới. */
    @DeleteMapping("/api/admin/comments/{commentId}/")
    public ResponseEntity<Void> deleteComment(@PathVariable("commentId") UUID commentId) {
        commentService.hardDelete(commentId);
        return ResponseEntity.noContent().build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
