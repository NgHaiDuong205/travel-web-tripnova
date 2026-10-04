-- Chatbot AI (Text-to-SQL + RAG): schema `chatbot` gồm các view công khai, 2 user DB quyền hạn chế.
-- Chạy bằng superuser (cần CREATE EXTENSION). Chạy lại nhiều lần được.
-- Mật khẩu 2 role KHÔNG nằm ở đây; đặt tay:
--   ALTER ROLE chatbot_ro  LOGIN PASSWORD '...';
--   ALTER ROLE chatbot_rag LOGIN PASSWORD '...';
-- rồi ghi vào .env của dịch vụ Python (DATABASE_URL_RO / DATABASE_URL_RAG).
--
-- Quy ước view: cột tiền (USD) có hậu tố `_usd` -> dịch vụ Python dựa vào hậu tố này để
-- đổi sang tiền hiển thị (VND khi tiếng Việt). Chỉ bản ghi đang active, không có cột nhạy cảm
-- (phone/email/managed_by/license_plate/google_place_id...).

CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE SCHEMA IF NOT EXISTS chatbot;

-- So tên không phân biệt hoa thường / dấu: chatbot.norm('Đà Nẵng') = chatbot.norm('Da Nang').
-- unaccent không đổi đ/Đ -> thay tay.
CREATE OR REPLACE FUNCTION chatbot.norm(t text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, translate($1, 'đĐ', 'dD'))) $$;

CREATE OR REPLACE VIEW chatbot.destinations AS
SELECT d.id, d.name, c.name AS country, c.country_code, ct.name AS continent,
       d.is_popular, d.latitude, d.longitude, d.description
FROM destinations d
JOIN countries c ON c.id = d.country_id
LEFT JOIN continents ct ON ct.id = c.continent_id
WHERE d.is_active;

CREATE OR REPLACE VIEW chatbot.hotels AS
SELECT h.id, h.name, d.name AS destination, h.destination_id, c.name AS country, h.address,
       h.star_rating, p.min_price_usd, r.avg_rating, COALESCE(r.review_count, 0) AS review_count,
       h.cancellation_policy, h.cancellation_hours, h.breakfast_included, h.pet_friendly,
       h.check_in_time, h.check_out_time, h.latitude, h.longitude, h.description
FROM hotels h
JOIN destinations d ON d.id = h.destination_id
JOIN countries c ON c.id = d.country_id
LEFT JOIN LATERAL (
    SELECT min(rt.price_per_night) AS min_price_usd
    FROM room_types rt WHERE rt.hotel_id = h.id AND rt.is_active
) p ON true
LEFT JOIN LATERAL (
    SELECT round(avg(po.rating), 2) AS avg_rating, count(*) AS review_count
    FROM posts po
    WHERE po.entity_type = 'hotel' AND po.entity_id = h.id AND po.status = 'approved' AND po.rating IS NOT NULL
) r ON true
WHERE h.is_active;

CREATE OR REPLACE VIEW chatbot.room_types AS
SELECT rt.id, rt.hotel_id, h.name AS hotel_name, rt.name, rt.max_occupancy, rt.bed_type,
       rt.area_sqm, rt.price_per_night AS price_per_night_usd, rt.description
FROM room_types rt
JOIN hotels h ON h.id = rt.hotel_id
WHERE rt.is_active AND h.is_active;

CREATE OR REPLACE VIEW chatbot.hotel_amenities AS
SELECT ha.hotel_id, h.name AS hotel_name, a.name AS amenity, a.category
FROM hotel_amenities ha
JOIN hotels h ON h.id = ha.hotel_id
JOIN amenities a ON a.id = ha.amenity_id
WHERE h.is_active;

CREATE OR REPLACE VIEW chatbot.tours AS
SELECT t.id, t.name, d.name AS destination, t.destination_id, t.duration_days, t.duration_nights,
       t.price_adult AS price_adult_usd, COALESCE(t.price_child, t.price_adult) AS price_child_usd,
       t.departure_date, t.return_date, t.departure_location, t.max_participants,
       r.avg_rating, COALESCE(r.review_count, 0) AS review_count,
       t.highlights::text AS highlights, t.included, t.excluded, t.description
FROM tours t
LEFT JOIN destinations d ON d.id = t.destination_id
LEFT JOIN LATERAL (
    SELECT round(avg(po.rating), 2) AS avg_rating, count(*) AS review_count
    FROM posts po
    WHERE po.entity_type = 'tour' AND po.entity_id = t.id AND po.status = 'approved' AND po.rating IS NOT NULL
) r ON true
WHERE t.is_active;

CREATE OR REPLACE VIEW chatbot.cars AS
SELECT ca.id, ca.name, ca.brand, ca.model, ca.car_type::text AS car_type, ca.seats, ca.transmission,
       ca.fuel_type, ca.with_driver, ca.price_per_day AS price_per_day_usd,
       d.name AS destination, ca.destination_id, ca.pickup_location, ca.description
FROM cars ca
LEFT JOIN destinations d ON d.id = ca.destination_id
WHERE ca.is_active;

-- Chỉ chuyến chưa bay và còn bán (đóng bán trước giờ bay 2h như FlightServiceImpl).
CREATE OR REPLACE VIEW chatbot.flights AS
SELECT f.id, f.flight_number, f.airline, f.departure_airport_code, f.arrival_airport_code,
       f.departure_city, f.arrival_city, f.departure_time, f.arrival_time, f.duration_minutes,
       f.aircraft_type, f.base_price AS base_price_usd, f.available_seats, f.baggage_policy
FROM flights f
WHERE f.is_active AND f.departure_time > now() + interval '2 hours';

CREATE OR REPLACE VIEW chatbot.landmarks AS
SELECT l.id, l.name, d.name AS destination, l.destination_id, l.category::text AS category,
       l.address, l.opening_hours, l.entry_fee AS entry_fee_usd, l.latitude, l.longitude, l.description
FROM landmarks l
JOIN destinations d ON d.id = l.destination_id
WHERE l.is_active;

-- Kho tài liệu RAG: phát hiện upload trùng + biết model đã lập chỉ mục.
ALTER TABLE knowledge_documents ADD COLUMN IF NOT EXISTS content_sha256 char(64);
ALTER TABLE knowledge_documents ADD COLUMN IF NOT EXISTS embedding_model varchar(100);
CREATE INDEX IF NOT EXISTS idx_kd_active_language ON knowledge_documents (is_active, language);
CREATE INDEX IF NOT EXISTS idx_kd_sha ON knowledge_documents (content_sha256);

-- Mỗi user 1 đánh giá cho mỗi tin nhắn (gửi lại = ghi đè).
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_feedback_message_user ON chat_feedback (message_id, user_id);

-- Role
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'chatbot_ro') THEN
        CREATE ROLE chatbot_ro LOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'chatbot_rag') THEN
        CREATE ROLE chatbot_rag LOGIN;
    END IF;
END $$;

-- chatbot_ro: chỉ đọc các view của schema chatbot (view chạy bằng quyền chủ sở hữu nên
-- không cần quyền trên bảng gốc). Không cấp gì trên bảng public.
GRANT USAGE ON SCHEMA chatbot TO chatbot_ro;
GRANT SELECT ON ALL TABLES IN SCHEMA chatbot TO chatbot_ro;
GRANT EXECUTE ON FUNCTION chatbot.norm(text) TO chatbot_ro;
ALTER ROLE chatbot_ro SET default_transaction_read_only = on;
ALTER ROLE chatbot_ro SET statement_timeout = '3s';
ALTER ROLE chatbot_ro SET search_path = chatbot;
ALTER ROLE chatbot_ro SET timezone = 'Asia/Ho_Chi_Minh';
ALTER ROLE chatbot_ro CONNECTION LIMIT 10;

-- chatbot_rag: chỉ 2 bảng kho tài liệu.
GRANT SELECT, INSERT, UPDATE, DELETE ON knowledge_documents, knowledge_chunks TO chatbot_rag;
ALTER ROLE chatbot_rag SET statement_timeout = '30s';
ALTER ROLE chatbot_rag CONNECTION LIMIT 10;
