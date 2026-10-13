# Chạy TripNova bằng Docker Compose

Một lệnh dựng toàn bộ hệ thống: **Postgres 16**, **backend** (Spring Boot), **dịch vụ AI** (FastAPI), **frontend** (React build, phục vụ bằng nginx), **Mailpit** (hộp thư giả để xem email), và tuỳ chọn **OSRM** tự host với bản đồ Việt Nam.

## Chuẩn bị

Ba repo nằm cạnh nhau (đổi đường dẫn trong `.env` nếu khác):

```
travel-web-tripnova/        <- repo này (docker-compose.yml)
fe-tripnova/
travel_chatbot_python/travel_chatbot/   (có .env chứa khoá Gemini)
```

```bash
cp docker/compose.env.example .env    # điền POSTGRES_PASSWORD, CHATBOT_*_PASSWORD, JWT_SECRET, AI_INTERNAL_KEY
docker compose up -d --build
```

| Dịch vụ | Địa chỉ |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Mailpit (xem email) | http://localhost:8025 |
| Postgres | `localhost:5433` (5433 để không đụng Postgres cài trên máy) |

Dịch vụ AI không mở cổng ra ngoài; chỉ backend gọi qua mạng nội bộ.

## Cơ sở dữ liệu

- Lần đầu (volume `pgdata` trống): `db-init/00-chatbot-roles.sh` tạo 2 role AI với mật khẩu trong `.env`, rồi backend khởi động và **Flyway** chạy `V1__baseline_schema.sql` để tạo toàn bộ schema. DB lúc này **trống dữ liệu** (chỉ có role USER/ADMIN/HOTEL_MANAGER).
- Muốn có dữ liệu thật: dump từ DB đang dùng rồi nạp vào container **trước** khi backend chạy lần đầu (Flyway thấy DB đã có bảng sẽ chỉ baseline, không chạy V1):

  ```bash
  pg_dump -h localhost -U postgres -d travel-web-project -Fc -f tripnova.dump    # chỉ đọc DB nguồn
  docker compose up -d db
  docker compose cp tripnova.dump db:/tmp/tripnova.dump
  docker compose exec db pg_restore -U postgres -d travel-web-project --no-owner /tmp/tripnova.dump
  docker compose up -d --build
  ```
- Dữ liệu nằm trong volume `pgdata`; `docker compose down` **không** xoá volume. Chỉ `docker compose down -v` mới xoá — đừng dùng nếu muốn giữ dữ liệu.
- Admin đầu tiên: đăng ký tài khoản trên web, đặt `ADMIN_EMAIL=<email>` trong `.env`, `docker compose up -d backend`, đăng nhập lại.

## Email

Mặc định backend gửi vào Mailpit (mọi email đều được giữ lại, không gửi ra ngoài). Dùng SMTP thật: đặt `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `MAIL_FROM` trong `.env` (xem `compose.env.example`).

## Đường đi (OSRM tự host)

Mặc định backend dùng máy chủ demo công cộng `router.project-osrm.org` (chỉ để thử, giới hạn tần suất). Tự host:

```bash
docker compose --profile routing up -d      # lần đầu: tải ~300 MB + xử lý 10-30 phút, cần ~4-6 GB RAM cho Docker
```

rồi đặt trong `.env`: `ROUTING_BASE_URL=http://osrm:5000`, `ROUTING_MAX_CONCURRENT=8` và `docker compose up -d backend`. Dữ liệu bản đồ nằm trong volume `osrm-data`, lần sau khởi động ngay. Đổi vùng bằng `OSRM_REGION` (VD `asia/vietnam`, `asia/thailand`).

## Lệnh hay dùng

```bash
docker compose logs -f backend          # xem log
docker compose up -d --build backend    # build lại sau khi sửa code backend
docker compose ps
docker compose down                     # dừng (giữ dữ liệu)
```
