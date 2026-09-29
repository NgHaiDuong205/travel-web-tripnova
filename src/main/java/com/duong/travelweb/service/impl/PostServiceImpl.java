package com.duong.travelweb.service.impl;

import com.duong.travelweb.builder.PostSearchBuilder;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.PostDTO;
import com.duong.travelweb.model.dto.PostRequestDTO;
import com.duong.travelweb.model.dto.RatingSummaryDTO;
import com.duong.travelweb.model.entity.PostEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.CommentRepository;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.PostReactionRepository;
import com.duong.travelweb.repository.PostRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.NotificationService;
import com.duong.travelweb.service.EntityReferenceService;
import com.duong.travelweb.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PostServiceImpl implements PostService {
    public static final List<String> STATUSES = List.of("pending", "approved", "rejected");
    private static final List<String> REACTIONS = List.of("like", "dislike");
    private static final Pattern IMAGE_URL = Pattern.compile("^(https?://|/)\\S+$");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final NotificationService notificationService;
    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final CommentRepository commentRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final UserRepository userRepository;
    private final EntityReferenceService entityReferenceService;
    private final ObjectMapper objectMapper;

    public PostServiceImpl(PostRepository postRepository,
                           PostReactionRepository postReactionRepository,
                           CommentRepository commentRepository,
                           HotelBookingRepository hotelBookingRepository,
                           UserRepository userRepository,
                           EntityReferenceService entityReferenceService,
                           ObjectMapper objectMapper,
                           NotificationService notificationService) {
        this.postRepository = postRepository;
        this.postReactionRepository = postReactionRepository;
        this.commentRepository = commentRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.userRepository = userRepository;
        this.entityReferenceService = entityReferenceService;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> search(PostSearchBuilder criteria, UUID viewerId, int page, int limit) {
        if (criteria.getEntityType() != null && !EntityReferenceService.LINKED_TYPES.contains(criteria.getEntityType())) {
            throw ApiException.badRequest("entityType phải là một trong: " + String.join(", ", EntityReferenceService.LINKED_TYPES));
        }
        if (criteria.getStatus() != null && !STATUSES.contains(criteria.getStatus())) {
            throw ApiException.badRequest("Trạng thái không hợp lệ: " + criteria.getStatus());
        }
        List<PostEntity> posts = postRepository.findPosts(criteria, page, limit);
        long total = postRepository.countPosts(criteria);
        return new PageImpl<>(toDTOs(posts, viewerId), PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDTO get(UUID postId, UUID viewerId, boolean isAdmin) {
        return toDTO(requireVisible(postId, viewerId, isAdmin), viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryDTO ratingSummary(String entityType, UUID entityId) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(star, 0L);
        }
        long total = 0;
        long sum = 0;
        for (Object[] row : postRepository.countRatings(entityType, entityId)) {
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            distribution.put(star, count);
            total += count;
            sum += star * count;
        }
        RatingSummaryDTO dto = new RatingSummaryDTO();
        dto.setTotalReviews(total);
        dto.setAverageRating(total == 0 ? null : Math.round(sum * 10.0 / total) / 10.0);
        dto.setDistribution(distribution);
        return dto;
    }

    @Override
    @Transactional
    public PostDTO create(UUID userId, boolean isAdmin, PostRequestDTO request) {
        String entityType = request.getEntityType().trim().toLowerCase();
        entityReferenceService.requireActiveName(entityType, request.getEntityId());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Tài khoản không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        PostEntity post = new PostEntity();
        post.setUser(user);
        post.setEntityType(entityType);
        post.setEntityId(request.getEntityId());
        post.setIsVerifiedBooking("hotel".equals(entityType)
                && hotelBookingRepository.existsCompletedStay(userId, request.getEntityId(), LocalDate.now()));
        post.setUpvotes(0);
        post.setDownvotes(0);
        post.setCreatedAt(now);
        applyContent(post, request, isAdmin, now);
        return toDTO(postRepository.save(post), userId);
    }

    @Override
    @Transactional
    public PostDTO update(UUID userId, boolean isAdmin, UUID postId, PostRequestDTO request) {
        PostEntity post = findPost(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Bạn chỉ được sửa bài viết của mình");
        }
        if (!post.getEntityType().equals(request.getEntityType().trim().toLowerCase())
                || !post.getEntityId().equals(request.getEntityId())) {
            throw ApiException.badRequest("Không thể đổi đối tượng của bài viết, hãy tạo bài mới");
        }
        applyContent(post, request, isAdmin, LocalDateTime.now());
        return toDTO(post, userId);
    }

    @Override
    @Transactional
    public void delete(UUID userId, boolean isAdmin, UUID postId) {
        PostEntity post = findPost(postId);
        if (!isAdmin && !post.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Bạn chỉ được xoá bài viết của mình");
        }
        // comments, post_reactions: ON DELETE CASCADE
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public PostDTO react(UUID userId, UUID postId, String type) {
        String reaction = type == null ? null : type.trim().toLowerCase();
        if (!REACTIONS.contains(reaction)) {
            throw ApiException.badRequest("Reaction phải là like hoặc dislike");
        }
        PostEntity post = findPost(postId);
        if (!"approved".equals(post.getStatus())) {
            throw ApiException.badRequest("Bài viết chưa được duyệt");
        }
        if (post.getUser().getId().equals(userId)) {
            throw ApiException.badRequest("Không thể tự bình chọn bài viết của mình");
        }
        postReactionRepository.upsert(postId, userId, reaction);
        postRepository.syncReactionCounts(postId);
        return toDTO(findPost(postId), userId);
    }

    @Override
    @Transactional
    public PostDTO removeReaction(UUID userId, UUID postId) {
        PostEntity post = requireVisible(postId, userId, false);
        if (postReactionRepository.deleteByPostAndUser(post.getId(), userId) > 0) {
            postRepository.syncReactionCounts(postId);
            post = findPost(postId);
        }
        return toDTO(post, userId);
    }

    @Override
    @Transactional
    public PostDTO updateStatus(UUID postId, String status) {
        String newStatus = status == null ? null : status.trim().toLowerCase();
        if (!STATUSES.contains(newStatus)) {
            throw ApiException.badRequest("Trạng thái phải là một trong: " + String.join(", ", STATUSES));
        }
        PostEntity post = findPost(postId);
        boolean changed = !newStatus.equals(post.getStatus());
        post.setStatus(newStatus);
        post.setUpdatedAt(LocalDateTime.now());
        if (changed && !"pending".equals(newStatus)) {
            String label = post.getTitle() != null && !post.getTitle().isBlank() ? "\"" + post.getTitle() + "\"" : "Your review";
            boolean approved = "approved".equals(newStatus);
            notificationService.notify(post.getUser().getId(), "post_" + newStatus,
                    label + (approved ? " is now published" : " was not approved"),
                    approved ? null : "It did not meet our community guidelines. You can edit it and submit again.",
                    "/my-reviews", "post", post.getId());
        }
        return toDTO(post, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PostEntity requireVisible(UUID postId, UUID viewerId, boolean isAdmin) {
        PostEntity post = findPost(postId);
        boolean visible = "approved".equals(post.getStatus()) || isAdmin
                || (viewerId != null && post.getUser().getId().equals(viewerId));
        if (!visible) {
            throw ApiException.notFound("Không tìm thấy bài viết");
        }
        return post;
    }

    /** Ghi nội dung; bài của user thường (tạo mới hoặc vừa sửa) chờ duyệt lại, bài của admin duyệt luôn. */
    private void applyContent(PostEntity post, PostRequestDTO request, boolean isAdmin, LocalDateTime now) {
        String title = request.getTitle() == null || request.getTitle().isBlank() ? null : request.getTitle().trim();
        post.setTitle(title);
        post.setContent(request.getContent().trim());
        post.setRating(request.getRating());
        post.setImages(writeImages(request.getImages()));
        post.setStatus(isAdmin ? "approved" : "pending");
        post.setUpdatedAt(now);
    }

    private String writeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        List<String> urls = new ArrayList<>();
        for (String image : images) {
            String url = image.trim();
            if (!IMAGE_URL.matcher(url).matches()) {
                throw ApiException.badRequest("URL ảnh không hợp lệ: " + url);
            }
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
        return objectMapper.writeValueAsString(urls);
    }

    private List<String> readImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        try {
            List<String> urls = objectMapper.readValue(images, STRING_LIST);
            return urls == null ? List.of() : urls;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private PostEntity findPost(UUID postId) {
        return postRepository.findWithUser(postId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
    }

    private PostDTO toDTO(PostEntity post, UUID viewerId) {
        return toDTOs(List.of(post), viewerId).get(0);
    }

    /** Gom số bình luận, reaction của viewer và tên đối tượng theo lô (không N+1). */
    private List<PostDTO> toDTOs(List<PostEntity> posts, UUID viewerId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        Set<UUID> postIds = new HashSet<>();
        Map<String, Set<UUID>> idsByType = new HashMap<>();
        for (PostEntity post : posts) {
            postIds.add(post.getId());
            idsByType.computeIfAbsent(post.getEntityType(), k -> new HashSet<>()).add(post.getEntityId());
        }
        Map<UUID, Long> commentCounts = new HashMap<>();
        for (Object[] row : commentRepository.countActiveByPostIds(postIds)) {
            commentCounts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        Map<UUID, String> myReactions = new HashMap<>();
        if (viewerId != null) {
            for (Object[] row : postReactionRepository.findUserReactions(viewerId, postIds)) {
                myReactions.put((UUID) row[0], Objects.toString(row[1]));
            }
        }
        Map<String, Map<UUID, String>> names = entityReferenceService.resolveNames(idsByType);

        List<PostDTO> result = new ArrayList<>();
        for (PostEntity post : posts) {
            PostDTO dto = new PostDTO();
            dto.setId(post.getId());
            dto.setEntityType(post.getEntityType());
            dto.setEntityId(post.getEntityId());
            dto.setEntityName(names.getOrDefault(post.getEntityType(), Map.of()).get(post.getEntityId()));
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setRating(post.getRating());
            dto.setImages(readImages(post.getImages()));
            dto.setIsVerifiedBooking(Boolean.TRUE.equals(post.getIsVerifiedBooking()));
            dto.setStatus(post.getStatus());
            dto.setUpvotes(post.getUpvotes() == null ? 0 : post.getUpvotes());
            dto.setDownvotes(post.getDownvotes() == null ? 0 : post.getDownvotes());
            dto.setCommentCount(commentCounts.getOrDefault(post.getId(), 0L));
            dto.setMyReaction(myReactions.get(post.getId()));
            UserEntity author = post.getUser();
            dto.setAuthorId(author.getId());
            dto.setAuthorName(author.getDeletedAt() != null ? "Người dùng đã xoá" : author.getFullName());
            dto.setAuthorAvatar(author.getDeletedAt() != null ? null : author.getAvatarUrl());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setUpdatedAt(post.getUpdatedAt());
            result.add(dto);
        }
        return result;
    }
}
