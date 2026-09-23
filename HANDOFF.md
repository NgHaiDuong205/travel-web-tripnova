# HANDOFF — TripNova (BE + FE)

Cập nhật: 2026-09-23. Phiên trước chỉ đọc code + viết file này, **chưa code thêm gì mới**.

## 1. Mục tiêu tổng thể
Làm tiếp các API trong spec `C:\Users\HPC\Desktop\1.pdf` cho cả:
- **BE**: `D:\travel-web-tripnova` (Spring Boot 4.0.3, Java 21, JPA, PostgreSQL `travel-web-project`, ModelMapper).
- **FE**: `D:\fe-tripnova` (React 19 CRA, react-router 7, axios, Tailwind 3).

Tài liệu tham chiếu trong repo BE (chưa commit):
- `proposed_apis.txt` — bản chuẩn hoá lại toàn bộ PDF + đề xuất thêm (~230 endpoint, gắn nhãn `[OK]/[KEEP]/[FIX]/[NEW]/[AUTH]/[ADMIN]`). **Dùng file này thay cho đọc PDF.**
- `db_schema_guide.txt` — mô tả 59 bảng Postgres theo domain + luồng đặt phòng (cuối file).
- `.cursorrules` — quy chuẩn code BẮT BUỘC (xem mục 5).

## 2. Đã hoàn thành
### BE — đã commit (d3dfe75 trở về trước)
| Endpoint | File |
|---|---|
| `GET /api/countries/`, `/{id}/`, `POST /`, `DELETE /{id}/` | `api/CountryAPI.java` |
| `GET /api/destinations/`, `/{destinationId}/`, `/{id}/landmarks/`, `/{id}/landmarks/{landmarkId}/` | `api/DestinationAPI.java` |
| `GET /api/landmarks/` | `api/LandmarkAPI.java` |
| `GET /api/hotels/`, `/{id}/`, `/{hotelId}/rooms/`, `/{hotelId}/rooms/{roomId}/` | `api/HotelAPI.java` |

### BE — đã sửa nhưng CHƯA commit (build `mvnw -o compile` PASS)
Tính năng: **đếm phòng còn trống theo ngày + header phân trang + amenities batch**.
- `HotelAPI.java`: list trả header `X-Total-Count`; `GET /api/hotels/{id}/` nhận thêm `?checkIn=&checkOut=`.
- `WebCorsConfig.java`: `exposedHeaders("X-Total-Count")` (thiếu cái này FE không đọc được header).
- `HotelSearchBuilder.java` + `HotelSearchBuilderConverter.java`: thêm `checkIn/checkOut` (LocalDate).
- `util/DateUtil.java` (mới): `parseLocalDate(Object)` → null nếu sai format.
- `HotelDTO.java`: thêm `remainingRooms`.
- `HotelRepository.java`: `findAmenityNamesByHotelIds` (tránh N+1).
- `HotelRepositoryCustom` / `HotelRepositoryImpl.java`: thêm `countHotel(builder)`, refactor lọc + phân trang (`page` 1-based, `limit`, có `DEFAULT_LIMIT`/`MAX_LIMIT`).
- `RoomRepository.java`: các query đếm phòng trống (loại phòng có `room_availability.status IN ('booked','blocked')` trong `[checkIn, checkOut)`), `findBookedOrBlockedRoomStatuses`, `findAmenityNamesByRoomTypeIds`.
- `HotelServiceImpl.java`: gắn `remainingRooms` + amenities cho list và detail; `countHotel`.
- `RoomServiceImpl.java`, `RoomDTOConverter.java`, `RoomSearchBuilderConverter.java`, `RoomRepositoryImpl.java`: trạng thái phòng theo ngày + amenities theo room type.
- `HotelDTOConverter.java`: overload `toHotelDTO(entity, amenities)`.
- `CountryRepositoryImpl.java`: sửa nhỏ.
- Đã xoá `schema_dump.txt`, `schema_dump_utf8.txt` (thay bằng `db_schema_guide.txt`).

### FE — `D:\fe-tripnova` (toàn bộ CHƯA commit, git chỉ có commit CRA init)
- Hạ tầng: `src/config/axiosConfig.js` (baseURL từ `REACT_APP_API_BASE_URL`, default `http://localhost:8080`), `src/setupProxy.js`, `.env.development`, `STRUCTURE.md` (quy tắc: page không gọi axios trực tiếp, chỉ qua `services/`).
- Services đã có: `countryService.js`, `destinationService.js`, `hotelService.js`, `roomService.js` (+ `utils/apiHelpers.js`: `pickField`, `unwrapListResponse`).
- Hooks: `useHotelsPage.js`, `useRoomsPage.js`, `useCountryDestinationOptions.js`.
- Pages đã nối API: Home, Hotels, HotelDetail, Rooms, RoomDetail, Destinations, DestinationDetail.
- Pages mới là UI tĩnh/placeholder: Login, Register, Profile, BookingHistory, PaymentHistory, SavedTrips, AccountSettings, About. `context/AuthContext.js` chưa gắn API thật. `routes/AdminRoutes.js` chưa có trang admin.

## 3. Đang dở ở đâu
- Nhóm **Hotel GET** (mục 6 trong `proposed_apis.txt`) xong code, **chưa commit**, chưa test tay với DB thật cho trường hợp có `checkIn/checkOut`.
- Chưa bắt đầu bất kỳ endpoint nào cần đăng nhập. **BE chưa có Spring Security / JWT** (pom chỉ có data-jpa, webmvc, thymeleaf, modelmapper, postgresql, lombok).

## 4. Bước tiếp theo (theo thứ tự)
1. Test tay các endpoint hotel/room (lệnh ở mục 7) → commit BE (`feat: hotel availability count + X-Total-Count`) và commit FE lần đầu.
2. **Auth** (chặn mọi thứ phía sau): thêm `spring-boot-starter-security`, `spring-boot-starter-validation`, lib JWT (jjwt). Viết `UserRepository`, `AuthAPI` (`/api/auth/register|login|logout|refresh-token|me`), BCrypt cho `users.password_hash`, gán role qua `user_roles`. FE: `services/authService.js`, nối Login/Register, `AuthContext` lưu token, axios interceptor gắn `Authorization: Bearer`, `routes/PrivateRoute.js`.
3. **Hotel booking** `POST /api/hotel-bookings/` theo luồng trong PDF / cuối `db_schema_guide.txt`:
   - GĐ1 (1 transaction): check trống trên `room_availability` → insert `orders(pending)` + `hotel_bookings(pending)` + `payments(pending)` → trả `{orderId, bookingId, amount, paymentUrl}`.
   - GĐ2 webhook `POST /api/payments/webhook/{provider}`: payment `completed`+`paid_at`, order `paid`, booking `confirmed`, INSERT `room_availability(room_id, date, 'booked')` cho mỗi ngày `checkIn ≤ d < checkOut`. Bắt lỗi unique `(room_id,date)` → rollback + refund. `rooms.status` giữ nguyên `available`.
   - Cần tạo mới: `RoomAvailabilityRepository`, `OrderRepository`, `PaymentRepository`, `HotelBookingRepository`, DTO request/response, `HotelBookingAPI`, `PaymentAPI`. FE: nút "Đặt phòng" ở RoomDetail → trang checkout.
4. `/api/me/bookings`, `/api/me/payments`, `/api/me/profile` → nối các page BookingHistory / PaymentHistory / Profile.
5. Lookup nhỏ: `/api/continents/`, `/api/amenities/`, `/api/room-types/` (entity đã có sẵn).
6. Sau đó mới tới Cars / Flights / Tours (chưa có entity — phải tạo từ `db_schema_guide.txt`), rồi Admin.

## 5. Quyết định kỹ thuật & lý do
- **Giữ trailing slash** (`/api/hotels/`) dù `proposed_apis.txt` đề xuất bỏ: `.cursorrules` bắt buộc, toàn bộ BE + FE đang dùng. Không đổi lẻ tẻ.
- **Phân trang**: `page` (1-based) + `limit`, tổng số qua header `X-Total-Count` (không bọc body) — FE `hotelService.buildHotelQueryParams` đang gửi đúng kiểu này. Không dùng `page=0&size=` của Spring Data.
- **Không Lombok**: getter/setter/constructor viết tay; constructor injection thủ công (`.cursorrules`).
- Query động dùng `EntityManager` + native SQL có binding trong `repository/custom/impl`; query tĩnh dùng JPQL `@Query`.
- Tính phòng trống bằng `NOT EXISTS room_availability` với khoảng `[checkIn, checkOut)` — ngày checkout không tính đêm.
- Amenities/remainingRooms lấy **batch theo list hotelIds** rồi ghép bằng Map (tránh N+1).
- OAuth: dùng path chuẩn Spring `/oauth2/authorization/{provider}`, `/login/oauth2/code/{provider}` (PDF ghi có prefix `/api` — sai).
- Hủy booking: `POST .../cancel` thay vì `DELETE`.

## 6. Bẫy đã gặp / cần lưu ý
- `pdftotext` (mingw) làm vỡ tiếng Việt trong `1.pdf`; `pypdf` chưa cài. → đọc `proposed_apis.txt` (đã tổng hợp PDF), hoặc `pip install pypdf`.
- `ddl-auto=none`: schema do DB quản lý, **không** để Hibernate tạo bảng. Entity phải khớp cột thật trong `db_schema_guide.txt`.
- Các cột status (`room_availability.status`, `rooms.status`, `orders.status`…) là **Postgres ENUM** nhưng entity map `String`. SELECT/so sánh chạy được; **INSERT/UPDATE có thể lỗi** `column "status" is of type availability_status but expression is of type character varying`. Khi làm booking: thêm `@JdbcType(PostgreSQLEnumJdbcType.class)` hoặc `?stringtype=unspecified` vào JDBC URL.
- `application.properties` bật profile `uat`; `application-uat.properties` chứa user/pass DB và bị `.gitignore` — không commit, không in ra.
- CORS: header tuỳ biến phải thêm vào `exposedHeaders`, nếu không axios đọc `undefined`.
- FE: `paramsSerializer: { indexes: null }` để gửi `amenities=a&amenities=b` (Spring bind `List<String>`); bỏ đi sẽ thành `amenities[]=` và BE không nhận.
- `HotelAPI` nhận `@RequestParam Map<String,Object> params` → mọi query param (kể cả `amenities`, `page`) đều lọt vào map; converter phải bỏ qua key không dùng.

## 7. Lệnh chạy / test
```bash
# BE (cần Postgres local :5432, DB travel-web-project)
cd D:/travel-web-tripnova
./mvnw -o compile            # build nhanh (offline) — hiện PASS
./mvnw spring-boot:run       # chạy :8080
./mvnw test

# Smoke test
curl -i "http://localhost:8080/api/hotels/?page=1&limit=5"                       # xem header X-Total-Count
curl "http://localhost:8080/api/hotels/?checkIn=2026-10-01&checkOut=2026-10-03&amenities=Wifi"
curl "http://localhost:8080/api/hotels/<hotelId>/?checkIn=2026-10-01&checkOut=2026-10-03"
curl "http://localhost:8080/api/hotels/<hotelId>/rooms/?checkIn=2026-10-01&checkOut=2026-10-03"
curl "http://localhost:8080/api/destinations/"

# FE
cd D:/fe-tripnova
npm install
npm start                    # :3000, gọi BE qua REACT_APP_API_BASE_URL (.env.development)
```
