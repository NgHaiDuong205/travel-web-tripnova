package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.UserEntity;

import java.util.List;

public interface UserRepositoryCustom {
    /**
     * @param keyword tìm theo email / họ tên / số điện thoại (null = bỏ qua)
     * @param role    tên role, VD ADMIN (null = tất cả)
     * @param active  true = đang hoạt động, false = bị khoá (null = tất cả)
     */
    List<UserEntity> findForAdmin(String keyword, String role, Boolean active, int page, int limit);

    long countForAdmin(String keyword, String role, Boolean active);
}
