package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.HotelAssignmentRequestDTO;
import com.duong.travelweb.model.dto.HotelManagerDTO;
import com.duong.travelweb.model.dto.HotelManagerRequestDTO;

import java.util.List;
import java.util.UUID;

/** Admin tạo tài khoản quản lý khách sạn (role HOTEL_MANAGER) và gán khách sạn cho tài khoản đó. */
public interface HotelManagerAccountService {
    /** Mọi tài khoản có role HOTEL_MANAGER (chưa xoá), kèm khách sạn đang quản lý. */
    List<HotelManagerDTO> findManagers();

    /** Tạo tài khoản mới; password trống → sinh mật khẩu tạm, trả về một lần trong `temporaryPassword`. */
    HotelManagerDTO createManager(UUID adminId, HotelManagerRequestDTO request);

    /** Gán lại toàn bộ khách sạn của manager (khách sạn không còn trong danh sách → bỏ người quản lý). */
    HotelManagerDTO assignHotels(UUID managerId, HotelAssignmentRequestDTO request);
}
