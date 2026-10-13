#!/bin/sh
# Chạy 1 lần khi volume Postgres còn trống (docker-entrypoint-initdb.d).
# Tạo 2 role đăng nhập cho dịch vụ AI; quyền (GRANT) do Flyway V1 cấp khi backend khởi động.
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v ro_pass="$CHATBOT_RO_PASSWORD" -v rag_pass="$CHATBOT_RAG_PASSWORD" <<'SQL'
CREATE ROLE chatbot_ro LOGIN PASSWORD :'ro_pass';
CREATE ROLE chatbot_rag LOGIN PASSWORD :'rag_pass';
SQL
