-- Xe demo cho các điểm đến Việt Nam (AI Planner đặt trọn gói khách sạn + xe).
-- Sao chép thông số / ảnh của 4 xe demo DEMO-01 (Vios tự lái), DEMO-04 (VF 8 tự lái), DEMO-05 (Transit 16 chỗ có tài xế),
-- DEMO-06 (Carnival có tài xế) sang 10 thành phố. Biển số VN-<TÊN>-<số> → chạy lại an toàn (ON CONFLICT DO NOTHING).
-- Xoá: DELETE FROM cars WHERE license_plate LIKE 'VN-%' AND NOT EXISTS (SELECT 1 FROM car_bookings b WHERE b.car_id = cars.id);
-- Chạy: psql -1 -v ON_ERROR_STOP=1 -f db/seed/demo_cars_vn.sql
INSERT INTO cars (id, destination_id, name, brand, model, license_plate, car_type, seats, transmission, fuel_type,
                  price_per_day, with_driver, pickup_location, description, cover_image_url, is_active, created_at, updated_at)
SELECT gen_random_uuid(), d.id, c.name, c.brand, c.model,
       'VN-' || upper(left(regexp_replace(d.name, '[^A-Za-z]', '', 'g'), 8)) || '-' || right(c.license_plate, 2),
       c.car_type, c.seats, c.transmission, c.fuel_type, c.price_per_day, c.with_driver,
       CASE WHEN c.with_driver THEN 'Hotel pick-up in ' || d.name ELSE d.name || ' city office' END,
       c.description, c.cover_image_url, true, now(), now()
FROM destinations d
JOIN cars c ON c.license_plate IN ('DEMO-01', 'DEMO-04', 'DEMO-05', 'DEMO-06')
WHERE d.name IN ('Hanoi', 'Ho Chi Minh City', 'Da Nang', 'Hoi An', 'Hue', 'Nha Trang', 'Da Lat', 'Ha Long Bay',
                 'Sa Pa', 'Phu Quoc')
  AND d.is_active
ON CONFLICT (license_plate) DO NOTHING;
