package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AdminStatsDTO;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

public interface AdminStatsService {
    /** Thống kê cho các trang admin; section xem AdminStatsServiceImpl.SECTIONS. */
    AdminStatsDTO getStats(String section, LocalDate from, LocalDate to, String granularity);

    /** Thống kê vận hành cho một nhóm khách sạn (trang quản lý khách sạn), section = "hotel". */
    AdminStatsDTO getHotelStats(Collection<UUID> hotelIds, LocalDate from, LocalDate to, String granularity);
}
