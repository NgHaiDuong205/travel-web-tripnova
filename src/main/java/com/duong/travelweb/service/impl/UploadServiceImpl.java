package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lưu file ra đĩa: {app.upload.dir}/images/{userId}/{uuid}.{ext}, phục vụ công khai tại /uploads/** (xem WebMvcConfig).
 * Loại file xác định bằng magic bytes (không tin Content-Type / tên file); không nhận SVG (có thể chứa script).
 */
@Service
public class UploadServiceImpl implements UploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    /** Đường dẫn tương đối sau /uploads/ của file do server lưu. */
    private static final Pattern STORED_PATH = Pattern.compile(
            "^/uploads/images/([0-9a-f-]{36})/([0-9a-f-]{36}\\.(?:jpg|png|gif|webp))$");

    private final Path rootDir;
    private final String publicBaseUrl;

    public UploadServiceImpl(@Value("${app.upload.dir:uploads}") String uploadDir,
                             @Value("${app.upload.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.rootDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @Override
    public String storeImage(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Chưa chọn file ảnh");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw ApiException.badRequest("Ảnh tối đa 5 MB");
        }
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw ApiException.badRequest("Không đọc được file tải lên");
        }
        String ext = detectImageType(Arrays.copyOf(header, read));
        if (ext == null) {
            throw ApiException.badRequest("Chỉ nhận ảnh JPEG, PNG, GIF hoặc WEBP");
        }
        String relative = "images/" + userId + "/" + UUID.randomUUID() + "." + ext;
        Path target = rootDir.resolve(relative).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Không lưu được file upload {}", target, e);
            throw new IllegalStateException("Không lưu được file upload", e);
        }
        return publicBaseUrl + "/uploads/" + relative;
    }

    @Override
    public void deleteImage(UUID userId, boolean isAdmin, String url) {
        Matcher m = matchStored(url);
        if (m == null) {
            throw ApiException.badRequest("URL không phải ảnh đã tải lên hệ thống");
        }
        if (!isAdmin && !m.group(1).equals(userId.toString())) {
            throw ApiException.forbidden("Bạn chỉ được xoá ảnh do mình tải lên");
        }
        Path target = rootDir.resolve("images").resolve(m.group(1)).resolve(m.group(2)).normalize();
        try {
            if (!Files.deleteIfExists(target)) {
                throw ApiException.notFound("Không tìm thấy ảnh");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không xoá được file " + target, e);
        }
    }

    @Override
    public void deleteQuietly(String url) {
        Matcher m = matchStored(url);
        if (m == null) {
            return;
        }
        try {
            Files.deleteIfExists(rootDir.resolve("images").resolve(m.group(1)).resolve(m.group(2)).normalize());
        } catch (IOException e) {
            log.warn("Không xoá được file cũ {}: {}", url, e.getMessage());
        }
    }

    /** Nhận cả URL tuyệt đối (publicBaseUrl/uploads/...) lẫn đường dẫn /uploads/...; regex chặn "..". */
    private Matcher matchStored(String url) {
        if (url == null) {
            return null;
        }
        String path = url.trim();
        if (path.startsWith(publicBaseUrl + "/")) {
            path = path.substring(publicBaseUrl.length());
        }
        Matcher m = STORED_PATH.matcher(path);
        return m.matches() ? m : null;
    }

    private static String detectImageType(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return "png";
        }
        if (h.length >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8' && (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
            return "gif";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
