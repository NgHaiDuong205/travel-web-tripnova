-- Địa danh: thêm địa chỉ, liên hệ, toạ độ (nạp từ CSDL du lịch VN + geocode OSM)
-- và 2 loại mới: wellness (chăm sóc sức khoẻ), sport (thể thao).
ALTER TABLE landmarks
    ADD COLUMN IF NOT EXISTS address   text,
    ADD COLUMN IF NOT EXISTS phone     varchar(30),
    ADD COLUMN IF NOT EXISTS website   varchar(255),
    ADD COLUMN IF NOT EXISTS latitude  numeric(10, 7),
    ADD COLUMN IF NOT EXISTS longitude numeric(10, 7);

ALTER TYPE landmark_category ADD VALUE IF NOT EXISTS 'wellness';
ALTER TYPE landmark_category ADD VALUE IF NOT EXISTS 'sport';
