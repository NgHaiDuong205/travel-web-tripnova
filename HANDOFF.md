# HANDOFF — TripNova (BE + FE)

Cập nhật: 2026-09-24. Người dùng yêu cầu **làm lần lượt cả FE + BE, không cần hỏi ý kiến**.

## 1. Mục tiêu tổng thể
Làm hết các API trong spec `C:\Users\HPC\Desktop\1.pdf` cho:
- **BE**: `D:\travel-web-tripnova` — Spring Boot 4.0.3, **Java 21**, JPA, PostgreSQL `travel-web-project`, Spring Security 7 + JWT (oauth2-resource-server/Nimbus).
- **FE**: `D:\fe-tripnova` — React 19 CRA, react-router 7, axios, Tailwind 3.

Tham chiếu: `proposed_apis.txt` (PDF đã chuẩn hoá, dùng thay PDF), `db_schema_guide.txt` (59 bảng), `.cursorrules` (quy chuẩn BẮT BUỘC), FE `STRUCTURE.md`.

## 2. Đã hoàn thành (đều đã commit, test E2E bằng curl với DB thật)
| Nhóm | BE commit | FE commit |
|---|---|---|
| Hotel/Room GET + remainingRooms + X-Total-Count | `6eefd95` | `976f44f` (khởi tạo FE) |
| Auth JWT | `bc41828` | `0ce057d` |
| Hotel booking + payment (mock) | `340a434` | `38b096e` |
| /api/me (profile, dashboard, bookings, payments) | `dd6b562` | `f8124e7` |
| Lookup (continents/amenities/room-types), Favorites, Contact | `6b90f35` | `954f3d6` |
| Admin đợt 1 (dashboard, bookings, users, payments, contact messages) | `8e3db96` | `feb511f` |

### BE endpoints hiện có (tất cả có trailing slash)
- Public GET: `/api/countries/`, `/api/destinations/…`, `/api/landmarks/`, `/api/hotels/…`, `/api/hotel-bookings/check-availability/?hotelId&roomTypeId&roomId?&checkIn&checkOut`
- Auth (public POST): `/api/auth/register|login|logout|refresh-token|forgot-password|reset-password/`; `[AUTH]` `GET /api/auth/me/`, `POST /api/auth/change-password/`
- Booking `[AUTH]`: `POST /api/hotel-bookings/`, `GET /api/hotel-bookings/{id}/`, `POST /api/hotel-bookings/{id}/cancel/`
- Payment `[AUTH]`: `GET /api/payments/{id}/`, `POST /api/payments/{id}/mock-confirm/ {success}`
- Me `[AUTH]`: `GET|PUT /api/me/profile/`, `PUT /api/me/change-password/`, `GET /api/me/dashboard/`, `GET /api/me/bookings/?status=all|upcoming|completed|cancelled|pending&page&limit`, `GET /api/me/bookings/{id}/`, `POST /api/me/bookings/{id}/cancel/`, `GET /api/me/payments/?status=all|pending|success|failed|refunded&page&limit`, `GET /api/me/payments/summary/`, `GET /api/me/payments/{id}/`
- Lookup (public): `GET /api/continents/`, `/api/amenities/?category=`, `/api/room-types/?hotelId=`, `/api/hotels/{id}/room-types/`
- Favorites `[AUTH]`: `GET /api/favorites/?type=`, `POST /api/favorites/ {itemType,itemId}` (idempotent, hiện chỉ `hotel`), `DELETE /api/favorites/{id}/`, `GET /api/favorites/check/?itemType&itemId`
- Contact (public): `POST /api/contact/`, `GET /api/contact/info/` (lấy từ `app.contact.*`)
- Admin `[ADMIN]` (`api/AdminAPI.java`): `GET /api/admin/dashboard/`; `GET /api/admin/hotel-bookings/?status&q&page&limit`, `GET …/{id}/`, `PUT …/{id}/status/ {status,reason}`; `GET /api/admin/users/?q&role&status=active|locked`, `GET …/{id}/`, `PUT …/{id}/status/ {isActive}`, `PUT …/{id}/roles/ {roles:[…]}`; `GET /api/admin/payments/?status`; `GET /api/admin/contact-messages/?status`, `PUT …/{id}/status/`
- `POST/DELETE /api/countries/` và `/api/admin/**` yêu cầu ROLE_ADMIN.

### File BE chính
`config/SecurityConfig.java` (danh sách PUBLIC_GET/PUBLIC_POST), `config/WebCorsConfig.java` (bean CorsConfigurationSource), `config/RoleInitializer.java` (seed USER/ADMIN/HOTEL_MANAGER), `config/SchedulingConfig.java` (job huỷ booking quá hạn mỗi phút), `security/JwtService.java`, `security/JsonAuthErrorHandler.java`, `exception/ApiException.java`, `advice/ControllerAdvisor.java`, `util/SecurityUtil.java` (`getCurrentUserId()`), `util/TokenUtil.java`, `service/impl/{Auth,HotelBooking,Payment,User}ServiceImpl.java`, `repository/custom/impl/HotelBookingRepositoryImpl.java`, `api/{Auth,HotelBooking,Payment,Me}API.java`.

### File FE chính
`config/axiosConfig.js` (gắn Bearer, refresh 1 lần khi 401, lỗi có `.status/.details`), `utils/tokenStorage.js`, `context/AuthContext.js` (`useAuth()`: user, isLoggedIn, initializing, login, register, logout, refreshUser, hasRole), `routes/PrivateRoute.js` (`role="ADMIN"`), services: `auth|booking|payment|me|hotel|room|country|destination`, pages mới: `auth/ForgotPassword`, `auth/ResetPassword`, `client/Checkout`, `client/Payment`, `client/BookingDetail`; đã nối API thật: Login, Register, Profile, BookingHistory, PaymentHistory, AccountSettings, RoomDetail. Components chung: `FormInput, FormAlert, StatusBadge, Pagination, UserAvatar`, `client/BookingListItem`. `utils/formatters.js` (formatMoney/Date, isoDateFromToday, nightsBetween).

## 3. Đang dở
**Không có gì dở.** Admin đợt 1 đã xong cả BE + FE, cả 2 repo sạch (đã commit). Theo yêu cầu người dùng (2026-09-24): phiên đó chỉ làm nốt phần dở, **không bắt đầu tính năng mới** — phiên sau chờ người dùng chọn việc tiếp theo trong mục 4 trước khi làm.

FE Admin (`D:\fe-tripnova`): `routes/AdminRoutes.js` (lồng trong `components/layouts/AdminLayout.js`, đã bọc `PrivateRoute role="ADMIN"` ở `AppRouter`), pages `src/pages/admin/{Dashboard,Bookings,Users,Payments,Messages}`, `components/admin/RevenueChart.js` (cột doanh thu 30 ngày, 1 màu, tooltip hover + bảng số liệu), `services/adminService.js` (`BOOKING_NEXT_STATUSES` phải khớp `HotelBookingServiceImpl.updateStatusByAdmin`). Kiểm chứng: `npx react-scripts build` sạch; API admin đã test E2E bằng curl; **chưa bấm thử giao diện admin trên trình duyệt** (không có công cụ trình duyệt, dev server bị tắt vì thiếu RAM).

## 4. Bước tiếp theo (chưa làm — hỏi người dùng trước)
1. **Admin đợt 2 — CRUD khách sạn**: `GET/POST/PUT/DELETE /api/admin/hotels/` (xoá mềm `is_active=false`), room-types, rooms (+ FE form). Lưu ý bẫy ở mục 6: `hotels.total_rooms` do trigger, `check_in_time/check_out_time` là kiểu `time`, `cancellation_policy` ∈ free|partial|strict, `star_rating` 1–5, `destination_id` NOT NULL.
2. Cars / Flights / Tours: **chưa có entity** — tạo từ `db_schema_guide.txt` (kiểm tra schema thật bằng psql trước). Sau đó mở Favorites cho tour/car/flight (`FavoriteServiceImpl.SUPPORTED_TYPES`).
3. Cổng thanh toán thật (VNPay sandbox): `POST /api/payments/webhook/{provider}/` + `GET /api/payments/return/{provider}/` gọi `PaymentService.handleGatewayResult(...)` (đã có, idempotent). Tắt `app.payment.mock-enabled`.
4. OAuth2 Google/Facebook (đã bỏ nút Google ở Login/Register vì chưa có backend).
5. **Reviews: DB không có bảng `reviews`** → cần tạo bảng (ddl-auto=none, phải viết SQL migration) hoặc bỏ tính năng; hỏi người dùng trước khi tạo bảng.
6. Cải tiến nhỏ: `FavoriteButton` gọi `/check/` cho từng card (10 request/trang) → có thể gom bằng 1 lần `GET /api/favorites/`.

## 5. Quyết định kỹ thuật & lý do
- **Trailing slash** ở mọi endpoint (theo `.cursorrules`, FE cũng gọi có `/`).
- **Phân trang**: `page` (1-based) + `limit`, tổng qua header `X-Total-Count` (đã expose trong CORS). Không dùng `page=0&size=`.
- **Không Lombok**, getter/setter/constructor viết tay; constructor injection. Code Java viết tay (người dùng yêu cầu "làm bằng Java 21", không sinh code bằng script).
- **JWT HS256** qua `spring-boot-starter-oauth2-resource-server` (không dùng jjwt). Claim: `sub`=userId, `email`, `roles` → authority `ROLE_x`. Access 15 phút, refresh 7 ngày: chuỗi random, lưu **SHA-256** trong `refresh_tokens`, **xoay vòng** (`replaced_by`), dùng lại token cũ → thu hồi toàn bộ phiên của user.
- Refresh token trả trong body, FE lưu `localStorage` (đơn giản, chưa dùng httpOnly cookie).
- Forgot-password: chưa có mail server → **log link reset ra console** (`AuthServiceImpl`), luôn trả 204 để không lộ email.
- **Đặt phòng**: giai đoạn 1 giữ phòng 15 phút bằng booking `pending` (không ghi `room_availability`); giai đoạn 2 (thanh toán OK) khoá dòng `rooms` (`PESSIMISTIC_WRITE`), kiểm tra lại, upsert `room_availability` = `booked` từng ngày `[checkIn, checkOut)`. Phòng bị lấy mất → tự chuyển phòng khác cùng hạng, hết phòng → booking/order/payment `refunded`. Callback trùng → idempotent.
- Huỷ miễn phí nếu trước `checkIn + hotels.check_in_time − hotels.cancellation_hours` (mặc định 14:00 và 24h) → hoàn 100%, xoá dòng `booked`.
- Không tính thuế (tax=0); FE đã bỏ "8% tax" giả. Đơn vị tiền `app.booking.currency=USD` (giá trong DB 60–2000 nên không phải VND).
- Mock payment: `app.payment.mock-enabled=true` → trang FE `/payment/:id` có nút Pay / Simulate failed.

## 6. Bẫy đã gặp
- Cột status/gender là **Postgres ENUM**, `ip_address` là `inet`, `gateway_response` là `jsonb` nhưng entity map `String` → đã thêm `spring.datasource.hikari.data-source-properties.stringtype=unspecified`. **Hệ quả**: JPQL dạng `(:param IS NULL OR x = :param)` với tham số String sẽ lỗi `could not determine data type of parameter` → tách thành 2 query (xem `PaymentRepository.findByUser/findByUserAndStatus`). Tham số UUID thì không bị.
- `hotel_bookings.num_nights` là **generated column** → `@Column(insertable=false, updatable=false)`.
- `user_roles` **không có cột id**, PK = (user_id, role_id) → `@IdClass(UserRoleId)`.
- Nhiều cột `created_at/updated_at` NOT NULL nhưng Hibernate chèn null nếu không set → luôn set trong service.
- Entity cũ thiếu `@GeneratedValue` và thiếu cột (`orders.total_amount`, refund của hotel_bookings) → đã sửa; entity khác (cars, tours…) cần đối chiếu schema thật trước khi dùng. Kiểm tra: `select column_name, udt_name, is_nullable, column_default, is_generated from information_schema.columns where table_name='...'`.
- Spring Boot 4 dùng **Jackson 3** (`tools.jackson.databind.ObjectMapper`).
- Devtools tự restart khi `mvnw compile` chạy lúc app đang chạy → đôi khi lỗi `ClassNotFoundException` giữa chừng; muốn chắc thì kill hẳn process :8080 rồi chạy lại. `taskkill` xong phải **chờ cổng 8080 được giải phóng** mới start lại.
- Git Bash trên Windows: `curl -d` với tiếng Việt bị lỗi encoding (400) → test bằng ASCII; `-w "/path"` bị MSYS đổi path; Python Windows không thấy `/tmp`.
- CRA dev server chỉ trả `index.html` cho request có `Accept: text/html`.
- `pdftotext` làm vỡ tiếng Việt của `1.pdf`.
- `application-uat.properties` (user/pass DB) bị gitignore — không commit, không in ra.
- Commit: **không thêm dòng Co-Authored-By Claude** (người dùng yêu cầu).
- Tạo admin đầu tiên: đăng ký tài khoản, rồi chạy app với biến môi trường `ADMIN_EMAIL=<email>` (`app.admin.bootstrap-email`, xử lý trong `config/RoleInitializer.java`) — hoặc SQL: `insert into user_roles(user_id, role_id) select u.id, r.id from users u, roles r where u.email='<email>' and r.name='ADMIN';`. Sau đó **đăng nhập lại** (roles nằm trong JWT). Đổi role/khoá user cũng thu hồi refresh token → user phải đăng nhập lại.
- Spring Boot đọc `application.properties` theo **ISO-8859-1**: giá trị tiếng Việt có dấu sẽ lỗi font → dùng ASCII hoặc `\uXXXX`. Công cụ Edit tự đổi `\uXXXX` thành ký tự thật, nên đã để `app.contact.address=Ha Noi, Viet Nam`. Comment tiếng Việt thì không sao.
- CRA/ESLint đôi khi dùng **cache cũ** (báo lỗi ở số dòng không khớp file hiện tại) → `rm -rf node_modules/.cache` rồi build lại.
- Không có bảng `reviews` trong DB (dù `proposed_apis.txt` có nhắc).
- Máy thiếu RAM: dev server FE chạy nền từng bị hệ thống tắt. Chạy FE + BE cùng lúc có thể không ổn định; kiểm tra FE bằng `npx react-scripts build` là đủ.

## 7. Lệnh chạy / test
```bash
# BE (Postgres 16 local :5432, psql tại "C:/Program Files/PostgreSQL/16/bin/psql")
cd D:/travel-web-tripnova
./mvnw -q -o compile          # build nhanh
./mvnw spring-boot:run        # :8080 (lần đầu sau khi thêm dependency: bỏ -o)

# Smoke test
curl -i "http://localhost:8080/api/hotels/?page=1&limit=5"
curl -s -H 'Content-Type: application/json' -d '{"email":"a@b.local","password":"secret123","fullName":"A"}' http://localhost:8080/api/auth/register/
curl -s -H "Authorization: Bearer <accessToken>" http://localhost:8080/api/me/dashboard/

# FE
cd D:/fe-tripnova
npm start                     # :3000, BE qua REACT_APP_API_BASE_URL (.env.development)
npx react-scripts build       # kiểm tra compile + ESLint (cảnh báo source map @mediapipe là có sẵn, bỏ qua)
```
Script test E2E của phiên trước nằm trong scratchpad (không còn ở phiên mới): tạo user `claude.test+…@tripnova.local`, đặt/thanh toán/huỷ, rồi xoá sạch dữ liệu test theo email đó.
