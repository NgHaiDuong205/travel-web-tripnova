package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ContactMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, UUID> {

    @Query(value = "SELECT c FROM ContactMessageEntity c ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM ContactMessageEntity c")
    Page<ContactMessageEntity> findAllOrdered(Pageable pageable);

    @Query(value = "SELECT c FROM ContactMessageEntity c WHERE c.status = :status ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM ContactMessageEntity c WHERE c.status = :status")
    Page<ContactMessageEntity> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(c) FROM ContactMessageEntity c WHERE c.status = :status")
    long countByStatus(@Param("status") String status);
}
