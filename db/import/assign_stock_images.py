#!/usr/bin/env python3
"""
Gán ảnh minh hoạ (kho ảnh stock ở FE public/images/stock, xem collect_stock_photos.py) cho khách sạn và hạng phòng:
mỗi khách sạn đủ HOTEL_TARGET ảnh (giữ ảnh thật đang có, chỉ thêm phần thiếu), mỗi hạng phòng ROOM_TARGET ảnh.

    python db/import/assign_stock_images.py <CREDITS.json> <hotels.csv> <room_types.csv> <out.sql>

hotels.csv     = id,name,star_rating,image_count,cover_generic(t/f)       (khách sạn đang hoạt động)
room_types.csv = id,hotel_id,name,image_count,cover_generic(t/f)          (hạng phòng đang hoạt động)
  (lệnh psql \\copy xuất 2 file: xem HANDOFF mục "Ảnh stock")

- Ảnh chọn theo loại hình (khách sạn / resort / homestay / villa / căn hộ / du thuyền / cắm trại) và tên hạng phòng.
- Chọn ngẫu nhiên có seed theo id → chạy lại ra cùng kết quả; id ảnh = uuid5 → ON CONFLICT DO NOTHING.
- Caption ghi "Ảnh minh hoạ – tác giả, giấy phép" (CC BY / BY-SA bắt buộc ghi tác giả; nói rõ không phải ảnh thật của cơ sở).
- Ảnh bìa: chỉ thay khi ảnh bìa hiện tại là ảnh mặc định (Unsplash dùng chung) hoặc trống.
"""
import collections
import csv
import json
import random
import re
import sys

from import_vn_hotels import norm, q, uid, write_inserts

HOTEL_TARGET = 6
ROOM_TARGET = 3

HOTEL_TEMPLATES = {
    "hotel": ["exterior", "lobby", "room_double", "restaurant", "room_view", "room_bath"],
    "hotel_lux": ["exterior", "lobby", "pool", "room_double", "restaurant", "spa"],
    "guesthouse": ["exterior", "room_double", "lobby", "room_bath", "room_view", "restaurant"],
    "resort": ["resort", "pool", "room_double", "restaurant", "spa", "room_view"],
    "homestay": ["homestay", "room_double", "room_family", "restaurant", "room_view", "room_bath"],
    "villa": ["villa_ext", "pool", "room_villa", "room_villa", "restaurant", "room_view"],
    "apartment": ["exterior", "room_apartment", "room_apartment", "pool", "room_bath", "lobby"],
    "cruise": ["cruise", "cruise", "room_cabin", "restaurant", "room_view", "spa"],
    "camp": ["camp", "camp", "room_tent", "restaurant", "room_view", "homestay"],
}
ROOM_RULES = [  # (regex trên tên hạng phòng đã bỏ dấu, nhóm ảnh)
    (r"\btent\b|\blều\b|\bleu\b", ["room_tent", "camp", "room_view"]),
    (r"\bcabin\b", ["room_cabin", "room_cabin", "room_view"]),
    (r"\bvilla\b|\bbungalow\b", ["room_villa", "room_villa", "pool"]),
    (r"\bapartment\b|\bstudio\b|\bcan ho\b", ["room_apartment", "room_apartment", "room_bath"]),
    (r"\bsuite\b", ["room_suite", "room_suite", "room_bath"]),
    (r"\bfamily\b|\bgia dinh\b|\bdorm\b", ["room_family", "room_bath", "room_view"]),
    (r"\btwin\b", ["room_twin", "room_bath", "room_view"]),
]
ROOM_DEFAULT = ["room_double", "room_view", "room_bath"]
# nhóm thiếu ảnh (duyệt xong còn quá ít) → nhóm thay thế
FALLBACK = {"room_twin": "room_double", "room_family": "room_double", "room_suite": "room_double",
            "room_villa": "room_suite", "room_apartment": "room_double", "room_cabin": "room_double",
            "room_tent": "camp", "room_view": "room_double", "room_bath": "room_double", "cruise": "resort",
            "camp": "homestay", "homestay": "exterior", "villa_ext": "resort", "resort": "exterior", "spa": "pool",
            "pool": "resort", "lobby": "exterior", "restaurant": "lobby"}
MIN_BUCKET = 6


def hotel_kind(name, star, room_names):
    n = norm(name)
    rn = " ".join(norm(x) for x in room_names)
    if re.search(r"\b(du thuyen|cruise)\b", n) or "cabin" in rn:
        return "cruise"
    if re.search(r"\b(glamping|camping|cam trai)\b", n) or "tent" in rn:
        return "camp"
    if re.search(r"\b(villa|biet thu)\b", n) or "villa" in rn:
        return "villa"
    if re.search(r"\b(apartment|can ho|condotel)\b", n) or "apartment" in rn:
        return "apartment"
    if re.search(r"\bhomestay\b", n):
        return "homestay"
    if re.search(r"^nha nghi\b|\b(motel|guest ?house|hostel)\b", n):
        return "guesthouse"
    if re.search(r"\bresort\b|khu nghi duong", n):
        return "resort"
    return "hotel_lux" if (star or 0) >= 4 else "hotel"


def main(credits_path, hotels_csv, rooms_csv, out_path):
    credits = json.load(open(credits_path, encoding="utf-8"))
    pool = collections.defaultdict(list)
    for url in sorted(credits):
        m = re.match(r"^/images/stock/([^/]+)/", url)
        if m:
            pool[m.group(1)].append(url)

    def bucket(b):
        seen = set()
        while len(pool.get(b, [])) < MIN_BUCKET and b in FALLBACK and b not in seen:
            seen.add(b)
            b = FALLBACK[b]
        return pool[b]

    def caption(url):
        c = credits[url]
        who = re.sub(r"\s+", " ", c.get("creator") or "").strip() or "không rõ tác giả"
        return f"Ảnh minh hoạ – {who}, {c.get('license') or 'CC'}"[:255]

    def pick(rng, buckets, used):
        out = []
        for b in buckets:
            cands = [u for u in bucket(b) if u not in used] or bucket(b)
            if not cands:
                continue
            u = rng.choice(cands)
            used.add(u)
            out.append(u)
        return out

    rooms_by_hotel = collections.defaultdict(list)
    room_rows = list(csv.reader(open(rooms_csv, encoding="utf-8")))
    for rid, hid, name, cnt, generic in room_rows:
        rooms_by_hotel[hid].append(name)

    stats = collections.Counter()
    hotel_img, hotel_cover, room_img, room_cover = [], [], [], []
    for hid, name, star, cnt, generic in csv.reader(open(hotels_csv, encoding="utf-8")):
        cnt = int(cnt)
        kind = hotel_kind(name, float(star or 0), rooms_by_hotel[hid])
        stats[f"kind:{kind}"] += 1
        need = HOTEL_TARGET - cnt
        if need <= 0:
            continue
        tpl = HOTEL_TEMPLATES[kind]
        start = 1 if cnt > 0 else 0  # đã có ảnh thật (thường là mặt tiền) → bỏ ô mặt tiền
        buckets = (tpl[start:] + tpl)[:need]
        urls = pick(random.Random(f"hotel:{hid}"), buckets, set())
        for i, u in enumerate(urls):
            hotel_img.append([q(uid("stock-hotel", hid, u)), q(hid), q(u), q(caption(u)), q(100 + i)])
        stats["hotel_images"] += len(urls)
        if generic == "t" and urls:
            hotel_cover.append(f"({q(hid)}::uuid, {q(urls[0])})")

    for rid, hid, name, cnt, generic in room_rows:
        need = ROOM_TARGET - int(cnt)
        if need <= 0:
            continue
        n = norm(name)
        buckets = next((b for pat, b in ROOM_RULES if re.search(pat, n)), ROOM_DEFAULT)[:need]
        urls = pick(random.Random(f"room:{rid}"), buckets, set())
        for i, u in enumerate(urls):
            room_img.append([q(uid("stock-room", rid, u)), q(rid), q(u), q(caption(u)), q(100 + i)])
        stats["room_images"] += len(urls)
        if generic == "t" and urls:
            room_cover.append(f"({q(rid)}::uuid, {q(urls[0])})")

    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        out.write("-- Sinh bởi db/import/assign_stock_images.py — KHÔNG sửa tay.\nSET client_encoding = 'UTF8';\n\n")
        write_inserts(out, "hotel_images", ["id", "hotel_id", "url", "caption", "sort_order"], hotel_img,
                      "ON CONFLICT (id) DO NOTHING", chunk=1000)
        write_inserts(out, "room_type_images", ["id", "room_type_id", "url", "caption", "sort_order"], room_img,
                      "ON CONFLICT (id) DO NOTHING", chunk=1000)
        for table, rows in (("hotels", hotel_cover), ("room_types", room_cover)):
            for i in range(0, len(rows), 1000):
                out.write(f"UPDATE {table} t SET cover_image_url = v.url FROM (VALUES\n" + ",\n".join(rows[i:i + 1000])
                          + "\n) v(id, url)\nWHERE t.id = v.id AND (t.cover_image_url IS NULL OR t.cover_image_url LIKE '%images.unsplash.com%');\n")
    stats["hotel_covers"] = len(hotel_cover)
    stats["room_covers"] = len(room_cover)
    stats.update({f"pool:{k}": len(v) for k, v in pool.items()})
    print(json.dumps(dict(sorted(stats.items())), ensure_ascii=False, indent=1))


if __name__ == "__main__":
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    main(*sys.argv[1:])
