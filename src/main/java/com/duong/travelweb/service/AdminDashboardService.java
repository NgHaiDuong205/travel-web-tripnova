package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AdminDashboardDTO;
import com.duong.travelweb.model.dto.DashboardStatisticsDTO;
import com.duong.travelweb.model.dto.RevenuePointDTO;
import com.duong.travelweb.model.dto.TopItemDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Khoảng thời gian [from, to] tính cả 2 đầu; null -> 30 ngày gần nhất tính đến hôm nay.
 * granularity / groupBy: day | week | month (null -> day).
 */
public interface AdminDashboardService {
    AdminDashboardDTO getDashboard();

    DashboardStatisticsDTO getStatistics(LocalDate from, LocalDate to, String granularity);

    /** Doanh thu (giao dịch thành công theo paid_at) từng kỳ, kỳ trống = 0. */
    List<RevenuePointDTO> getRevenue(LocalDate from, LocalDate to, String groupBy);

    List<TopItemDTO> getTopHotels(LocalDate from, LocalDate to, int limit);

    List<TopItemDTO> getTopDestinations(LocalDate from, LocalDate to, int limit);
}
