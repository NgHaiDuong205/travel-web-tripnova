-- Ẩn (is_active = false, xoá mềm — KHÔNG xoá dữ liệu) các khách sạn Việt Nam không có điểm tham quan nào trong
-- bán kính :radius km, kể cả khách sạn chưa có toạ độ. "Điểm tham quan" = landmarks đang hoạt động, có toạ độ,
-- trừ restaurant / wellness / sport. Chạy SAU khi nạp điểm tham quan (import_osm_attractions.py).
--
--   psql -1 -v ON_ERROR_STOP=1 -v radius=10 -v out=D:/osm/hidden_hotels.csv -f db/import/hide_far_vn_hotels.sql
--
-- Danh sách id bị ẩn ghi ra file :out (id,reason) để khôi phục khi cần:
--   CREATE TEMP TABLE x (id uuid, reason text); \copy x FROM 'hidden_hotels.csv' csv
--   UPDATE hotels h SET is_active = true FROM x WHERE x.id = h.id;
CREATE TEMP TABLE far ON COMMIT DROP AS
WITH vh AS (
    SELECT h.id, h.latitude AS lat, h.longitude AS lng
    FROM hotels h
    JOIN destinations d ON d.id = h.destination_id
    JOIN countries c ON c.id = d.country_id
    WHERE c.country_code = 'VNM' AND h.is_active
), lm AS (
    SELECT latitude AS lat, longitude AS lng
    FROM landmarks
    WHERE is_active AND latitude IS NOT NULL AND category NOT IN ('restaurant', 'wellness', 'sport')
)
SELECT vh.id, CASE WHEN vh.lat IS NULL THEN 'no_coordinates' ELSE 'no_attraction_within_' || :radius || 'km' END AS reason
FROM vh
WHERE vh.lat IS NULL
   OR NOT EXISTS (
        SELECT 1 FROM lm
        WHERE abs(lm.lat - vh.lat) < :radius / 111.0 + 0.01
          AND abs(lm.lng - vh.lng) < :radius / 100.0 + 0.01
          AND 6371 * 2 * asin(sqrt(power(sin(radians(lm.lat - vh.lat) / 2), 2)
                + cos(radians(vh.lat)) * cos(radians(lm.lat)) * power(sin(radians(lm.lng - vh.lng) / 2), 2))) <= :radius);

COPY far TO STDOUT (FORMAT csv) \g :out

UPDATE hotels h SET is_active = false, updated_at = now()
FROM far
WHERE far.id = h.id;

SELECT reason, count(*) FROM far GROUP BY reason ORDER BY reason;
