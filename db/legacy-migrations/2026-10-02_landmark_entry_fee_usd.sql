-- Phí vào cửa địa danh seed đang lưu theo VND (VD 150000) trong khi hệ thống/admin dùng USD
-- → FE hiện "$150,000.00". Quy đổi về USD theo tỷ giá 26.000 VND/USD (khớp REACT_APP_USD_VND_RATE).
-- Chỉ đụng giá trị >= 1000 (không có vé USD nào cao như vậy) → chạy lại không đổi thêm.
UPDATE landmarks
SET entry_fee = ROUND(entry_fee / 26000, 2),
    updated_at = now()
WHERE entry_fee >= 1000;
