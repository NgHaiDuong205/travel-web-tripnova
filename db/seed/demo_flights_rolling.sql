-- Lịch bay demo cuốn chiếu: 10 chuyến/ngày theo đúng mẫu 50 chuyến demo ban đầu
-- (HAN/SGN/DAD/SIN/BKK, ghế business hàng 1-3 ACDF + economy 4-20 ABCDEF, business = 3x economy).
-- Sinh cho mọi ngày từ mai tới :days ngày tới (mặc định 60) mà CHƯA có chuyến demo. Chạy lại an toàn.
--   psql -v days=60 -f db/seed/demo_flights_rolling.sql
\if :{?days}
\else
\set days 60
\endif

BEGIN;

CREATE TEMP TABLE demo_slots (slot int, dep text, arr text, dep_city text, arr_city text,
                              hhmm time, minutes int, price numeric, fixed_airline text) ON COMMIT DROP;
INSERT INTO demo_slots VALUES
 (0, 'HAN','SGN','Hanoi','Ho Chi Minh City','07:00',130, 55,NULL),
 (3, 'SGN','HAN','Ho Chi Minh City','Hanoi','07:15',130, 55,NULL),
 (12,'SGN','SIN','Ho Chi Minh City','Singapore','09:00',125,115,'SQ'),
 (15,'HAN','BKK','Hanoi','Bangkok','09:15',115,115,NULL),
 (6, 'HAN','DAD','Hanoi','Da Nang','09:30', 80, 55,NULL),
 (9, 'DAD','HAN','Da Nang','Hanoi','09:45', 80, 55,NULL),
 (1, 'HAN','SGN','Hanoi','Ho Chi Minh City','13:15',130, 65,NULL),
 (4, 'SGN','HAN','Ho Chi Minh City','Hanoi','13:30',130, 65,NULL),
 (2, 'HAN','SGN','Hanoi','Ho Chi Minh City','19:30',130, 75,NULL),
 (5, 'SGN','HAN','Ho Chi Minh City','Hanoi','19:45',130, 75,NULL);

CREATE TEMP TABLE demo_airlines (k int, code text, name text, aircraft text) ON COMMIT DROP;
INSERT INTO demo_airlines VALUES (0,'VN','Vietnam Airlines','A321'), (1,'VJ','Vietjet Air','A320neo'),
                                 (2,'QH','Bamboo Airways','A321neo');

CREATE TEMP TABLE new_flights ON COMMIT DROP AS
SELECT gen_random_uuid() AS id, d.day, s.*,
       CASE WHEN s.fixed_airline = 'SQ' THEN 'SQ' ELSE a.code END AS code,
       CASE WHEN s.fixed_airline = 'SQ' THEN 'Singapore Airlines' ELSE a.name END AS airline,
       CASE WHEN s.fixed_airline = 'SQ' THEN 'B787-10' ELSE a.aircraft END AS aircraft,
       (d.day - date '2026-10-01') AS day_index
FROM generate_series(current_date + 1, current_date + :days, interval '1 day') AS g(ts)
CROSS JOIN LATERAL (SELECT g.ts::date AS day) d
CROSS JOIN demo_slots s
JOIN demo_airlines a ON a.k = ((d.day - date '2026-10-01') + s.slot) % 3
WHERE NOT EXISTS (
    SELECT 1 FROM flights f
    WHERE f.departure_airport_code = s.dep AND f.arrival_airport_code = s.arr
      AND f.departure_time = (d.day + s.hhmm) AT TIME ZONE 'Asia/Ho_Chi_Minh'
);

INSERT INTO flights (id, flight_number, airline, departure_airport_code, arrival_airport_code,
                     departure_city, arrival_city, departure_time, arrival_time, duration_minutes,
                     aircraft_type, base_price, total_seats, available_seats, baggage_policy,
                     is_active, created_at, updated_at)
SELECT n.id, n.code || (140 + 20 * n.day_index + n.slot), n.airline, n.dep, n.arr, n.dep_city, n.arr_city,
       (n.day + n.hhmm) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       (n.day + n.hhmm) AT TIME ZONE 'Asia/Ho_Chi_Minh' + make_interval(mins => n.minutes),
       n.minutes, n.aircraft, n.price, 114, 114,
       E'Carry-on 7kg included\nChecked baggage 23kg included', true, now(), now()
FROM new_flights n;

INSERT INTO flight_seats (flight_id, seat_number, seat_class, price, status)
SELECT n.id, r || l, 'business'::flight_seat_class, n.price * 3, 'available'::flight_seat_status
FROM new_flights n, generate_series(1, 3) r, unnest(ARRAY['A','C','D','F']) l
UNION ALL
SELECT n.id, r || l, 'economy'::flight_seat_class, n.price, 'available'::flight_seat_status
FROM new_flights n, generate_series(4, 20) r, unnest(ARRAY['A','B','C','D','E','F']) l;

SELECT count(*) AS flights_added FROM new_flights;
COMMIT;
