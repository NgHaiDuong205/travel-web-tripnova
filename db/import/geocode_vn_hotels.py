#!/usr/bin/env python3
"""
Sinh SQL gán toạ độ cho khách sạn VN đã nạp bằng import_vn_hotels.py (geocode offline, xem vn_geocode.py).

    python db/import/geocode_vn_hotels.py <thư mục vietnamtourism_v1> <index.pkl.gz> <out.sql>

Chỉ cập nhật dòng còn latitude IS NULL (không ghi đè toạ độ admin đã nhập).
"""
import collections
import json
import os
import sys

from import_vn_hotels import clean_address, clean_text, norm, province_of, q, uid
from vn_geocode import Geocoder

def main(src_dir, index_path, out_path):
    geo = Geocoder(index_path)
    records = [json.loads(l) for l in open(os.path.join(src_dir, "cslt", "data.jsonl"), encoding="utf-8")]
    stats = collections.Counter()
    rows = []
    seen = set()
    for r in sorted(records, key=lambda r: int(r["id"])):
        address = clean_address(((r.get("fields") or {}).get("Địa chỉ") or [""])[0])
        name = clean_text(r.get("name"))
        if not address or not name or (norm(name), norm(address)) in seen:
            continue
        seen.add((norm(name), norm(address)))
        province = province_of(address)
        if not province:
            stats["no_province"] += 1
            continue
        hit = geo.locate(name, address, province, "lodging")
        if not hit:
            stats["none"] += 1
            continue
        stats[hit[2]] += 1
        rows.append(f"({q(uid('cslt', r['id']))}, {hit[0]:.7f}, {hit[1]:.7f})")
    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        for i in range(0, len(rows), 2000):
            out.write("UPDATE hotels h SET latitude = v.lat, longitude = v.lng, updated_at = now()\nFROM (VALUES\n")
            out.write(",\n".join(rows[i:i + 2000]))
            out.write("\n) v(id, lat, lng) WHERE h.id = v.id::uuid AND h.latitude IS NULL;\n")
    print(json.dumps(stats, indent=2))


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
