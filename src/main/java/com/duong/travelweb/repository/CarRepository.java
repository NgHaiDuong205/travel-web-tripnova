package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CarEntity;
import com.duong.travelweb.repository.custom.CarRepositoryCustom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarRepository extends JpaRepository<CarEntity, UUID>, CarRepositoryCustom {

    @Query("SELECT c FROM CarEntity c LEFT JOIN FETCH c.destination d LEFT JOIN FETCH d.country WHERE c.id = :id")
    Optional<CarEntity> findDetailById(@Param("id") UUID id);

    /** Xe đang cho thuê tại một điểm đến, rẻ trước (đặt trọn gói từ lịch trình). */
    @Query("SELECT c FROM CarEntity c WHERE c.destination.id = :destinationId AND c.isActive = true ORDER BY c.pricePerDay, c.name")
    List<CarEntity> findActiveByDestinationId(@Param("destinationId") UUID destinationId);

    /** Khoá dòng xe khi đặt / xác nhận để 2 người không cùng giữ một xe. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CarEntity c WHERE c.id = :id")
    Optional<CarEntity> lockById(@Param("id") UUID id);

    /** @param plate biển số đã viết thường */
    @Query("SELECT COUNT(c) > 0 FROM CarEntity c WHERE LOWER(c.licensePlate) = :plate AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsByPlate(@Param("plate") String plate, @Param("excludeId") UUID excludeId);

    // ---- Giá trị cho bộ lọc (chỉ xe đang cho thuê) ----

    @Query("SELECT DISTINCT c.brand FROM CarEntity c WHERE c.isActive = true AND c.brand IS NOT NULL ORDER BY c.brand")
    List<String> findActiveBrands();

    @Query("SELECT DISTINCT c.transmission FROM CarEntity c WHERE c.isActive = true AND c.transmission IS NOT NULL ORDER BY c.transmission")
    List<String> findActiveTransmissions();

    @Query("SELECT DISTINCT c.fuelType FROM CarEntity c WHERE c.isActive = true AND c.fuelType IS NOT NULL ORDER BY c.fuelType")
    List<String> findActiveFuelTypes();

    /** [giá thấp nhất, giá cao nhất] */
    @Query("SELECT MIN(c.pricePerDay), MAX(c.pricePerDay) FROM CarEntity c WHERE c.isActive = true")
    List<Object[]> findActivePriceRange();
}
