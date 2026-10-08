package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.RouteDTO;
import com.duong.travelweb.model.dto.RouteRequestDTO;
import com.duong.travelweb.service.RoutingService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Gọi HTTP API của OSRM (/table, /route). Mặc định máy chủ demo công khai router.project-osrm.org (chỉ hợp dùng
 * thử / lượng nhỏ; routing.openstreetmap.de chỉ cho 1 request đồng thời mỗi IP và phạt băng thông khi vượt);
 * production nên tự chạy OSRM với dữ liệu Việt Nam rồi đổi app.routing.base-url + max-concurrent.
 * <ul>
 *   <li>Kết quả được cache trong bộ nhớ (LRU) theo toạ độ làm tròn 5 chữ số (~1 m).</li>
 *   <li>Tối đa {@code app.routing.max-concurrent} request cùng lúc tới máy chủ (chờ lượt quá hạn thì bỏ qua).</li>
 *   <li>429 → nghỉ {@code app.routing.throttle-pause-seconds} giây; lỗi mạng / 5xx → nghỉ {@code app.routing.pause-seconds}
 *       giây (không làm chậm mọi request khi máy chủ sập). Trong lúc nghỉ trả rỗng để bên gọi ước lượng.</li>
 * </ul>
 */
@Service
public class RoutingServiceImpl implements RoutingService {
    private static final Logger log = LoggerFactory.getLogger(RoutingServiceImpl.class);
    private static final int MAX_POINTS = 25;
    private static final int CACHE_SIZE = 2000;

    private final boolean enabled;
    private final String baseUrl;
    private final String profile;
    private final long pauseMillis;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final Map<String, Object> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private volatile long pausedUntil;
    private final long throttlePauseMillis;
    private final long timeoutMillis;
    private final Semaphore permits;
    private final int rateLimitMax;
    private final int rateLimitWindowMinutes;
    private final Map<UUID, Deque<Long>> recentCalls = new ConcurrentHashMap<>();

    public RoutingServiceImpl(@Value("${app.routing.enabled:true}") boolean enabled,
                              @Value("${app.routing.base-url:https://router.project-osrm.org}") String baseUrl,
                              @Value("${app.routing.profile:driving}") String profile,
                              @Value("${app.routing.timeout-seconds:6}") int timeoutSeconds,
                              @Value("${app.routing.pause-seconds:60}") int pauseSeconds,
                              @Value("${app.routing.throttle-pause-seconds:5}") int throttlePauseSeconds,
                              @Value("${app.routing.max-concurrent:3}") int maxConcurrent,
                              @Value("${app.routing.user-agent:TripNova/1.0 (travel planner demo)}") String userAgent,
                              @Value("${app.routing.rate-limit.max:300}") int rateLimitMax,
                              @Value("${app.routing.rate-limit.window-minutes:10}") int rateLimitWindowMinutes,
                              ObjectMapper objectMapper) {
        this.rateLimitMax = rateLimitMax;
        this.rateLimitWindowMinutes = rateLimitWindowMinutes;
        this.enabled = enabled && baseUrl != null && !baseUrl.isBlank();
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.profile = profile;
        this.pauseMillis = pauseSeconds * 1000L;
        this.throttlePauseMillis = throttlePauseSeconds * 1000L;
        this.timeoutMillis = timeoutSeconds * 1000L;
        this.permits = new Semaphore(Math.max(1, maxConcurrent), true);
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        // Chính sách máy chủ công khai yêu cầu User-Agent nhận diện được ứng dụng.
        // identity: bộ giải nén của JdkClientHttpRequestFactory đọc lỗi phản hồi nén của một số máy chủ (ZipException).
        this.restClient = RestClient.builder().requestFactory(factory).defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Encoding", "identity").build();
        this.objectMapper = objectMapper;
        // Mỗi ngày của lịch trình một tác vụ; số request thật tới máy chủ do permits giới hạn.
        this.executor = Executors.newFixedThreadPool(Math.max(1, maxConcurrent), r -> {
            Thread t = new Thread(r, "osrm-routing");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Optional<Matrix> matrix(List<double[]> points) {
        if (points.size() < 2 || points.size() > MAX_POINTS) {
            return Optional.empty();
        }
        String coords = coordinates(points);
        String key = "T" + coords;
        Object cached = cached(key);
        if (cached instanceof Matrix m) {
            return Optional.of(m);
        }
        JsonNode body = fetch("/table/v1/" + profile + "/" + coords + "?annotations=duration,distance");
        if (body == null || body.get("durations") == null || body.get("distances") == null) {
            return Optional.empty();
        }
        int n = points.size();
        double[][] km = new double[n][n];
        double[][] seconds = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                JsonNode d = body.get("distances").get(i).get(j);
                JsonNode s = body.get("durations").get(i).get(j);
                km[i][j] = d == null || d.isNull() ? Double.NaN : d.asDouble() / 1000;
                seconds[i][j] = s == null || s.isNull() ? Double.NaN : s.asDouble();
            }
        }
        Matrix matrix = new Matrix(km, seconds);
        store(key, matrix);
        return Optional.of(matrix);
    }

    @Override
    public List<Optional<Matrix>> matrices(List<List<double[]>> batches) {
        List<CompletableFuture<Optional<Matrix>>> futures = new ArrayList<>();
        for (List<double[]> points : batches) {
            futures.add(CompletableFuture.supplyAsync(() -> matrix(points), executor)
                    .exceptionally(ex -> Optional.empty()));
        }
        List<Optional<Matrix>> result = new ArrayList<>();
        for (CompletableFuture<Optional<Matrix>> f : futures) {
            result.add(f.join());
        }
        return result;
    }

    @Override
    public Optional<Route> route(List<double[]> points) {
        if (points.size() < 2 || points.size() > MAX_POINTS) {
            return Optional.empty();
        }
        String coords = coordinates(points);
        String key = "R" + coords;
        Object cached = cached(key);
        if (cached instanceof Route r) {
            return Optional.of(r);
        }
        JsonNode body = fetch("/route/v1/" + profile + "/" + coords + "?overview=full&geometries=geojson&steps=false");
        JsonNode routes = body == null ? null : body.get("routes");
        if (routes == null || !routes.isArray() || routes.isEmpty()) {
            return Optional.empty();
        }
        JsonNode best = routes.get(0);
        List<double[]> geometry = new ArrayList<>();
        JsonNode coordinates = best.path("geometry").path("coordinates");
        double[] last = null;
        for (JsonNode c : coordinates) {
            // GeoJSON là [lng, lat]; làm tròn 5 chữ số (~1 m) và bỏ điểm trùng cho gọn.
            double[] p = {round5(c.get(1).asDouble()), round5(c.get(0).asDouble())};
            if (last == null || last[0] != p[0] || last[1] != p[1]) {
                geometry.add(p);
                last = p;
            }
        }
        List<Leg> legs = new ArrayList<>();
        for (JsonNode leg : best.path("legs")) {
            legs.add(new Leg(leg.path("distance").asDouble() / 1000, leg.path("duration").asDouble()));
        }
        Route route = new Route(geometry, best.path("distance").asDouble() / 1000, best.path("duration").asDouble(), legs);
        store(key, route);
        return Optional.of(route);
    }

    @Override
    public RouteDTO directions(UUID userId, RouteRequestDTO request) {
        List<double[]> points = new ArrayList<>();
        for (List<Double> p : request.getPoints()) {
            double lat = p.get(0);
            double lng = p.get(1);
            if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
                throw ApiException.badRequest("Toạ độ không hợp lệ");
            }
            points.add(new double[]{lat, lng});
        }
        checkRateLimit(userId);
        RouteDTO dto = new RouteDTO();
        Optional<Route> route = route(points);
        if (route.isEmpty()) {
            dto.setSource("none");
            return dto;
        }
        dto.setSource("osrm");
        dto.setDistanceKm(Math.round(route.get().km() * 10) / 10.0);
        dto.setDurationMinutes((int) Math.round(route.get().seconds() / 60));
        dto.setGeometry(route.get().geometry());
        return dto;
    }

    /** Trong bộ nhớ (một instance BE); kết quả trùng lấy từ cache vẫn tính lượt để chặn vòng lặp ở FE. */
    private void checkRateLimit(UUID userId) {
        long now = System.currentTimeMillis();
        long windowStart = now - rateLimitWindowMinutes * 60_000L;
        Deque<Long> times = recentCalls.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < windowStart) {
                times.pollFirst();
            }
            if (times.size() >= rateLimitMax) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn tải đường đi quá nhiều, vui lòng thử lại sau ít phút");
            }
            times.addLast(now);
        }
    }

    /** Gọi OSRM; null khi tắt / đang tạm ngừng / lỗi / code khác "Ok" (VD NoRoute — không tạm ngừng vì do dữ liệu). */
    private JsonNode fetch(String path) {
        if (!enabled || System.currentTimeMillis() < pausedUntil) {
            return null;
        }
        try {
            if (!permits.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
                return null;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
        try {
            if (System.currentTimeMillis() < pausedUntil) {
                return null; // request khác vừa gặp lỗi trong lúc chờ lượt
            }
            String text = restClient.get().uri(URI.create(baseUrl + path)).retrieve().body(String.class);
            JsonNode body = objectMapper.readTree(text);
            return "Ok".equals(body.path("code").asString("")) ? body : null;
        } catch (RestClientResponseException ex) {
            // OSRM trả 400 kèm code NoRoute / NoSegment / InvalidQuery khi dữ liệu không hợp → không phải máy chủ sập.
            if (ex.getStatusCode().is4xxClientError() && ex.getStatusCode().value() != 429) {
                return null;
            }
            pause(ex, ex.getStatusCode().value() == 429 ? throttlePauseMillis : pauseMillis);
            return null;
        } catch (RuntimeException ex) {
            pause(ex, pauseMillis);
            return null;
        } finally {
            permits.release();
        }
    }

    private void pause(RuntimeException ex, long millis) {
        pausedUntil = System.currentTimeMillis() + millis;
        String cause = NestedExceptionUtils.getMostSpecificCause(ex).toString();
        log.warn("OSRM unavailable, routing paused {}s: {}", millis / 1000, cause.length() > 200 ? cause.substring(0, 200) : cause);
    }

    private Object cached(String key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }

    private void store(String key, Object value) {
        synchronized (cache) {
            cache.put(key, value);
        }
    }

    /** "lng,lat;lng,lat…" theo định dạng OSRM. */
    private static String coordinates(List<double[]> points) {
        StringBuilder sb = new StringBuilder();
        for (double[] p : points) {
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            sb.append(String.format(Locale.ROOT, "%.5f,%.5f", p[1], p[0]));
        }
        return sb.toString();
    }

    private static double round5(double value) {
        return Math.round(value * 1e5) / 1e5;
    }
}
