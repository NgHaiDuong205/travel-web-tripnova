package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.LandmarkEntity;
import com.duong.travelweb.repository.custom.LandmarkRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandmarkRepository extends JpaRepository<LandmarkEntity, UUID>, LandmarkRepositoryCustom {
    @Query("SELECT l FROM LandmarkEntity l WHERE l.destination.id = :destinationId AND l.isActive = true AND " +
           "(:category = '' OR LOWER(CAST(l.category AS string)) = LOWER(:category)) " +
           "ORDER BY l.name ASC")
    List<LandmarkEntity> queryActiveLandmarksByDestinationId(@Param("destinationId") UUID destinationId,
                                                             @Param("category") String category);

    // Tham số String không được dùng "IS NULL" (stringtype=unspecified → Postgres không suy ra kiểu) → null đổi thành ""
    default List<LandmarkEntity> findActiveLandmarksByDestinationId(UUID destinationId, String category) {
        return queryActiveLandmarksByDestinationId(destinationId, category == null ? "" : category.trim());
    }

    @Query("SELECT l FROM LandmarkEntity l WHERE l.id = :landmarkId AND l.destination.id = :destinationId AND l.isActive = true AND l.destination.isActive = true")
    Optional<LandmarkEntity> findActiveLandmarkByIdAndDestinationId(@Param("landmarkId") UUID landmarkId, 
                                                                    @Param("destinationId") UUID destinationId);

    /**
     * Ứng viên cho AI Planner (mọi địa danh active của điểm đến, kể cả nhà hàng):
     * id, name, category, description (≤400), cover, address, opening_hours, entry_fee, lat, lng,
     * avg_rating, review_count, same_point (số địa danh cùng toạ độ trong điểm đến — lớn = toạ độ cấp xã, kém chính xác).
     */
    @Query(value = """
            SELECT l.id, l.name, CAST(l.category AS text), left(l.description, 400), l.cover_image_url, l.address,
                   l.opening_hours, l.entry_fee, l.latitude, l.longitude, r.avg_rating, COALESCE(r.review_count, 0),
                   CASE WHEN l.latitude IS NULL THEN 0 ELSE count(*) OVER (PARTITION BY l.latitude, l.longitude) END
            FROM landmarks l
            LEFT JOIN LATERAL (
                SELECT avg(p.rating) AS avg_rating, count(*) AS review_count
                FROM posts p
                WHERE p.entity_type = 'landmark' AND p.entity_id = l.id AND p.status = 'approved' AND p.rating IS NOT NULL
            ) r ON true
            WHERE l.destination_id = :destinationId AND l.is_active = true
            """, nativeQuery = true)
    List<Object[]> findPlannerCandidates(@Param("destinationId") UUID destinationId);
}
