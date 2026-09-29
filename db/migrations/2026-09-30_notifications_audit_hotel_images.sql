-- 2026-09-30: bảng thông báo, nhật ký thao tác admin, ảnh khách sạn; thêm provider github cho OAuth2.
-- Chạy 1 lần trên DB (idempotent: IF NOT EXISTS).

ALTER TYPE oauth_provider ADD VALUE IF NOT EXISTS 'github';

-- Thông báo trong ứng dụng cho từng user.
CREATE TABLE IF NOT EXISTS notifications (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        varchar(40)  NOT NULL,             -- payment_success, booking_cancelled, refund, post_approved, comment_reply...
    title       varchar(200) NOT NULL,
    body        text,
    link        text,                               -- đường dẫn FE tương đối, VD /my-trips?tab=cars
    entity_type varchar(30),
    entity_id   uuid,
    is_read     boolean NOT NULL DEFAULT false,
    read_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notifications_user_time ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications (user_id) WHERE is_read = false;

-- Nhật ký thao tác (chủ yếu của admin): ai, làm gì, trên đối tượng nào.
CREATE TABLE IF NOT EXISTS audit_logs (
    id          bigserial PRIMARY KEY,
    user_id     uuid REFERENCES users(id) ON DELETE SET NULL,
    user_email  varchar(255),                       -- giữ lại email lúc thao tác (user có thể bị xoá)
    action      varchar(80)  NOT NULL,              -- VD admin.hotel.update
    http_method varchar(10),
    path        text,
    entity_type varchar(40),
    entity_id   varchar(64),
    status_code integer,
    ip_address  inet,
    user_agent  text,
    details     jsonb,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_time ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_time ON audit_logs (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);

-- Thư viện ảnh khách sạn (ngoài cover_image_url).
CREATE TABLE IF NOT EXISTS hotel_images (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id    uuid NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    url         text NOT NULL,
    caption     varchar(255),
    sort_order  integer NOT NULL DEFAULT 0,
    created_by  uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_hotel_images_hotel ON hotel_images (hotel_id, sort_order);
