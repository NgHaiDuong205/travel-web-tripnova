package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.RouteDTO;
import com.duong.travelweb.model.dto.RouteRequestDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Đường đi thật trên bản đồ đường bộ (OSRM). Mọi toạ độ là {lat, lng}.
 * Dịch vụ ngoài lỗi / tắt / quá chậm → Optional.empty(), bên gọi tự quay về ước lượng đường chim bay.
 */
public interface RoutingService {
    /** Ma trận đi ô tô giữa mọi cặp điểm: km[i][j], seconds[i][j] (NaN nếu không có đường). */
    record Matrix(double[][] km, double[][] seconds) {
    }

    /** Một chặng giữa 2 điểm liên tiếp. */
    record Leg(double km, double seconds) {
    }

    /** Đường đi qua các điểm theo thứ tự: geometry [[lat, lng], …], tổng km / giây và từng chặng. */
    record Route(List<double[]> geometry, double km, double seconds, List<Leg> legs) {
    }

    boolean isEnabled();

    Optional<Matrix> matrix(List<double[]> points);

    /** Lấy nhiều ma trận song song (VD mỗi ngày của lịch trình một ma trận); kết quả cùng thứ tự đầu vào. */
    List<Optional<Matrix>> matrices(List<List<double[]>> batches);

    Optional<Route> route(List<double[]> points);

    /**
     * Cho bản đồ ở FE: kiểm tra toạ độ (400), giới hạn tần suất theo user (429); dịch vụ ngoài lỗi → source none, không lỗi.
     */
    RouteDTO directions(UUID userId, RouteRequestDTO request);
}
