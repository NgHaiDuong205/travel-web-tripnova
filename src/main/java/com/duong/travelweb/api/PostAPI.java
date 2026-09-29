package com.duong.travelweb.api;

import com.duong.travelweb.builder.PostSearchBuilder;
import com.duong.travelweb.model.dto.CommentDTO;
import com.duong.travelweb.model.dto.CommentRequestDTO;
import com.duong.travelweb.model.dto.PostDTO;
import com.duong.travelweb.model.dto.PostReactionRequestDTO;
import com.duong.travelweb.model.dto.PostRequestDTO;
import com.duong.travelweb.model.dto.RatingSummaryDTO;
import com.duong.travelweb.service.CommentService;
import com.duong.travelweb.service.PostService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Bài viết / đánh giá và bình luận. GET /api/posts/** công khai (chỉ bài đã duyệt; kèm token thì có myReaction
 * và tác giả xem được bài chưa duyệt của mình), các thao tác ghi yêu cầu đăng nhập.
 * Đánh giá khách sạn = /api/posts/?entityType=hotel&entityId=...
 */
@RestController
public class PostAPI {
    private static final int MAX_LIMIT = 100;

    private final PostService postService;
    private final CommentService commentService;

    public PostAPI(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping("/api/posts/")
    public ResponseEntity<List<PostDTO>> getPosts(@RequestParam(value = "entityType", required = false) String entityType,
                                                  @RequestParam(value = "entityId", required = false) UUID entityId,
                                                  @RequestParam(value = "userId", required = false) UUID userId,
                                                  @RequestParam(value = "q", required = false) String q,
                                                  @RequestParam(value = "rating", required = false) Integer rating,
                                                  @RequestParam(value = "sort", required = false) String sort,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "limit", defaultValue = "10") int limit) {
        PostSearchBuilder criteria = new PostSearchBuilder.Builder()
                .entityType(blankToNull(entityType))
                .entityId(entityId)
                .userId(userId)
                .status("approved")
                .keyword(blankToNull(q))
                .rating(rating)
                .sort(sort)
                .build();
        return toResponse(postService.search(criteria, SecurityUtil.findCurrentUserId(), page, clamp(limit)));
    }

    @GetMapping("/api/posts/rating-summary/")
    public ResponseEntity<RatingSummaryDTO> getRatingSummary(@RequestParam("entityType") String entityType,
                                                             @RequestParam("entityId") UUID entityId) {
        return ResponseEntity.ok(postService.ratingSummary(entityType, entityId));
    }

    @GetMapping("/api/posts/{postId}/")
    public ResponseEntity<PostDTO> getPost(@PathVariable("postId") UUID postId) {
        return ResponseEntity.ok(postService.get(postId, SecurityUtil.findCurrentUserId(), SecurityUtil.hasRole("ADMIN")));
    }

    @PostMapping("/api/posts/")
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody PostRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.create(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), request));
    }

    @PutMapping("/api/posts/{postId}/")
    public ResponseEntity<PostDTO> updatePost(@PathVariable("postId") UUID postId,
                                              @Valid @RequestBody PostRequestDTO request) {
        return ResponseEntity.ok(postService.update(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), postId, request));
    }

    @DeleteMapping("/api/posts/{postId}/")
    public ResponseEntity<Void> deletePost(@PathVariable("postId") UUID postId) {
        postService.delete(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), postId);
        return ResponseEntity.noContent().build();
    }

    /** Thêm hoặc đổi reaction (like | dislike), trả về bài với số đếm mới. */
    @PostMapping("/api/posts/{postId}/reactions/")
    public ResponseEntity<PostDTO> react(@PathVariable("postId") UUID postId,
                                         @Valid @RequestBody PostReactionRequestDTO request) {
        return ResponseEntity.ok(postService.react(SecurityUtil.getCurrentUserId(), postId, request.getType()));
    }

    @DeleteMapping("/api/posts/{postId}/reactions/")
    public ResponseEntity<PostDTO> removeReaction(@PathVariable("postId") UUID postId) {
        return ResponseEntity.ok(postService.removeReaction(SecurityUtil.getCurrentUserId(), postId));
    }

    /** Danh sách phẳng theo thời gian tăng dần, FE dựng cây theo parentId. */
    @GetMapping("/api/posts/{postId}/comments/")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable("postId") UUID postId) {
        return ResponseEntity.ok(commentService.findByPost(postId, SecurityUtil.findCurrentUserId(), false));
    }

    @PostMapping("/api/posts/{postId}/comments/")
    public ResponseEntity<CommentDTO> createComment(@PathVariable("postId") UUID postId,
                                                    @Valid @RequestBody CommentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), postId, request));
    }

    @PutMapping("/api/comments/{commentId}/")
    public ResponseEntity<CommentDTO> updateComment(@PathVariable("commentId") UUID commentId,
                                                    @Valid @RequestBody CommentRequestDTO request) {
        return ResponseEntity.ok(commentService.update(SecurityUtil.getCurrentUserId(), commentId, request));
    }

    @DeleteMapping("/api/comments/{commentId}/")
    public ResponseEntity<Void> deleteComment(@PathVariable("commentId") UUID commentId) {
        commentService.delete(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), commentId);
        return ResponseEntity.noContent().build();
    }

    /** Bài của chính mình, mọi trạng thái (?status=pending|approved|rejected). */
    @GetMapping("/api/me/posts/")
    public ResponseEntity<List<PostDTO>> getMyPosts(@RequestParam(value = "status", required = false) String status,
                                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                                    @RequestParam(value = "limit", defaultValue = "10") int limit) {
        UUID userId = SecurityUtil.getCurrentUserId();
        PostSearchBuilder criteria = new PostSearchBuilder.Builder()
                .userId(userId)
                .status(blankToNull(status))
                .build();
        return toResponse(postService.search(criteria, userId, page, clamp(limit)));
    }

    private ResponseEntity<List<PostDTO>> toResponse(Page<PostDTO> result) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int clamp(int limit) {
        return limit < 1 ? 10 : Math.min(limit, MAX_LIMIT);
    }
}
