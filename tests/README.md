# Tests

Ba lớp test (Three layers):

| Lớp | Ở đâu | Chạy | Cần |
|---|---|---|---|
| Unit (JUnit) | `src/test/java` | `./mvnw test` | không gì cả |
| API end-to-end (pytest) | `tests/e2e` | `cd tests/e2e && python -m pytest -q` | backend đang chạy (+ Mailpit cho test email) |
| Giao diện (Playwright + Edge) | `tests/ui` | `cd tests/ui && npm install && npm test` | backend + frontend đang chạy, Microsoft Edge |

## Quy tắc dữ liệu — KHÔNG xoá dữ liệu có sẵn

Test chạy trên DB thật của máy dev nên:

- Dùng **tài khoản test cố định** (`e2e.user@tripnova.local`, đổi bằng biến môi trường): chưa có thì đăng ký, có rồi thì đăng nhập. Không xoá tài khoản. Tài khoản đăng ký mới phải xác thực email mới đăng nhập được → helper tự lấy link trong Mailpit (DB mới mà không chạy Mailpit thì test dừng kèm hướng dẫn).
- `test_email_verification.py` / `specs/email_verification.js` mỗi lần chạy tạo thêm tài khoản `e2e.verify...@tripnova.local` mới (không xoá).
- Đặt phòng / thuê xe chỉ trên **khách sạn / xe test** (`Claude Test Hotel B`, `Claude Test Car`; chưa có thì tạo). Bật lên khi test, **tắt lại** khi xong — không hiện cho khách.
- Booking tạo ra chỉ được **huỷ** (đổi trạng thái), không xoá. Không dùng `DELETE` lên dữ liệu không phải do test tạo.
- Test giao diện chỉ đọc, trừ `specs/routing.js` (lưu 2 lịch trình mới cho user test).

## API end-to-end (`tests/e2e`)

```bash
pip install -r tests/e2e/requirements.txt
# Mailpit để kiểm tra email (tuỳ chọn; không có thì các test email bị skip)
mailpit --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
# backend gửi email vào Mailpit
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.mail.host=127.0.0.1 --spring.mail.port=1025"

cd tests/e2e && python -m pytest -q
```

| Biến môi trường | Mặc định |
|---|---|
| `E2E_BASE_URL` | `http://localhost:8080` |
| `E2E_MAILPIT_URL` | `http://127.0.0.1:8025` |
| `E2E_ADMIN_EMAIL` / `E2E_ADMIN_PASSWORD` | `claude.test+a2@tripnova.local` / `secret123` (tài khoản ADMIN có sẵn) |
| `E2E_USER_EMAIL` / `E2E_USER_PASSWORD` | `e2e.user@tripnova.local` / `secret123` |

Nội dung: API công khai (phân trang, 404, 400), đăng ký → email chào mừng → bắt buộc xác thực trước khi đăng nhập → gửi lại link → xác thực, quên mật khẩu qua link email, đăng nhập / refresh token xoay vòng / phân quyền 401-403, email quên mật khẩu + xác thực, đặt phòng → thanh toán giả lập → hoá đơn PDF + email kèm PDF → admin gửi lại → huỷ, thanh toán lỗi giải phóng chỗ, thuê xe (trùng lịch 409), liên hệ → admin trả lời → email, AI Planner ghi lượt vào audit log, chấm điểm rủi ro gian lận cho đơn mới + admin đánh dấu, gợi ý cá nhân hoá + ghi lượt xem + khách sạn tương tự, tóm tắt review bằng AI qua hàng đợi `ai_jobs` (dịch vụ AI không chạy → bỏ qua) (cần dịch vụ AI Python; tự tạo 3 review trên khách sạn test rồi xoá đúng 3 review đó).

## Giao diện (`tests/ui`)

```bash
cd tests/ui
npm install            # chỉ playwright-core, dùng Edge có sẵn (không tải trình duyệt)
npm test               # chạy mọi spec trong specs/
node run.js smoke      # chỉ 1 spec
UI_HEADED=1 node run.js smoke   # xem trình duyệt
```

Biến môi trường: `UI_FE_URL` (`http://localhost:3000`), `UI_BE_URL` (`http://localhost:8080`), `UI_ADMIN_EMAIL/PASSWORD`, `UI_USER_EMAIL/PASSWORD`. Ảnh chụp lưu ở `tests/ui/shots/` (gitignore).

| Spec | Kiểm tra |
|---|---|
| `smoke.js` | ~30 trang khách / tài khoản / admin, VI + EN, desktop + điện thoại: có nội dung, không lỗi JS, không 5xx, không cuộn ngang; chặn khách vào trang cần đăng nhập / admin |
| `dropdown_overlap.js` | Gợi ý điểm đến của AI Planner không bị ô lưới phía sau che (sáng / tối / điện thoại) |
| `fraud.js` | Trang admin Rủi ro gian lận: số theo mức, mặc định "Chờ xem xét", lý do VI/EN, chip lọc mức Cao, đánh dấu Hợp lệ rồi trả lại Xem lại |
| `chatbot_availability.js` | Đang xem trang xe, hỏi chatbot "xe này còn trống ..." → trả lời có nhãn LIVE (xanh), giá VND; cần dịch vụ AI, hết hạn mức thì bỏ qua |
| `chatbot_stream.js` | Chatbot trả lời dạng stream: bong bóng chữ chạy dần → tin đã lưu (link, 👍/👎), tải lại vẫn còn, câu hỏi rỗng → 400 JSON; cần dịch vụ AI |
| `recommendations.js` | "Dành cho bạn" trên trang chủ (chưa đăng nhập không hiện; nhiều loại, lý do gợi ý, bấm thẻ), trang chi tiết ghi lượt xem, "Khách sạn tương tự"; EN + tối + điện thoại |
| `email_verification.js` | Đăng ký → màn "Kiểm tra hộp thư" → email chào mừng (Mailpit) → đăng nhập bị chặn + nút gửi lại → bấm link xác thực → đăng nhập; quên mật khẩu bằng link email; điện thoại EN. Cần Mailpit (`UI_MAILPIT_URL`) |
| `ai_summaries.js` | Khối "Tóm tắt từ AI" trên trang khách sạn (VI / EN / tối / điện thoại), trang admin Job AI (lọc, tab tóm tắt, Tạo lại). Bật tạm khách sạn test, tạo 3 review rồi xoá, tắt lại; cần dịch vụ AI |
| `routing.js` | Nút "Lên lịch trình bằng AI", đường đi OSRM trên bản đồ, bỏ điểm tải lại đường, lưu lịch trình (cần dịch vụ AI hoặc AI tắt → lịch trình tự động, và OSRM truy cập được) |

Viết spec mới: tạo `specs/<tên>.js`, dùng `require("../lib")` (`check`, `login`, `useAuth`, `watch`, `shot`, `run`).
