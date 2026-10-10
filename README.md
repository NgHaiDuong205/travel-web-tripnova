# TripNova — Travel Booking Backend

**[English](#english) · [Tiếng Việt](#tiếng-việt)**

---

## English

TripNova is a travel booking platform. This repository is the **backend REST API**. It covers hotels, car rentals, flights and tours, a shopping cart with checkout and payment, invoices (PDF), user content (reviews, comments), an AI chat assistant and an **AI trip planner** that builds itineraries from real places and real road routes.

The web frontend (React) and the AI service (Python) live in separate repositories.

### Tech stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 4.0.3 (Web MVC, Data JPA, Validation) |
| Security | Spring Security 7, JWT HS256 (OAuth2 Resource Server), OAuth2 login (Google / Facebook / GitHub) |
| Database | PostgreSQL 16 (extension `unaccent`) |
| Other | ModelMapper, OpenPDF (invoices), OSRM (road routing) |
| Build | Maven (wrapper `mvnw` included) |

### Features

- **Accounts**: register / login with JWT (access 15 min, rotating refresh token 7 days), email verification, forgot/reset password, OAuth2 social login, profile, avatar upload.
- **Roles**: `USER`, `ADMIN`, `HOTEL_MANAGER`, with a permission catalogue.
- **Catalogue**: continents → countries → destinations → landmarks; hotels, room types, rooms; cars; flights with seat maps; tours. Search with suggestions.
- **Booking**: per-day room availability, a 15-minute hold while the user pays, automatic expiry of unpaid bookings, free-cancellation window, full or partial refunds.
- **Cart and orders**: one order can hold several bookings. A mock payment gateway is included for development.
- **Invoices**: issued automatically when an order is paid. They can be downloaded as PDF with Vietnamese text.
- **Community**: posts and reviews for hotels, tours, cars, flights, destinations and landmarks, with comments, reactions and admin moderation.
- **Itineraries and AI**: user-made itineraries; AI Planner (clusters places by day, picks the shortest route, finds nearby restaurants, picks a hotel by budget, uses OSRM travel times); AI chat assistant; book a whole trip (hotel + car) from an itinerary.
- **Admin panel API**: dashboard and statistics, management of all entities, refunds, audit logs, contact messages, AI knowledge documents.
- **Hotel manager API**: manage your own hotels and rooms, view statistics.

### Project structure

```
src/main/java/com/duong/travelweb/
├── api/            REST controllers (*API.java)
├── service/        Service interfaces + impl/ (business logic)
├── repository/     Spring Data repositories + custom/impl (Criteria / native queries)
├── model/          JPA entities, DTOs, request/response objects
├── converter/      Entity ⇄ DTO mappers
├── builder/        Search/filter builders
├── config/         Security, CORS, MVC, scheduling, startup runners
├── security/       JWT service, auth error handlers
├── exception/      ApiException
├── advice/         Global exception handler (ControllerAdvisor)
└── util/           Helpers (current user, tokens, geo planning…)
src/main/resources/
├── application.properties        Shared configuration
├── application-uat.properties    Local DB credentials (git-ignored, you create it)
└── fonts/                        DejaVu Sans, used for PDF invoices
db/
├── migrations/     Incremental SQL migrations (run in file-name order)
├── seed/           Demo data (cars, rolling flight schedule)
└── import/         Python scripts that import Vietnamese hotels/landmarks + offline geocoding
```

### Requirements

- JDK 21
- PostgreSQL 16 (superuser access is needed once to create extensions)
- Optional: Python 3.11 for the AI service and the `db/import` scripts

### Installation

1. **Clone**
   ```bash
   git clone https://github.com/NgHaiDuong205/travel-web-tripnova.git
   cd travel-web-tripnova
   ```
2. **Create the database** `travel-web-project` and load the base schema (59 tables, see [Database](#database)).
   > The base schema dump is **not** in this repository yet. Get it from the project owner. The files in `db/migrations/` are only the changes made on top of it.
3. **Apply the migrations** in file-name order:
   ```bash
   for f in db/migrations/*.sql; do psql -v ON_ERROR_STOP=1 -d travel-web-project -f "$f"; done
   ```
   `2026-10-04_chatbot.sql` must be run as a superuser. It creates extensions and the restricted DB roles `chatbot_ro` and `chatbot_rag`, whose passwords you set by hand.
4. **(Optional) Load demo data**
   ```bash
   psql -1 -v ON_ERROR_STOP=1 -d travel-web-project -f db/seed/demo_cars_vn.sql
   psql -v days=60 -d travel-web-project -f db/seed/demo_flights_rolling.sql
   ```
5. **Create** `src/main/resources/application-uat.properties` (see [Configuration](#configuration)).
6. **Run**
   ```bash
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```
   The API starts at `http://localhost:8080`. Quick check:
   ```bash
   curl -i "http://localhost:8080/api/hotels/?page=1&limit=5"
   ```
7. **Create the first admin**: register an account, then restart the app with the environment variable `ADMIN_EMAIL=<that email>` and log in again.

### Configuration

`application-uat.properties` (local, git-ignored):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/travel-web-project
spring.datasource.username=postgres
spring.datasource.password=<your password>

# AI service (optional)
app.ai.service-url=http://127.0.0.1:8000
app.ai.internal-key=<shared secret with the AI service>
```

Main settings (in `application.properties`; most can also be set as environment variables):

| Key / env var | Default | Description |
|---|---|---|
| `app.jwt.secret` / `JWT_SECRET` | dev key | JWT signing key, **at least 32 bytes**. Always set your own in production. |
| `app.jwt.access-token-ttl-seconds` | `900` | Access token lifetime |
| `app.jwt.refresh-token-ttl-days` | `7` | Refresh token lifetime |
| `app.frontend-url` | `http://localhost:3000` | Frontend origin (CORS, links in emails) |
| `app.admin.bootstrap-email` / `ADMIN_EMAIL` | empty | This account becomes ADMIN at startup |
| `app.booking.hold-minutes` | `15` | How long an unpaid booking holds the room |
| `app.booking.currency` | `USD` | Currency of all prices |
| `app.payment.mock-enabled` | `true` | Enables the mock payment gateway (dev only) |
| `app.upload.dir` | `uploads` | Folder for uploaded images (served at `/uploads/**`) |
| `app.upload.public-base-url` | `http://localhost:8080` | Base URL used in links to uploaded files |
| `app.routing.enabled` | `true` | Use OSRM for real road routes (`false` = straight-line estimate) |
| `app.routing.base-url` | `https://router.project-osrm.org` | OSRM server. The public demo server is for testing only; run your own in production. |
| `app.ai.service-url` | `http://127.0.0.1:8000` | AI service URL |
| `app.ai.internal-key` / `AI_INTERNAL_KEY` | empty | Shared key for calls between the backend and the AI service |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | empty | Google login |
| `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | empty | Facebook login |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | empty | GitHub login |
| `app.contact.*` | — | Contact info shown on the Contact page |

Notes:
- Spring reads `application.properties` as ISO-8859-1. Use ASCII or `\uXXXX` for accented values.
- There is no mail server yet. Password-reset and verification links are written to the console log.

### Database

PostgreSQL, **59 tables** in 12 groups. Keys are UUIDs. Status columns are Postgres ENUMs.

| Group | Tables |
|---|---|
| Users / auth | `users`, `roles`, `permissions`, `role_permissions`, `user_roles`, `user_oauth_accounts`, `refresh_tokens`, `password_reset_tokens`, `user_preferences` |
| Geography | `continents`, `countries`, `destinations`, `landmarks` |
| Hotels | `hotels`, `amenities`, `hotel_amenities`, `room_types`, `room_type_amenities`, `rooms`, `room_availability` |
| Cars | `cars`, `car_bookings` |
| Flights | `flights`, `flight_seats`, `flight_bookings` |
| Tours | `tours`, `tour_hotels`, `tour_cars`, `tour_flights`, `tour_bookings` |
| Orders and payments | `orders`, `payments`, `invoices` |
| Cart and favorites | `carts`, `cart_items`, `favorites` |
| Community | `posts`, `comments`, `post_reactions`, `review_ai_analysis` |
| Contact | `contact_messages` |
| AI / ML | `chat_sessions`, `chat_messages`, `chat_feedback`, `knowledge_documents`, `knowledge_chunks`, `item_embeddings`, `similar_items`, `recommendations`, `price_history`, `price_predictions`, `ai_summaries`, `ai_jobs`, `image_ai_metadata`, `fraud_scores`, `search_queries`, `user_interactions` |
| Itineraries | `itineraries`, `itinerary_items` |

The migrations also add `notifications`, `audit_logs`, `hotel_images` and the `chatbot` schema (read-only views for the AI service).

Main relationships:

```
continents 1─N countries 1─N destinations 1─N landmarks
destinations 1─N hotels / cars / tours / itineraries

users N─N roles (user_roles)          roles N─N permissions (role_permissions)
hotels 1─N room_types 1─N rooms 1─N room_availability   (one row per room per day)
flights 1─N flight_seats              tours N─N hotels / cars / flights

users 1─N orders 1─N payments
orders 1─N hotel_bookings / car_bookings / flight_bookings / tour_bookings
orders 1─N invoices (invoice N─1 payment)

users 1─N carts 1─N cart_items        users 1─N favorites (item_type + item_id)
users 1─N posts 1─N comments (self-reference for replies)
users 1─N itineraries 1─N itinerary_items
users 1─N chat_sessions 1─N chat_messages
```

Things to know about the schema:
- `hotel_bookings.num_nights` is a generated column. `hotels.total_rooms` and `updated_at` are kept up to date by triggers.
- The app sets `stringtype=unspecified` so that Java `String` values can be written into ENUM, `inet` and `jsonb` columns.
- `spring.jpa.hibernate.ddl-auto=none`: Hibernate never changes the schema. Every schema change is a SQL file in `db/migrations/`.

### API overview

All endpoints start with `/api` and **end with a trailing slash** (`/api/hotels/`). Lists use `page` (starting at 1) and `limit`, and return the total count in the `X-Total-Count` response header. Protected endpoints need the header `Authorization: Bearer <accessToken>`.

| Area | Prefixes |
|---|---|
| Auth | `/api/auth/*` (register, login, refresh-token, logout, forgot/reset password, verify-email, oauth) |
| Current user | `/api/me/*` (profile, dashboard, bookings, payments, invoices, itineraries, notifications, posts), `/api/account/` |
| Catalogue (public) | `/api/continents/`, `/api/countries/`, `/api/destinations/`, `/api/landmarks/`, `/api/hotels/`, `/api/room-types/`, `/api/amenities/`, `/api/cars/`, `/api/flights/`, `/api/airports/`, `/api/tours/`, `/api/search/` |
| Booking and payment | `/api/hotel-bookings/`, `/api/car-bookings/`, `/api/flight-bookings/`, `/api/tour-bookings/`, `/api/cart/`, `/api/trip-cart/`, `/api/payments/` |
| Community | `/api/posts/`, `/api/comments/`, `/api/favorites/`, `/api/contact/` |
| AI and maps | `/api/ai/chat/`, `/api/ai/itineraries/`, `/api/routing/route/` |
| Uploads | `/api/uploads/images/` (files are served at `/uploads/**`) |
| Hotel manager | `/api/manager/*` (role `HOTEL_MANAGER`) |
| Admin | `/api/admin/*` (role `ADMIN`) |

### Data import scripts (optional)

`db/import/` holds Python scripts that turn a crawl of csdl.vietnamtourism.gov.vn into SQL. They import Vietnamese hotels and landmarks, with coordinates found offline from an OpenStreetMap extract. Running them twice is safe: IDs are fixed (uuid5) and inserts use `ON CONFLICT DO NOTHING`. Usage is described at the top of each script.

```bash
python db/import/vn_geocode.py build vietnam-latest.osm.pbf osm_index.pkl.gz
python db/import/import_vn_hotels.py <data_dir> hotels.sql
python db/import/geocode_vn_hotels.py <data_dir> osm_index.pkl.gz hotels_geo.sql
python db/import/import_vn_landmarks.py <data_dir> osm_index.pkl.gz landmarks.sql
```

### Development

```bash
./mvnw -q compile        # quick build
./mvnw test              # unit tests
```

---

## Tiếng Việt

TripNova là nền tảng đặt dịch vụ du lịch. Repo này là **backend REST API**, gồm: khách sạn, thuê xe, chuyến bay, tour; giỏ hàng, thanh toán; hoá đơn PDF; nội dung cộng đồng (review, bình luận); trợ lý chat AI và **AI lập lịch trình** dựng lịch trình từ địa điểm thật và đường đi thật.

Frontend (React) và dịch vụ AI (Python) nằm ở các repo riêng.

### Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.0.3 (Web MVC, Data JPA, Validation) |
| Bảo mật | Spring Security 7, JWT HS256 (OAuth2 Resource Server), đăng nhập OAuth2 (Google / Facebook / GitHub) |
| CSDL | PostgreSQL 16 (extension `unaccent`) |
| Khác | ModelMapper, OpenPDF (hoá đơn), OSRM (tính đường đi) |
| Build | Maven (có sẵn `mvnw`) |

### Chức năng

- **Tài khoản**: đăng ký / đăng nhập bằng JWT (access token 15 phút, refresh token 7 ngày, xoay vòng), xác thực email, quên / đặt lại mật khẩu, đăng nhập mạng xã hội, hồ sơ, ảnh đại diện.
- **Phân quyền**: `USER`, `ADMIN`, `HOTEL_MANAGER`, kèm danh mục quyền.
- **Danh mục**: châu lục → quốc gia → điểm đến → địa danh; khách sạn, hạng phòng, phòng; xe; chuyến bay và sơ đồ ghế; tour. Tìm kiếm có gợi ý.
- **Đặt chỗ**: quản lý phòng trống theo từng ngày, giữ phòng 15 phút trong lúc thanh toán, tự huỷ booking quá hạn chưa trả tiền, huỷ miễn phí trong thời hạn, hoàn tiền toàn phần hoặc một phần.
- **Giỏ hàng và đơn hàng**: một đơn có thể gồm nhiều booking. Có sẵn cổng thanh toán giả lập để dev.
- **Hoá đơn**: tự xuất khi đơn đã thanh toán, tải được file PDF hiển thị đúng tiếng Việt.
- **Cộng đồng**: bài viết / review cho khách sạn, tour, xe, chuyến bay, điểm đến, địa danh; có bình luận, reaction và admin kiểm duyệt.
- **Lịch trình và AI**: người dùng tự lập lịch trình; AI Planner (chia địa điểm theo ngày, chọn lộ trình ngắn nhất, tìm nhà hàng gần đó, chọn khách sạn theo ngân sách, tính thời gian đi lại bằng OSRM); trợ lý chat AI; đặt trọn gói khách sạn + xe từ lịch trình.
- **API trang quản trị**: dashboard và thống kê, quản lý mọi dữ liệu, hoàn tiền, nhật ký thao tác, tin nhắn liên hệ, tài liệu tri thức cho AI.
- **API quản lý khách sạn**: quản lý khách sạn và phòng của mình, xem thống kê.

### Cấu trúc thư mục

```
src/main/java/com/duong/travelweb/
├── api/            REST controller (*API.java)
├── service/        Interface service + impl/ (logic nghiệp vụ)
├── repository/     Spring Data repository + custom/impl (Criteria / native query)
├── model/          Entity JPA, DTO, request/response
├── converter/      Chuyển đổi Entity ⇄ DTO
├── builder/        Builder cho tìm kiếm / lọc
├── config/         Security, CORS, MVC, job định kỳ, tác vụ lúc khởi động
├── security/       JWT, xử lý lỗi xác thực
├── exception/      ApiException
├── advice/         Xử lý exception toàn cục (ControllerAdvisor)
└── util/           Tiện ích (user hiện tại, token, tính toán lịch trình…)
src/main/resources/
├── application.properties        Cấu hình chung
├── application-uat.properties    Thông tin DB trên máy (bị gitignore, tự tạo)
└── fonts/                        Font DejaVu Sans cho hoá đơn PDF
db/
├── migrations/     Các file SQL thay đổi schema (chạy theo thứ tự tên file)
├── seed/           Dữ liệu demo (xe, lịch bay cuốn chiếu)
└── import/         Script Python nạp khách sạn / địa danh Việt Nam + geocode offline
```

### Yêu cầu

- JDK 21
- PostgreSQL 16 (cần quyền superuser một lần để tạo extension)
- Tuỳ chọn: Python 3.11 cho dịch vụ AI và các script trong `db/import`

### Cài đặt

1. **Clone**
   ```bash
   git clone https://github.com/NgHaiDuong205/travel-web-tripnova.git
   cd travel-web-tripnova
   ```
2. **Tạo database** `travel-web-project` và nạp schema gốc (59 bảng, xem [Cấu trúc CSDL](#cấu-trúc-csdl)).
   > Repo **chưa có** file dump schema gốc, hãy xin chủ dự án. Thư mục `db/migrations/` chỉ chứa các thay đổi làm thêm trên schema đó.
3. **Chạy migration** theo thứ tự tên file:
   ```bash
   for f in db/migrations/*.sql; do psql -v ON_ERROR_STOP=1 -d travel-web-project -f "$f"; done
   ```
   File `2026-10-04_chatbot.sql` phải chạy bằng superuser. File này tạo extension và hai role DB bị giới hạn quyền là `chatbot_ro` và `chatbot_rag`; mật khẩu của hai role bạn tự đặt.
4. **(Tuỳ chọn) Nạp dữ liệu demo**
   ```bash
   psql -1 -v ON_ERROR_STOP=1 -d travel-web-project -f db/seed/demo_cars_vn.sql
   psql -v days=60 -d travel-web-project -f db/seed/demo_flights_rolling.sql
   ```
5. **Tạo file** `src/main/resources/application-uat.properties` (xem [Cấu hình](#cấu-hình)).
6. **Chạy**
   ```bash
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```
   API chạy ở `http://localhost:8080`. Kiểm tra nhanh:
   ```bash
   curl -i "http://localhost:8080/api/hotels/?page=1&limit=5"
   ```
7. **Tạo admin đầu tiên**: đăng ký một tài khoản, chạy lại app với biến môi trường `ADMIN_EMAIL=<email đó>`, rồi đăng nhập lại.

### Cấu hình

`application-uat.properties` (trên máy, bị gitignore):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/travel-web-project
spring.datasource.username=postgres
spring.datasource.password=<mật khẩu>

# Dịch vụ AI (tuỳ chọn)
app.ai.service-url=http://127.0.0.1:8000
app.ai.internal-key=<khoá dùng chung với dịch vụ AI>
```

Các cấu hình chính (trong `application.properties`; phần lớn đặt được bằng biến môi trường):

| Khoá / biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `app.jwt.secret` / `JWT_SECRET` | khoá dev | Khoá ký JWT, **tối thiểu 32 byte**. Bắt buộc đặt khoá riêng khi chạy thật. |
| `app.jwt.access-token-ttl-seconds` | `900` | Thời hạn access token |
| `app.jwt.refresh-token-ttl-days` | `7` | Thời hạn refresh token |
| `app.frontend-url` | `http://localhost:3000` | Địa chỉ frontend (CORS, link trong email) |
| `app.admin.bootstrap-email` / `ADMIN_EMAIL` | trống | Tài khoản này được cấp quyền ADMIN khi khởi động |
| `app.booking.hold-minutes` | `15` | Thời gian giữ phòng khi chưa thanh toán |
| `app.booking.currency` | `USD` | Đơn vị tiền của mọi giá |
| `app.payment.mock-enabled` | `true` | Bật cổng thanh toán giả lập (chỉ dùng khi dev) |
| `app.upload.dir` | `uploads` | Thư mục lưu ảnh upload (phục vụ tại `/uploads/**`) |
| `app.upload.public-base-url` | `http://localhost:8080` | URL gốc dùng trong link tới file upload |
| `app.routing.enabled` | `true` | Dùng OSRM tính đường đi thật (`false` = ước lượng theo đường chim bay) |
| `app.routing.base-url` | `https://router.project-osrm.org` | Máy chủ OSRM. Máy chủ demo công khai chỉ để thử; khi chạy thật nên tự dựng. |
| `app.ai.service-url` | `http://127.0.0.1:8000` | Địa chỉ dịch vụ AI |
| `app.ai.internal-key` / `AI_INTERNAL_KEY` | trống | Khoá dùng chung khi backend gọi dịch vụ AI và ngược lại |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | trống | Đăng nhập Google |
| `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | trống | Đăng nhập Facebook |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | trống | Đăng nhập GitHub |
| `app.contact.*` | — | Thông tin hiển thị ở trang Liên hệ |

Lưu ý:
- Spring đọc `application.properties` theo bảng mã ISO-8859-1. Giá trị có dấu tiếng Việt phải viết ASCII hoặc `\uXXXX`.
- Chưa có mail server. Link đặt lại mật khẩu và link xác thực email được ghi ra log console.

### Cấu trúc CSDL

PostgreSQL, **59 bảng** chia thành 12 nhóm. Khoá chính là UUID. Các cột trạng thái dùng kiểu ENUM của Postgres.

| Nhóm | Bảng |
|---|---|
| Người dùng / xác thực | `users`, `roles`, `permissions`, `role_permissions`, `user_roles`, `user_oauth_accounts`, `refresh_tokens`, `password_reset_tokens`, `user_preferences` |
| Địa lý | `continents`, `countries`, `destinations`, `landmarks` |
| Khách sạn | `hotels`, `amenities`, `hotel_amenities`, `room_types`, `room_type_amenities`, `rooms`, `room_availability` |
| Xe | `cars`, `car_bookings` |
| Chuyến bay | `flights`, `flight_seats`, `flight_bookings` |
| Tour | `tours`, `tour_hotels`, `tour_cars`, `tour_flights`, `tour_bookings` |
| Đơn hàng và thanh toán | `orders`, `payments`, `invoices` |
| Giỏ hàng và yêu thích | `carts`, `cart_items`, `favorites` |
| Cộng đồng | `posts`, `comments`, `post_reactions`, `review_ai_analysis` |
| Liên hệ | `contact_messages` |
| AI / ML | `chat_sessions`, `chat_messages`, `chat_feedback`, `knowledge_documents`, `knowledge_chunks`, `item_embeddings`, `similar_items`, `recommendations`, `price_history`, `price_predictions`, `ai_summaries`, `ai_jobs`, `image_ai_metadata`, `fraud_scores`, `search_queries`, `user_interactions` |
| Lịch trình | `itineraries`, `itinerary_items` |

Các migration thêm các bảng `notifications`, `audit_logs`, `hotel_images` và schema `chatbot` (các view chỉ đọc dành cho dịch vụ AI).

Quan hệ chính:

```
continents 1─N countries 1─N destinations 1─N landmarks
destinations 1─N hotels / cars / tours / itineraries

users N─N roles (user_roles)          roles N─N permissions (role_permissions)
hotels 1─N room_types 1─N rooms 1─N room_availability   (mỗi phòng mỗi ngày một dòng)
flights 1─N flight_seats              tours N─N hotels / cars / flights

users 1─N orders 1─N payments
orders 1─N hotel_bookings / car_bookings / flight_bookings / tour_bookings
orders 1─N invoices (invoice N─1 payment)

users 1─N carts 1─N cart_items        users 1─N favorites (item_type + item_id)
users 1─N posts 1─N comments (tự tham chiếu để trả lời bình luận)
users 1─N itineraries 1─N itinerary_items
users 1─N chat_sessions 1─N chat_messages
```

Cần biết về schema:
- `hotel_bookings.num_nights` là cột sinh tự động (generated column). `hotels.total_rooms` và `updated_at` do trigger cập nhật.
- App đặt `stringtype=unspecified` để ghi được giá trị `String` của Java vào cột ENUM, `inet`, `jsonb`.
- `spring.jpa.hibernate.ddl-auto=none`: Hibernate không bao giờ sửa schema. Mọi thay đổi schema đều là file SQL trong `db/migrations/`.

### Tổng quan API

Mọi endpoint bắt đầu bằng `/api` và **kết thúc bằng dấu `/`** (ví dụ `/api/hotels/`). Danh sách phân trang bằng `page` (bắt đầu từ 1) và `limit`; tổng số bản ghi trả trong header `X-Total-Count`. Endpoint cần đăng nhập phải gửi header `Authorization: Bearer <accessToken>`.

| Nhóm | Đường dẫn |
|---|---|
| Xác thực | `/api/auth/*` (register, login, refresh-token, logout, forgot/reset password, verify-email, oauth) |
| Người dùng hiện tại | `/api/me/*` (hồ sơ, dashboard, booking, thanh toán, hoá đơn, lịch trình, thông báo, bài viết), `/api/account/` |
| Danh mục (công khai) | `/api/continents/`, `/api/countries/`, `/api/destinations/`, `/api/landmarks/`, `/api/hotels/`, `/api/room-types/`, `/api/amenities/`, `/api/cars/`, `/api/flights/`, `/api/airports/`, `/api/tours/`, `/api/search/` |
| Đặt chỗ và thanh toán | `/api/hotel-bookings/`, `/api/car-bookings/`, `/api/flight-bookings/`, `/api/tour-bookings/`, `/api/cart/`, `/api/trip-cart/`, `/api/payments/` |
| Cộng đồng | `/api/posts/`, `/api/comments/`, `/api/favorites/`, `/api/contact/` |
| AI và bản đồ | `/api/ai/chat/`, `/api/ai/itineraries/`, `/api/routing/route/` |
| Upload | `/api/uploads/images/` (file phục vụ tại `/uploads/**`) |
| Quản lý khách sạn | `/api/manager/*` (role `HOTEL_MANAGER`) |
| Quản trị | `/api/admin/*` (role `ADMIN`) |

### Script nạp dữ liệu (tuỳ chọn)

Thư mục `db/import/` chứa các script Python chuyển bộ dữ liệu crawl từ csdl.vietnamtourism.gov.vn thành file SQL, để nạp khách sạn và địa danh Việt Nam. Toạ độ được tìm offline từ bản đồ OpenStreetMap. Chạy lại nhiều lần vẫn an toàn: id cố định (uuid5) và câu INSERT dùng `ON CONFLICT DO NOTHING`. Cách dùng ghi ở đầu mỗi script.

```bash
python db/import/vn_geocode.py build vietnam-latest.osm.pbf osm_index.pkl.gz
python db/import/import_vn_hotels.py <thư_mục_dữ_liệu> hotels.sql
python db/import/geocode_vn_hotels.py <thư_mục_dữ_liệu> osm_index.pkl.gz hotels_geo.sql
python db/import/import_vn_landmarks.py <thư_mục_dữ_liệu> osm_index.pkl.gz landmarks.sql
```

### Phát triển

```bash
./mvnw -q compile        # build nhanh
./mvnw test              # chạy unit test
```
