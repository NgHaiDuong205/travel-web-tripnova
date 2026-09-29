# HANDOFF — TripNova (BE + FE)

Cập nhật: 2026-09-29. Người dùng yêu cầu **làm lần lượt cả FE + BE theo checklist mục 4, không cần hỏi ý kiến**.

## 1. Mục tiêu tổng thể
Làm hết các API trong spec `C:\Users\HPC\Desktop\1.pdf` cho:
- **BE**: `D:\travel-web-tripnova` — Spring Boot 4.0.3, **Java 21**, JPA, PostgreSQL `travel-web-project`, Spring Security 7 + JWT (oauth2-resource-server/Nimbus).
- **FE**: `D:\fe-tripnova` — React 19 CRA, react-router 7, axios, Tailwind 3.

Tham chiếu: `proposed_apis.txt` (PDF đã chuẩn hoá, dùng thay PDF), `db_schema_guide.txt` (59 bảng), `.cursorrules` (quy chuẩn BẮT BUỘC — file đã bị xoá khỏi working tree nhưng vẫn còn trong git: `git show HEAD:.cursorrules`), `Claude.md` (phân công: Claude thiết kế/logic khó + review; **Codex MCP** làm boilerplate như entity/DTO — luôn review lại), FE `STRUCTURE.md`.

## 2. Đã hoàn thành (đều đã commit, test E2E bằng curl với DB thật)
| Nhóm | BE commit | FE commit |
|---|---|---|
| Hotel/Room GET + remainingRooms + X-Total-Count | `6eefd95` | `976f44f` (khởi tạo FE) |
| Auth JWT | `bc41828` | `0ce057d` |
| Hotel booking + payment (mock) | `340a434` | `38b096e` |
| /api/me (profile, dashboard, bookings, payments) | `dd6b562` | `f8124e7` |
| Lookup (continents/amenities/room-types), Favorites, Contact | `6b90f35` | `954f3d6` |
| Admin đợt 1 (dashboard, bookings, users, payments, contact messages) | `8e3db96` | `feb511f` |
| Admin A2: hotels / room types / rooms / lịch khoá phòng (+ fix destinations, landmarks 500) | `de0aaa5`, `24e418e` | `7f7e7a4` |
| Admin A12: amenities | `04c0107` | `ab6890a` |
| Admin A7: continents / countries / destinations / landmarks | `d8d97c7` | `2f708ce` |
| Admin A3/A9/A10: hoàn tiền + xoá booking, payment chi tiết/đổi trạng thái/hoàn tiền, contact chi tiết/xoá/trả lời | `5b98c8a` | `83eabe8` |
| Admin A8: sửa/xoá mềm user, roles CRUD, permissions (seed danh mục) | `20e5742` | `3306e88` |
| Admin A1: dashboard statistics/revenue/recent/top | `609a94e` | `9a4ed08` |
| Chuẩn hoá API public cũ (404, tên param, destinations/landmarks q + phân trang) | `cfa792e` | — |
| Itineraries | `fb937e0` | (xem git log FE) |
| Cart + order nhiều booking | `2a8027f` + bookingCount | `dee65b9` |
| Posts/Reviews + Comments + admin kiểm duyệt | `db01499` | `38f37a5` |
| Invoices (hoá đơn + PDF) + admin | (xem git log, 2026-09-29) | `f9ad5d9` |
| Refactor order đa loại sản phẩm (`OrderBookingHandler`) | `b6977f8` | — |
| Cars (thuê xe) + admin | `a2fe123`, `45b8493` | `05545e9` |

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
- Admin (mới 2026-09-29): `GET /api/admin/dashboard/statistics/?from&to&granularity=day|week|month`, `…/revenue/?from&to&groupBy`, `…/recent-bookings/?limit`, `…/recent-payments/?limit`, `…/top-hotels/?from&to&limit`, `…/top-destinations/…` (from/to mặc định 30 ngày gần nhất, tính cả 2 đầu); `POST /api/admin/hotel-bookings/{id}/refund/ {amount?,reason}`, `DELETE …/{id}/`; `GET /api/admin/payments/{id}/`, `PUT …/{id}/status/ {status,reason}`, `POST …/{id}/refund/ {reason}`; `GET|DELETE /api/admin/contact-messages/{id}/`, `POST …/{id}/reply/ {content}`; `PUT|DELETE /api/admin/users/{id}/`; `api/AdminRoleAPI.java`: `GET|POST /api/admin/roles/`, `PUT|DELETE …/{id}/`, `PUT …/{id}/permissions/ {permissionIds}`, `GET /api/admin/permissions/`
- `POST/DELETE /api/countries/` và `/api/admin/**` yêu cầu ROLE_ADMIN.

### File BE chính
`config/SecurityConfig.java` (danh sách PUBLIC_GET/PUBLIC_POST), `config/WebCorsConfig.java` (bean CorsConfigurationSource), `config/RoleInitializer.java` (seed USER/ADMIN/HOTEL_MANAGER), `config/SchedulingConfig.java` (job huỷ booking quá hạn mỗi phút), `security/JwtService.java`, `security/JsonAuthErrorHandler.java`, `exception/ApiException.java`, `advice/ControllerAdvisor.java`, `util/SecurityUtil.java` (`getCurrentUserId()`), `util/TokenUtil.java`, `service/impl/{Auth,HotelBooking,Payment,User}ServiceImpl.java`, `repository/custom/impl/HotelBookingRepositoryImpl.java`, `api/{Auth,HotelBooking,Payment,Me}API.java`.

### File FE chính
`config/axiosConfig.js` (gắn Bearer, refresh 1 lần khi 401, lỗi có `.status/.details`), `utils/tokenStorage.js`, `context/AuthContext.js` (`useAuth()`: user, isLoggedIn, initializing, login, register, logout, refreshUser, hasRole), `routes/PrivateRoute.js` (`role="ADMIN"`), services: `auth|booking|payment|me|hotel|room|country|destination`, pages mới: `auth/ForgotPassword`, `auth/ResetPassword`, `client/Checkout`, `client/Payment`, `client/BookingDetail`; đã nối API thật: Login, Register, Profile, BookingHistory, PaymentHistory, AccountSettings, RoomDetail. Components chung: `FormInput, FormAlert, StatusBadge, Pagination, UserAvatar`, `client/BookingListItem`. `utils/formatters.js` (formatMoney/Date, isoDateFromToday, nightsBetween).

## 3. Đang dở
Người dùng (2026-09-24): **làm lần lượt các API còn thiếu trong checklist mục 4, không cần hỏi**; làm xong mục nào thì đánh `[x]` vào checklist.
- A2 xong cả BE + FE (FE: `pages/admin/{Hotels,HotelEdit}`, `components/admin/{HotelForm,RoomTypeManager,RoomManager,RoomAvailabilityPanel}.js`, `services/adminHotelService.js`; build sạch, chưa bấm thử trên trình duyệt).
- A12 xong (BE `api/AdminAmenityAPI.java`, FE `pages/admin/Amenities`).
- A7 xong (BE `api/AdminGeographyAPI.java`; FE `pages/admin/Geography/*`). POST/DELETE `/api/countries/` cũ đã bỏ, dùng `/api/admin/countries/`.
- 2026-09-29: xong A1, A3, A8, A10 và A9 (trừ invoices) cả BE + FE (FE mới: `pages/admin/Roles`; Dashboard có chọn kỳ 7d/30d/90d/12m). Build FE sạch, **chưa bấm thử trên trình duyệt**.
- 2026-09-29 (phiên sau): xong Posts/Comments/Reviews BE + FE (E2E curl/Python OK, FE build sạch, chưa bấm thử trình duyệt). Đã `git push` BE lên GitHub (trước đó 28 commit chỉ nằm local). **FE `D:\fe-tripnova` chưa có remote GitHub** — chờ người dùng cho URL repo.
- Xong Invoices BE + FE (E2E Python OK: phát hành khi thanh toán, idempotent, PDF, hoàn tiền, admin lọc/gửi lại; FE build sạch).
- **Đã chạy thử UI thật** (2026-09-29) cho Posts/Reviews + Invoices: BE :8080 + FE `npm start` :3000, điều khiển Edge headless bằng Playwright (`npm i playwright-core` trong scratchpad, `chromium.launch({ channel: "msedge" })` — dùng Edge có sẵn, không tải browser; người dùng KHÔNG dùng extension Claude in Chrome). Luồng đã thử: viết review → My Reviews (pending) → admin duyệt → user khác like/bình luận/trả lời → tổng điểm cập nhật → xoá review; Invoices xem chi tiết + tải PDF; admin lọc/xem/gửi lại hoá đơn. Không lỗi console, không 5xx.
  Người dùng yêu cầu: **xong mỗi module phải chạy thử cả BE lẫn FE** như trên.
  Lỗi có sẵn (chưa sửa): trang khách sạn hiển thị giá phòng bằng "€" trong khi hệ thống dùng USD; thanh "Search Rooms" cố định dưới đáy trang che một phần khối review.
- **Kiến trúc order đa sản phẩm** (2026-09-29): mỗi order chỉ chứa 1 loại booking. `OrderBookingHandler` (hotel, car, … — implement bởi `HotelBookingServiceImpl`, `CarBookingServiceImpl`) lo confirm khi thanh toán, huỷ pending, hết hạn giữ chỗ, admin hoàn tiền theo giao dịch, dòng hoá đơn, tổng đã hoàn. `OrderBookingRouter` chọn handler theo order; `OrderFactory` tạo order+payment. PaymentService / InvoiceService / SchedulingConfig chỉ gọi qua router → thêm loại mới (flight, tour) chỉ cần implement handler. `PaymentDTO.bookingType` + `primaryBookingId`; `bookingId` chỉ điền với hotel (trang Payment cũ chuyển tới booking khách sạn).
- **FE mới theo thiết kế Stitch** (file HTML ở `D:\travel-web-tripnova\FE_HTML`, KHÔNG commit — người dùng tạo): CAR, CAR_DETAIL, FLIGHT, FLIGHT_DETAIL, TOUR, TOUR_DETAIL. Người dùng yêu cầu: trang cũ KHÔNG được sửa (chỉ thêm route/menu); các trang mới đồng bộ nhau → dùng chung `src/components/catalog/*` (CatalogHero, SearchPanel, SearchField, FilterSidebar, ListHeader, SpecTile, SpecChip, BookingSummaryCard, DetailBackLink, InfoRow). Trang thanh toán chung cho car/flight/tour: `/trip-payment/:paymentId` (trang `/payment` cũ chỉ điều hướng booking khách sạn). `/my-trips` liệt kê đơn ngoài khách sạn. Project không có `@tailwindcss/forms` → input phải có `border` + padding rõ ràng.
- Dữ liệu demo: 6 xe `DEMO-01..06` (ảnh Unsplash, gắn điểm đến bất kỳ — ảnh chưa đúng mẫu xe). Xe test `Claude Test Car/Van` (đã tắt).
- UI test Playwright + Edge headless: `scratchpad/ui/ui_cars.js` (danh sách, lọc, sort, tìm theo ngày, chi tiết, bắt đăng nhập, đặt → thanh toán → My Trips → huỷ, hoá đơn, admin) — pass.
- Tiếp theo: Flights (ghế) → Tours → các mục còn lại (trừ AI và cổng thanh toán thật — người dùng loại ra) → … (xem "Thứ tự dự kiến" cuối mục 4).
- **Dữ liệu test chưa dọn** (lệnh xoá SQL bị auto mode chặn): user `claude.test+a2@tripnova.local` (ADMIN, mật khẩu `secret123`; + order/payment/booking — 3 booking đã hoàn tiền trên `Claude Test Hotel B`), user `claude.test+a8@tripnova.local` (đã xoá mềm), khách sạn `Claude Test Hotel%` (đều `is_active=false`), điểm đến `Claude Test City%`, quốc gia `ZZY`, châu lục `ZZ`.

FE Admin (`D:\fe-tripnova`): `routes/AdminRoutes.js` (lồng trong `components/layouts/AdminLayout.js`, đã bọc `PrivateRoute role="ADMIN"` ở `AppRouter`), pages `src/pages/admin/{Dashboard,Bookings,Users,Payments,Messages}`, `components/admin/RevenueChart.js` (cột doanh thu 30 ngày, 1 màu, tooltip hover + bảng số liệu), `services/adminService.js` (`BOOKING_NEXT_STATUSES` phải khớp `HotelBookingServiceImpl.updateStatusByAdmin`). **Chưa bấm thử giao diện admin trên trình duyệt** (dev server bị tắt vì thiếu RAM).

## 4. Checklist API theo `proposed_apis.txt`
Quy ước khi code (khác spec, đã chốt): mọi path **có trailing slash**, phân trang `page` (1-based) + `limit`, tổng qua `X-Total-Count`.
Ký hiệu: `[x]` xong (BE, đã test curl) · `[~]` làm một phần / thay bằng endpoint tương đương · `[ ]` chưa làm · `(no table)` DB chưa có bảng → cần viết SQL migration, **hỏi người dùng trước khi tạo bảng**.

### I. Public / Client
**1. Auth & OAuth2**
- [x] POST /api/auth/register · login · logout · refresh-token · forgot-password · reset-password · change-password
- [x] GET /api/auth/me
- [ ] POST /api/auth/verify-email · resend-verification (chưa có mail server → log link ra console như forgot-password)
- [ ] OAuth2 Google / Facebook / GitHub (`/oauth2/authorization/*`, bảng `user_oauth_accounts`)

**2. Users / Me**
- [x] GET /api/me/dashboard · GET|PUT /api/me/profile · PUT /api/me/change-password
- [ ] PUT /api/me/avatar (multipart — cần hạ tầng upload, xem mục 19)
- [ ] /api/me/notifications (GET, PATCH {id}/read, POST mark-all-read) (no table `notifications`)
- [ ] GET /api/me/oauth-accounts · DELETE /api/me/oauth-accounts/{provider}

**3. Account settings**
- [~] GET /api/account/settings · PUT /api/account/profile · PUT /api/account/password → đã phủ bởi /api/me/profile, /api/me/change-password (FE AccountSettings dùng /api/me)
- [ ] PUT /api/account/email · DELETE /api/account (soft-delete)

**4. Lookup**
- [x] GET /api/continents · /api/countries · /api/countries/{id} · /api/amenities?category · /api/room-types?hotelId
- [~] GET /api/currencies → bỏ qua: hệ thống chỉ dùng 1 loại tiền (`app.booking.currency`)

**5. Destinations & Landmarks**
- [x] GET /api/destinations (?q&countryCode&continentCode&isPopular&page&limit — không truyền limit thì trả hết; luôn có X-Total-Count) · /{id} · /{id}/landmarks · /{id}/landmarks/{landmarkId} · GET /api/landmarks (?q&category&page&limit)
- [~] GET /api/landmarks/{landmarkId} → dùng /api/destinations/{id}/landmarks/{landmarkId}
- [~] GET /api/destinations/{id}/hotels → dùng /api/hotels?destinationId=
- [ ] GET /api/destinations/{id}/tours

**6. Hotels & Rooms**
- [x] GET /api/hotels · /{hotelId} · /{hotelId}/rooms · /{hotelId}/rooms/{roomId} · /{hotelId}/room-types
- [~] GET /api/hotels/{hotelId}/availability?from&to → dùng /api/hotel-bookings/check-availability + /api/hotels/{id}/rooms?checkIn&checkOut
- [~] GET /api/hotels/suggest?q → dùng /api/hotels?name=&limit=
- [~] GET|POST /api/hotels/{hotelId}/reviews → dùng /api/posts/?entityType=hotel&entityId= và POST /api/posts/ (bảng `posts` chính là review, xem mục 16)

**7. Hotel bookings**
- [x] POST /api/hotel-bookings · GET /{id} · POST /{id}/cancel · GET /check-availability

**8. Cars & Car bookings** — BE + FE xong (`CarAPI`, `CarServiceImpl`, `CarBookingServiceImpl`)
- [x] GET /api/cars (?q&destinationId&type&brands=a,b&seats&transmission&fuelType&withDriver&priceMin&priceMax&pickupDate&returnDate (ISO, chỉ xe còn trống)&sort&page&limit) · /filters · /{carId} · /{carId}/availability?from&to
- [x] POST /api/car-bookings {carId, pickupDate, returnDate, pickupLocation?, returnLocation?, driverLicenseNo (bắt buộc nếu xe tự lái), specialRequests, paymentMethod} → CarBookingDTO (có paymentId) · GET /{id} · POST /{id}/cancel · GET /api/me/car-bookings?status=all|upcoming|pending|completed|cancelled
  Quy tắc: 1 xe = 1 chiếc; khoá dòng `cars` (PESSIMISTIC_WRITE) khi đặt/xác nhận; chặn giao nhau với booking confirmed/checked_in hoặc pending còn hạn giữ. Giá = price_per_day × số ngày làm tròn lên theo 24h (tối thiểu 1, tối đa 30). Nhận xe phải sau hiện tại ≥ 1h. Huỷ miễn phí tới 48h trước giờ nhận (theo thiết kế) → hoàn 100%. `car_bookings` KHÔNG có cột refund → chỉ hoàn toàn bộ, status `refunded`, lý do lưu `orders.cancel_reason`.

**9. Flights & Flight bookings** (chưa có entity)
- [ ] GET /api/flights · /{flightId} · /{flightId}/seats
- [ ] POST /api/flight-bookings · POST /{id}/select-seat · GET /{id} · POST /{id}/cancel
- [ ] GET /api/airports?q (no table — có thể lấy distinct từ `flights`)

**10. Tours** (chưa có entity)
- [ ] GET /api/tours · /{tourId} · /{tourId}/hotels · /cars · /flights · /itinerary
- [ ] GET /api/tours/{tourId}/reviews (no table)
- [ ] POST /api/tour-bookings · GET /{id} · POST /{id}/cancel

**11. Itineraries** (bảng `itineraries`, `itinerary_items`)
- [x] GET|POST /api/me/itineraries · GET|PUT|DELETE /{id} · POST /{id}/items · DELETE /{id}/items/{itemId} — BE `api/ItineraryAPI.java` + FE `pages/client/{Itineraries,ItineraryDetail}` (menu "My Itineraries"). Tối đa 60 ngày, dayNumber phải trong khoảng ngày; item hotel/landmark/destination kiểm tra entityId tồn tại và tự lấy tên; rút ngắn chuyến mà còn hoạt động ở ngày bị cắt → 400. Chưa có sửa item (xoá + thêm lại).

**12. Cart & Favorites**
- [x] GET /api/cart · POST|PUT|DELETE /api/cart/items[/{itemId}] · DELETE /api/cart/clear · POST /api/cart/checkout {paymentMethod, itemIds?} — BE `api/CartAPI.java`, `CartServiceImpl`; FE `pages/client/Cart` + nút "Add to cart" ở Checkout + icon giỏ ở Header. Chỉ hotel; mỗi dòng = 1 loại phòng + ngày, `quantity` = số phòng (≤5), adults/children là TỔNG chia đều mỗi phòng; thêm trùng thì cộng dồn; GET tính lại giá/tình trạng (`priceChanged`, `issue`); checkout nguyên tử → 1 order + N booking + 1 payment (`HotelBookingService.createOrder`). Chưa hỗ trợ guest cart (session_token).
- [x] GET|POST /api/favorites · DELETE /{id} · GET /check (hiện chỉ `hotel`; mở tour/car/flight khi có entity)

**13. Payments**
- [~] POST /api/payments/create → payment được tạo cùng booking (POST /api/hotel-bookings)
- [x] GET /api/payments/{paymentId} (+ POST /{id}/mock-confirm khi `app.payment.mock-enabled`)
- [ ] POST /api/payments/webhook/{provider} · GET /api/payments/return/{provider} (VNPay sandbox; gọi `PaymentService.handleGatewayResult`)
- [ ] POST /api/payments/{paymentId}/refund [ADMIN]

**14. Booking history & Invoices**
- [x] GET /api/me/bookings · /{id} · POST /{id}/cancel · GET /api/me/payments · /{id} (+ /summary) — hiện chỉ booking khách sạn
- [x] GET /api/me/invoices (?page&limit) · /{invoiceId} (kèm items = các booking của order) · /{invoiceId}/download (PDF) — BE `api/InvoiceAPI.java`, `InvoiceServiceImpl`, `InvoicePdfRenderer`; FE trang `/invoices` (menu "Invoices").
  Quy tắc: 1 order = 1 hoá đơn (UNIQUE order_id), số `INV-<orderCode>`; phát hành trong transaction thanh toán (`PaymentServiceImpl.handleGatewayResult`) khi `confirmOrder` giữ được ≥1 phòng; gọi lặp lại không tạo thêm. `InvoiceBackfillRunner` lúc khởi động bổ sung hoá đơn cho order `paid|partially_refunded` chưa có (order `refunded` cũ bị bỏ qua vì không phân biệt được "hết phòng ngay khi thanh toán"). Số tiền lấy từ order; hoàn tiền không sửa hoá đơn mà hiện `refundedAmount` (tổng `hotel_bookings.refund_amount`) + dòng "Net amount" trong PDF. `billing_address/tax_code` chưa có API để user nhập.
  PDF: OpenPDF 3.0.5 (package `org.openpdf.text`), nhúng font `resources/fonts/DejaVuSans*.ttf` để có dấu tiếng Việt. CORS đã expose `Content-Disposition` (FE `utils/download.js` lấy tên file).

**15. Reviews** — [~] phủ bởi Posts (mục 16): GET /api/reviews → /api/posts/?entityType&entityId; POST/PUT/DELETE → /api/posts/…; helpful → POST /api/posts/{id}/reactions/ {type:like}. Không có bảng `reviews`/`review_ai_analysis` chưa dùng.

**16. Posts / Comments** (bảng `posts`, `post_reactions`, `comments`)
- [x] GET|POST /api/posts (?entityType&entityId&userId&q&rating&sort=newest|oldest|top|rating_high|rating_low&page&limit — public chỉ `approved`) · GET|PUT|DELETE /{id} · POST|DELETE /{id}/reactions {type: like|dislike} (trả PostDTO) · GET|POST /{id}/comments (danh sách phẳng, FE dựng cây theo parentId) · PUT|DELETE /api/comments/{id} — BE `api/PostAPI.java`, `PostServiceImpl`, `CommentServiceImpl`.
  Thêm: `GET /api/posts/rating-summary/?entityType&entityId` (điểm TB + phân bố), `GET /api/me/posts/?status` (bài của mình mọi trạng thái).
  Quy tắc: `posts` là review gắn hotel|landmark|destination (DB bắt buộc entity_type/entity_id). Bài mới/vừa sửa → `pending` (admin viết → `approved`); không đổi đối tượng khi sửa. `isVerifiedBooking` = có booking khách sạn đó đã checked_out/completed (hoặc confirmed/checked_in đã qua ngày trả phòng). Không tự vote bài mình; reaction upsert (ON CONFLICT) rồi **đếm lại** upvotes/downvotes. Bình luận chỉ ở bài approved; user xoá = `deleted` (xoá mềm), admin ẩn = `flagged`; bình luận không active bị che nội dung và bị lược bỏ nếu không còn trả lời hiển thị bên dưới.
  FE: `components/client/{ReviewSection,PostCard,PostForm,CommentThread}.js`, `common/StarRating.js`; HotelDetail thay "Guest Chronicles" giả bằng review thật; trang `/my-reviews` (menu "My Reviews"); admin `/admin/posts` (menu "Reviews"). **DestinationDetail FE vẫn là dữ liệu tĩnh** (id không phải UUID) nên chưa gắn review.

**17. Contact / About**
- [x] POST /api/contact · GET /api/contact/info
- [~] GET /api/about … → bỏ qua: FE đã có trang About tĩnh (Terms/Privacy nếu cần thì làm trang tĩnh ở FE)

**18. Search & AI** — [ ] /api/search, /api/search/suggest, /api/ai/* (bảng `search_queries`, `chat_*`, `recommendations`, `price_predictions`…)

**19. Uploads** — [ ] POST|DELETE /api/uploads/images

### II. Admin (`/api/admin/**`, ROLE_ADMIN)
**A1. Dashboard** — BE + FE xong
- [x] GET /api/admin/dashboard (đã gồm tổng số, booking theo trạng thái, doanh thu 30 ngày, booking gần đây)
- [x] GET /dashboard/statistics (kèm so với kỳ trước + chuỗi theo granularity) · /revenue?from&to&groupBy=day|week|month · /recent-bookings · /recent-payments · /top-hotels · /top-destinations

**A2. Hotels & Rooms** — BE + FE xong (`AdminHotelAPI`)
- [x] GET|POST /api/admin/hotels · GET|PUT|DELETE /{hotelId} (xoá mềm)
- [x] GET|POST /api/admin/hotels/{hotelId}/room-types · PUT|DELETE /{roomTypeId}
- [x] GET|POST /api/admin/hotels/{hotelId}/rooms · GET|PUT|DELETE /{roomId}
- [x] PUT /api/admin/hotels/{hotelId}/amenities {amenityIds}
- [x] PUT /api/admin/rooms/{roomId}/availability {from,to (tính cả 2 đầu),status=blocked|available,reason} + GET ?from&to (chỉ trả ngày booked/blocked)
- [ ] POST|DELETE /api/admin/hotels/{hotelId}/images (no table `hotel_images` — hiện chỉ có `cover_image_url`)

**A3. Hotel bookings**
- [x] GET /api/admin/hotel-bookings · /{id} · PUT /{id}/status
- [x] DELETE /{id} (chỉ booking `cancelled` chưa từng thanh toán; xoá kèm order/payment nếu order không còn booking và chưa có hoá đơn) · POST /{id}/refund (hoàn một phần/cộng dồn; hoàn đủ → `refunded` + trả phòng)

**A4. Cars** — [x] GET|POST /api/admin/cars (?q&type&active&destinationId) · GET|PUT|DELETE /{id} (xoá mềm, biển số unique) · GET /api/admin/car-bookings (?status&q) · GET /{id} · PUT /{id}/status (pending→cancelled, confirmed→cancelled(hoàn 100%)|checked_in|no_show, checked_in→checked_out→completed). FE `/admin/cars` (tab Fleet / Rentals).
**A5. Flights & Seats** — [ ] toàn bộ · **A6. Tours** — [ ] toàn bộ

**A7. Geography** — BE + FE xong (`AdminGeographyAPI`)
- [x] GET|POST /api/admin/continents · PUT|DELETE /{id} (409 nếu còn quốc gia)
- [x] GET|POST /api/admin/countries (?q&continentId) · PUT|DELETE /{id} (409 nếu còn điểm đến; slug tự sinh)
- [x] GET|POST /api/admin/destinations (phân trang) · GET|PUT|DELETE /{id} (xoá mềm)
- [x] GET|POST /api/admin/landmarks (phân trang, ?category) · PUT|DELETE /{id} (xoá mềm)

**A8. Users / Roles**
- [x] GET /api/admin/users · /{id} · PUT /{id}/status · PUT /{id}/roles
- [x] PUT /{id} (body như /api/me/profile, ghi đè toàn bộ) · DELETE /{id} (xoá mềm, 409 nếu còn booking mở, email vẫn bị chiếm)
- [x] /api/admin/roles CRUD · GET /api/admin/permissions · PUT /roles/{id}/permissions

**A9. Payments & Invoices**
- [x] GET /api/admin/payments?status
- [x] GET /{id} · PUT /{id}/status (pending→success|failed, success→refunded) · POST /{id}/refund
- [x] GET /api/admin/invoices (?q=số HĐ/mã order/email/tên&from&to=ngày phát hành, tính cả 2 đầu&page&limit) · GET /{id} · GET /{id}/download · POST /{id}/resend (log ra console như mail khác) — FE `/admin/invoices`

**A10. Contact messages**
- [x] GET /api/admin/contact-messages · PUT /{id}/status
- [x] GET /{id} · DELETE /{id} (xoá hẳn) · POST /{id}/reply (lưu reply_content/replied_by/at, status → resolved, mail log ra console; không trả lời tin spam)

**A11. Reviews / Posts moderation** — BE + FE xong (`AdminPostAPI`)
- [x] GET /api/admin/posts (?status&entityType&entityId&userId&q&sort&page&limit) · GET /{id} · PUT /{id}/status {pending|approved|rejected} · DELETE /{id} (xoá hẳn, cascade) · GET /{id}/comments (kể cả đã ẩn/xoá)
- [x] GET /api/admin/comments (?status&postId&q) · PUT /{id}/status {active|flagged|deleted} · DELETE /{id} (xoá hẳn kèm trả lời)
- [~] /api/admin/reviews → chính là /api/admin/posts
**A12. Amenities** — [x] GET?category (kèm hotelCount/roomTypeCount) · POST · PUT · DELETE (409 nếu đang dùng, `?force=true` để xoá)
**A13. AI / Knowledge base** — [ ] toàn bộ
**A14. Audit / Logs** — [ ] /api/admin/audit-logs (no table) · [ ] /api/admin/search-queries

### Thứ tự dự kiến
~~A2 → A12 → A7 → A1/A3/A8/A9/A10~~ (xong) → ~~shortcuts/About/Currencies~~ (bỏ qua, xem `[~]`) → ~~Itineraries~~ → ~~Cart~~ → ~~Posts/Comments~~ → ~~Invoices~~ (xong) → Cars/Flights/Tours (+ admin) → Payment gateway → OAuth2 → Uploads/Avatar → Search/AI. Mục `(no table)` để cuối và hỏi người dùng.

Quy tắc (người dùng 2026-09-29): endpoint mà endpoint cũ đã đáp ứng thì **không làm lại** (đánh `[~]`); code cũ lệch quy chuẩn thì **được sửa**. Việc khó Claude tự làm, việc lặt vặt giao Codex.
Đã chuẩn hoá API public cũ: service ném `ApiException.notFound` thay vì trả null (Destination/Landmark/Hotel/Room/Country), controller ghi rõ tên `@PathVariable/@RequestParam`.

Cải tiến nhỏ tồn đọng: `FavoriteButton` gọi `/check/` cho từng card (10 request/trang) → gom bằng 1 lần `GET /api/favorites/`.

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
- **Hoàn tiền admin**: cộng dồn vào `hotel_bookings.refund_amount` (không vượt `total_price`); booking chỉ chuyển `refunded` khi hoàn đủ (hoàn một phần = bồi thường, giữ trạng thái). Order: hoàn đủ tất cả booking → `refunded` (+ payment `refunded`), ngược lại `partially_refunded` (payment giữ `success`). Chỉ áp dụng cho booking `confirmed|no_show|checked_out|completed` của order `paid|partially_refunded`; `checked_in` phải check-out trước.
- **Order nhiều booking** (từ giỏ hàng): thanh toán xong mà có phòng bị lấy mất → chỉ hoàn booking đó, order `partially_refunded`, payment giữ `success` (`confirmOrder` trả false chỉ khi không giữ được phòng nào). Khách huỷ 1 booking đã thanh toán → `syncOrderRefundStatus` (hoàn đủ mọi booking mới `refunded`). Huỷ 1 booking **pending** = huỷ cả order (chung 1 payment). `PaymentDTO.bookingCount` > 1 → FE chuyển về Booking History.
- **Doanh thu dashboard** = tổng payment `success` theo `paid_at` (payment hoàn đủ bị loại, hoàn một phần KHÔNG bị trừ — xem `refundedAmount` riêng). Top hotels/destinations dùng doanh thu ròng `total_price - refund_amount` của booking tạo trong kỳ.
- **Permissions** chỉ là danh mục (seed 8 mã trong `RoleInitializer`, quyền mới tự gán cho ADMIN); phân quyền API vẫn theo role. Role hệ thống USER/ADMIN/HOTEL_MANAGER không đổi tên/xoá được.
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
- `hotels.total_rooms` do trigger `trg_rooms_sync_total` → entity đã để `insertable=false, updatable=false` (nếu không Hibernate sẽ ghi đè số cũ). `hotels.updated_at` cũng có trigger.
- `HotelDTO` (public) ẩn nhiều trường bằng `@JsonIgnore` (phone, isActive, destinationId…) → admin dùng `AdminHotelDTO` + `AdminHotelDTOConverter` (map tay, không ModelMapper vì Hotel/Destination trùng tên trường như `latitude`).
- `HotelAmenityEntity` (map sai: bảng `hotel_amenities` không có cột `id`) đã **xoá**; `HotelEntity.hotelAmenities` là `@ManyToMany` trực tiếp. `room_type_amenities` (entity `@IdClass`) ghi bằng native insert `RoomTypeAmenityRepository.insertLink` — `saveAll/merge` sinh SQL lỗi.
- Một số file `.java` dùng CRLF (vd `AmenityEntity.java`) → sửa bằng perl/sed với `\n` sẽ **không khớp**; dùng công cụ Edit.
- Jackson 3: `JsonNode.asString(default)` của NullNode trả `""` chứ không trả default → dùng `hasNonNull` (xem `CartServiceImpl.textOrNull`).
- Python `subprocess` gọi `bash` trên Windows ra **WSL bash**; Git Bash nằm ở `E:\Programs\Git\Git\usr\bin\bash.exe`.
- Admin không chặn được ngày phòng đang có booking giữ chỗ (409) → muốn giả lập "phòng bị lấy mất" trong test thì insert `room_availability` trực tiếp.
- Bảng `posts` có trigger `set_updated_at` → mọi UPDATE (kể cả đồng bộ upvotes) đều đổi `updated_at`, nên không dùng `updated_at` để suy ra "đã sửa" cho post.
- `EntityReferenceService` (tên + kiểm tra tồn tại hotel/landmark/destination) dùng chung cho itinerary và posts.
- User test posts: `claude.test+p1@tripnova.local`, `claude.test+p2@tripnova.local` (mật khẩu `secret123`, không còn post nào).
- Order test `TN26092916177352` (user `claude.test+p1`, `Claude Test Hotel B`, đã hoàn tiền đủ) có hoá đơn → không xoá được booking/order này (FK RESTRICT).
- `role_permissions` không có entity → thao tác bằng native query trong `PermissionRepository` (như `room_type_amenities`).
- `date_trunc` với tham số: phải `CAST(:unit AS text)` (stringtype=unspecified). Tuần của Postgres bắt đầu thứ Hai → FE/BE điền kỳ trống phải khớp (`AdminDashboardServiceImpl.buckets`).
- Để test đặt phòng: bật tạm `Claude Test Hotel B` (`update hotels set is_active=true where name='Claude Test Hotel B'`), room type `a40ba716-…`, xong tắt lại. Script test Python (urllib) tiện hơn curl trên Git Bash.
- **Test E2E: chỉ thao tác trên dữ liệu tự tạo**, không dùng phòng/khách sạn seed (từng xoá nhầm phòng 403 seed, đã khôi phục). `room_availability` từng có dòng `booked` mồ côi (không có booking).
- Public: `/api/hotels/` và `/api/hotels/{id}/` giờ **chỉ trả khách sạn `is_active=true`** (xoá mềm từ admin).

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
