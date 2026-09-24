package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CountryEntity;
import com.duong.travelweb.repository.custom.CountryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<CountryEntity, UUID>, JpaSpecificationExecutor<CountryEntity>, CountryRepositoryCustom {

    /** continentId = null → tất cả. */
    @Query("SELECT c FROM CountryEntity c JOIN FETCH c.continent " +
           "WHERE (:continentId IS NULL OR c.continent.id = :continentId) ORDER BY c.name")
    List<CountryEntity> findAllForAdmin(@Param("continentId") UUID continentId);

    /** [continentId, số quốc gia] */
    @Query("SELECT c.continent.id, COUNT(c) FROM CountryEntity c GROUP BY c.continent.id")
    List<Object[]> countGroupByContinent();

    @Query("SELECT COUNT(c) > 0 FROM CountryEntity c WHERE LOWER(c.countryCode) = LOWER(:code) " +
           "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsByCode(@Param("code") String code, @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(c) > 0 FROM CountryEntity c WHERE c.slug = :slug AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsBySlug(@Param("slug") String slug, @Param("excludeId") UUID excludeId);
}
