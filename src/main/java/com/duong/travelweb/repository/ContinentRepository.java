package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ContinentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContinentRepository extends JpaRepository<ContinentEntity, UUID>, JpaSpecificationExecutor<ContinentEntity> {

    @Query("SELECT c FROM ContinentEntity c ORDER BY c.name")
    List<ContinentEntity> findAllOrdered();

    @Query("SELECT COUNT(c) > 0 FROM ContinentEntity c WHERE LOWER(c.code) = LOWER(:code) " +
           "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsByCode(@Param("code") String code, @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(c) > 0 FROM ContinentEntity c WHERE LOWER(c.name) = LOWER(:name) " +
           "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsByName(@Param("name") String name, @Param("excludeId") UUID excludeId);
}
