package com.duong.travelweb.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UploadService {
    /** Lưu ảnh (jpeg/png/gif/webp, xác định theo nội dung file) của user, trả về URL tuyệt đối công khai. */
    String storeImage(UUID userId, MultipartFile file);

    /**
     * Xoá ảnh đã upload. Chủ file (thư mục theo userId) hoặc admin mới được xoá.
     * URL không phải file do server này lưu -> 400.
     */
    void deleteImage(UUID userId, boolean isAdmin, String url);

    /** Xoá lặng lẽ nếu url là file do server lưu (VD avatar cũ); url ngoài -> bỏ qua. */
    void deleteQuietly(String url);
}
