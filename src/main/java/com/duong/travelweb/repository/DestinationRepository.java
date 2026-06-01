package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.DestinationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DestinationRepository extends JpaRepository<DestinationEntity, UUID> {
    @Query("SELECT d FROM DestinationEntity d WHERE " +
           "d.isActive = true AND " +
           "(:countryCode IS NULL OR :countryCode = '' OR LOWER(d.country.countryCode) = LOWER(:countryCode)) AND " +
           "(:continentCode IS NULL OR :continentCode = '' OR LOWER(d.country.continent.code) = LOWER(:continentCode))")
    List<DestinationEntity> findDestinations(@Param("countryCode") String countryCode, 
                                             @Param("continentCode") String continentCode);
}
