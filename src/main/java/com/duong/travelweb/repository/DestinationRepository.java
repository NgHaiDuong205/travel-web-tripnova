package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.repository.custom.DestinationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DestinationRepository extends JpaRepository<DestinationEntity, UUID>, DestinationRepositoryCustom {

    /** [destinationId, số khách sạn] */
    @Query("SELECT h.destination.id, COUNT(h) FROM HotelEntity h WHERE h.destination.id IN :ids GROUP BY h.destination.id")
    List<Object[]> countHotelsByDestinationIds(@Param("ids") List<UUID> ids);

    /** [destinationId, số địa danh] */
    @Query("SELECT l.destination.id, COUNT(l) FROM LandmarkEntity l WHERE l.destination.id IN :ids GROUP BY l.destination.id")
    List<Object[]> countLandmarksByDestinationIds(@Param("ids") List<UUID> ids);

    /** [countryId, số điểm đến] */
    @Query("SELECT d.country.id, COUNT(d) FROM DestinationEntity d GROUP BY d.country.id")
    List<Object[]> countGroupByCountry();
}
