package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.UserRoleEntity;
import com.duong.travelweb.model.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
}
