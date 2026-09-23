package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ContactMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, UUID> {
}
