#!/usr/bin/env python3
"""
Sinh SQL nạp địa danh Việt Nam (bảng landmarks) từ bộ crawl csdl.vietnamtourism.gov.vn:
dest (điểm đến), area (khu du lịch), vcgt (vui chơi giải trí), rest (nhà hàng), shop (mua sắm),
cssk (chăm sóc sức khoẻ), tt (thể thao). Cần migration 2026-10-02b_landmark_location.sql.

    python db/import/import_vn_landmarks.py <thư mục vietnamtourism_v1> <index OSM .pkl.gz> <out.sql>

- Chạy lại được: id uuid5 theo (bộ dữ liệu, id nguồn), ON CONFLICT DO NOTHING.
- Điểm đến: cùng quy tắc với khách sạn (từ khoá địa chỉ → điểm đến có sẵn, không thì theo tỉnh).
- Toạ độ: geocode offline OSM (vn_geocode.py) — không khớp được thì để trống.
- Không có ảnh → cover_image_url NULL (FE hiện ảnh mặc định).
"""
import collections
import json
import os
import re
import sys

from import_vn_hotels import (DESTINATIONS, clean_address, clean_images, clean_text, destination_of, norm, province_of, q, uid,
                              write_inserts)
from vn_geocode import Geocoder

DATASETS = ["dest", "area", "vcgt", "rest", "shop", "cssk", "tt"]
FIXED_CATEGORY = {"rest": "restaurant", "shop": "market", "cssk": "wellness", "tt": "sport"}
DEFAULT_CATEGORY = {"dest": "other", "area": "park", "vcgt": "entertainment"}

# (regex trên tên đã bỏ dấu, loại) — xét theo thứ tự
CATEGORY_RULES = [
    (r"\ble hoi\b|\bfestival\b", "other"),
    (r"\bbao tang\b|\bnha trung bay\b|\bmuseum\b", "museum"),
    (r"^(chua|den|dinh|mieu|nha tho|thanh that|tu vien|thien vien|thanh duong|thien phat)\b|\b(chua|pagoda|temple|nha tho)\b", "temple"),
    (r"\bbai bien\b|^bien\b|\bbai tam\b|\bbeach\b", "beach"),
    (r"^cho\b|\bcho noi\b|\bsieu thi\b|\btrung tam thuong mai\b|\bmall\b", "market"),
    (r"\bdi tich\b|\bthanh co\b|\bco do\b|\bphao dai\b|\bnha tu\b|\bdia dao\b|\btuong niem\b|\btuong dai\b"
     r"|\bnha co\b|\bpho co\b|\blang co\b|\bhoang thanh\b|\bdi chi\b|\bcan cu\b|\bthap\b|\bcot co\b|\bdinh thu\b"
     r"|\bchien khu\b|\btran dia\b|\bkhu luu niem\b", "historical"),
    (r"^(nui|deo|hang|cao nguyen|dinh nui)\b|\bnui\b|\bdeo\b|\bhang dong\b|\bruong bac thang\b|\bfansipan\b", "mountain"),
    (r"\bcong vien nuoc\b|\bkhu vui choi\b|\bkaraoke\b|\bcinema\b|\bcgv\b|\brap chieu\b|\bbowling\b|\bthao cam vien\b"
     r"|\bnha hat\b|\bgame\b|\bclub\b", "entertainment"),
    (r"\bcong vien\b|\bvuon quoc gia\b|\bkhu bao ton\b|^ho\b|\bthac\b|\bsuoi\b|\brung\b|\bsinh thai\b|\bcu lao\b"
     r"|\bban dao\b|\bvinh\b|\bdao\b|\bdam\b|\bvuon\b|\bsong\b", "park"),
    (r"\bnha hang\b|\bquan an\b|\bcafe\b|\bcoffee\b|\bca phe\b|\bbuffet\b|\blau\b|\bnuong\b|\bam thuc\b", "restaurant"),
]
CATEGORY_VI = {"temple": "công trình tâm linh", "museum": "bảo tàng", "beach": "bãi biển", "mountain": "điểm tham quan thiên nhiên",
               "park": "điểm tham quan sinh thái", "market": "điểm mua sắm", "historical": "di tích lịch sử",
               "entertainment": "điểm vui chơi giải trí", "restaurant": "nhà hàng", "wellness": "cơ sở chăm sóc sức khoẻ",
               "sport": "cơ sở thể thao", "other": "điểm tham quan"}


# Luật trên tên CÒN DẤU (bỏ dấu thì "Lăng"/"Làng", "Động"/"Đồng", "Cồn"/"Con" trùng nhau) — xét trước
ACCENTED_RULES = [
    (r"^lễ hội\b", "other"),
    (r"^lăng\b|^(khu )?di sản\b|nghĩa trang|nhà lưu niệm|^đàn\b|hiển lâm các|\batk\b|văn miếu", "historical"),
    (r"^động\b|^hang\b|thạch động", "mountain"),
    (r"phật đài|linh từ|\btự$", "temple"),
    (r"^(cồn|hòn|phá|bàu|ghềnh|đồi cát|cồn cát|mũi)\b|thắng cảnh|khu du lịch|du lịch sinh thái", "park"),
    (r"phố đi bộ", "entertainment"),
]


def category_of(dataset, name):
    if dataset in FIXED_CATEGORY:
        return FIXED_CATEGORY[dataset]
    low = name.lower().replace("Ð", "Đ").replace("ð", "đ")
    for pattern, cat in ACCENTED_RULES:
        if re.search(pattern, low):
            return cat
    n = norm(name)
    for pattern, cat in CATEGORY_RULES:
        if re.search(pattern, n):
            return cat
    return DEFAULT_CATEGORY[dataset]


def first_field(fields, *keys, limit):
    for k in keys:
        for v in fields.get(k) or []:
            v = clean_text(v)
            if v and len(v) <= limit:
                return v
    return None


def opening_hours(fields):
    o = first_field(fields, "Giờ mở cửa", "Giờmởcửa", limit=200)
    c = first_field(fields, "Giờ đóng cửa", limit=50)
    if o and c:
        return None if o == c == "00:00" else f"{o} - {c}"  # 00:00-00:00 = nguồn không ghi giờ
    return o


def website_of(fields):
    w = first_field(fields, "Website", limit=240)
    if not w or " " in w or "." not in w:
        return None
    return w if re.match(r"^https?://", w, re.I) else "http://" + w


def main(src_dir, index_path, out_path):
    geo = Geocoder(index_path)
    common = set(json.load(open(os.path.join(src_dir, "common_images.json"), encoding="utf-8")))
    stats = collections.Counter()
    rows, used_dest, seen = [], {}, set()

    for ds in DATASETS:
        records = [json.loads(l) for l in open(os.path.join(src_dir, ds, "data.jsonl"), encoding="utf-8")]
        records.sort(key=lambda r: int(r["id"]))
        # tỉnh: huyện trùng tên → tỉnh của bản ghi gần nhất theo id (như khách sạn)
        prov = {}
        for r in records:
            a = clean_address(((r.get("fields") or {}).get("Địa chỉ") or (r.get("fields") or {}).get("Địachỉ") or [""])[0])
            prov[r["id"]] = (a, province_of(a) if a else None)
        known = [int(i) for i, (_, p) in prov.items() if p]
        for r in records:
            sid, name = r["id"], clean_text(r.get("name"))[:200]
            address, province = prov[sid]
            if r.get("status") != "ok" or not name or not address:
                stats[f"{ds}:skip_no_data"] += 1
                continue
            if not province and known:
                province = prov[str(min(known, key=lambda x: abs(x - int(sid))))][1]
            key = (norm(name), norm(address))
            if not province or key in seen:
                stats[f"{ds}:skip_dup_or_province"] += 1
                continue
            seen.add(key)
            dest = destination_of(address, province)
            cat = category_of(ds, name)
            fields = r.get("fields") or {}
            imgs = clean_images(r, common)
            desc = clean_text(r.get("description"))
            if len(desc) < 40:
                desc = f"{name} là {CATEGORY_VI[cat]} tại {address}."
                stats["desc_generated"] += 1
            hit = geo.locate(name, address, province, "food" if ds == "rest" else "attraction")
            stats[f"geo:{hit[2] if hit else 'none'}"] += 1
            stats[f"cat:{cat}"] += 1
            if not used_dest.get(dest):
                used_dest[dest] = imgs[0] if imgs else None
            rows.append([
                q(uid("lm", ds, sid)), f"(SELECT id FROM vn_dest WHERE key = {q(dest)})", q(name), q(desc),
                q(cat) + "::landmark_category", q(imgs[0] if imgs else None), q(opening_hours(fields)),
                q(address), q(first_field(fields, "Điện thoại cố định", "Điện thoại di động", "Điện thoại", "Điệnthoại", limit=30)),
                q(website_of(fields)), q(round(hit[0], 7) if hit else None), q(round(hit[1], 7) if hit else None), "true",
            ])

    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        out.write("-- Sinh bởi db/import/import_vn_landmarks.py — KHÔNG sửa tay.\nSET client_encoding = 'UTF8';\n\n")
        dest_rows = []
        for k in sorted(used_dest):
            name, lat, lng, popular = DESTINATIONS[k]
            desc = f"Discover {name}, Vietnam: local stays, culture, food and landscapes."
            dest_rows.append(f"({q(uid('dest', k))}, {q(name)}, {q(desc)}, {q(used_dest[k])}, {q(popular)}, {lat}, {lng})")
        out.write("""INSERT INTO destinations (id, country_id, name, description, cover_image_url, is_popular, is_active, latitude, longitude)
SELECT v.id::uuid, c.id, v.name, v.description, v.cover, v.popular, true, v.lat, v.lng
FROM (VALUES
%s
) v(id, name, description, cover, popular, lat, lng)
JOIN countries c ON c.country_code = 'VNM'
WHERE NOT EXISTS (SELECT 1 FROM destinations d WHERE d.country_id = c.id AND d.name = v.name);

CREATE TEMP TABLE vn_dest (key text PRIMARY KEY, id uuid NOT NULL) ON COMMIT DROP;
INSERT INTO vn_dest (key, id)
SELECT DISTINCT ON (v.key) v.key, d.id
FROM (VALUES %s) v(key, name)
JOIN countries c ON c.country_code = 'VNM'
JOIN destinations d ON d.country_id = c.id AND d.name = v.name
ORDER BY v.key, d.created_at;

""" % (",\n".join(dest_rows), ", ".join(f"({q(k)}, {q(DESTINATIONS[k][0])})" for k in sorted(used_dest))))
        write_inserts(out, "landmarks", ["id", "destination_id", "name", "description", "category", "cover_image_url",
                                         "opening_hours", "address", "phone", "website", "latitude", "longitude",
                                         "is_active"], rows, "ON CONFLICT (id) DO NOTHING")
    stats["landmarks"] = len(rows)
    print(json.dumps(dict(sorted(stats.items())), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
