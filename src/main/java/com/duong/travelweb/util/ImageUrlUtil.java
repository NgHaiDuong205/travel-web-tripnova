package com.duong.travelweb.util;

import com.duong.travelweb.exception.ApiException;

import java.util.regex.Pattern;

/** Kiểm tra URL ảnh do admin / manager nhập (ảnh khách sạn, ảnh hạng phòng). */
public final class ImageUrlUtil {
    /**
     * URL tuyệt đối http(s), file đã upload lên server (/uploads/...) hoặc ảnh tĩnh của FE (/images/...,
     * VD kho ảnh stock /images/stock/... nạp bằng db/import).
     */
    private static final Pattern IMAGE_URL = Pattern.compile("^(https?://\\S+|/uploads/\\S+|/images/\\S+)$");

    private ImageUrlUtil() {
    }

    public static String requireValid(String url) {
        String value = url == null ? "" : url.trim();
        if (!IMAGE_URL.matcher(value).matches() || value.contains("..")) {
            throw ApiException.badRequest("URL ảnh phải bắt đầu bằng http://, https://, /uploads/ hoặc /images/");
        }
        return value;
    }
}
