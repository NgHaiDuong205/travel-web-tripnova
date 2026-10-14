#!/usr/bin/env python3
"""
Gom ảnh "stock" có giấy phép mở cho khách sạn / hạng phòng (không phải ảnh của đúng khách sạn đó).

    python db/import/collect_stock_photos.py search  <work_dir>            # tìm ứng viên → candidates.json
    python db/import/collect_stock_photos.py thumbs  <work_dir>            # tải thumbnail + ghép contact sheet để duyệt
    python db/import/collect_stock_photos.py publish <work_dir> <fe_public_dir>
        # đọc approved.json {bucket: [candidate id…]} → tải ảnh 1280px, lưu <fe_public_dir>/images/stock/<bucket>/<id>.jpg
        # + <fe_public_dir>/images/stock/CREDITS.json (tác giả, giấy phép, link gốc)

Nguồn: Wikimedia Commons API (không key) và Openverse API (không key: 20 request/phút, 200/ngày).
Chỉ nhận giấy phép cho phép dùng thương mại: CC0, Public Domain, CC BY, CC BY-SA (BY/BY-SA phải ghi tác giả → CREDITS.json).
"""
import hashlib
import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

UA = "TripNovaStockCollector/1.0 (travel demo project; contact: admin@tripnova.local)"

# bucket → (truy vấn Commons, truy vấn Openverse)
BUCKETS = {
    # ảnh chung của khách sạn
    "exterior": (["hotel building facade", "hotel exterior", "boutique hotel exterior", "Vietnam hotel building"],
                 ["hotel exterior", "hotel facade", "boutique hotel building"]),
    "resort": (["beach resort", "resort bungalows", "resort Vietnam"], ["beach resort", "tropical resort"]),
    "lobby": (["hotel lobby", "hotel reception desk"], ["hotel lobby", "hotel reception"]),
    "pool": (["hotel swimming pool", "resort swimming pool", "infinity pool"], ["hotel swimming pool", "resort pool"]),
    "restaurant": (["hotel restaurant", "hotel breakfast buffet"], ["hotel restaurant", "hotel breakfast"]),
    "homestay": (["homestay Vietnam", "stilt house homestay", "guesthouse Vietnam"], ["homestay", "guesthouse"]),
    "villa_ext": (["villa with pool", "holiday villa"], ["villa pool", "holiday villa"]),
    "cruise": (["Ha Long Bay cruise ship", "cruise boat Ha Long"], ["halong bay cruise boat"]),
    "camp": (["glamping tent", "campsite tents"], ["glamping", "glamping tent"]),
    "spa": (["hotel spa", "spa massage room"], ["hotel spa"]),
    # ảnh phòng
    "room_double": (["hotel room double bed", "hotel bedroom", "hotel room interior"], ["hotel room", "hotel bedroom"]),
    "room_twin": (["hotel room twin beds", "twin room hotel"], ["twin beds hotel room"]),
    "room_family": (["family hotel room", "hotel room bunk bed"], ["family hotel room"]),
    "room_suite": (["hotel suite", "hotel suite living room"], ["hotel suite", "luxury hotel suite"]),
    "room_bath": (["hotel bathroom", "hotel room bathroom"], ["hotel bathroom"]),
    "room_view": (["hotel room view window", "hotel balcony"], ["hotel room view", "hotel balcony"]),
    "room_villa": (["villa bedroom", "villa living room"], ["villa bedroom", "villa interior"]),
    "room_apartment": (["serviced apartment interior", "apartment kitchen living room"], ["apartment interior"]),
    "room_cabin": (["ship cabin", "cruise ship cabin"], ["cruise cabin"]),
    "room_tent": (["glamping tent interior", "safari tent interior"], ["glamping tent interior"]),
    # ảnh khách sạn / resort thật ở Việt Nam (mặt tiền hợp bối cảnh hơn kiến trúc châu Âu)
    "exterior_vn": (["hotel Hanoi", "hotel Ho Chi Minh City", "hotel Saigon", "hotel Da Nang", "hotel Nha Trang",
                     "hotel Hoi An", "hotel Hue Vietnam", "hotel Da Lat", "hotel Vung Tau", "hotel Sa Pa",
                     "hotel Ha Long", "hotel Can Tho", "hotel Quy Nhon"], []),
    "resort_vn": (["resort Phu Quoc", "resort Mui Ne", "resort Nha Trang", "resort Da Nang", "resort Hoi An",
                   "resort Con Dao", "resort Quy Nhon", "resort Vietnam beach"], []),
    "restaurant_more": (["hotel dining room", "hotel breakfast room", "restaurant interior Vietnam",
                         "Vietnamese restaurant interior"], []),
    "room_vn": (["hotel room Vietnam", "hotel room Hanoi", "hotel room Da Nang", "hotel room Saigon",
                 "hotel room Hoi An", "hotel room king bed", "guest room hotel bed"], []),
}
OK_LICENSES = {"cc0", "pdm", "by", "by-sa"}


def get_json(url, tries=4):
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=40) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code == 429:
                time.sleep(65)
                continue
            if i == tries - 1:
                raise
        except Exception:
            if i == tries - 1:
                raise
        time.sleep(3 * (i + 1))


def commons_license(meta):
    short = ((meta.get("LicenseShortName") or {}).get("value") or "").lower()
    if "cc0" in short or "public domain" in short or short in ("pd", "no restrictions"):
        return "cc0" if "cc0" in short else "pdm"
    m = re.match(r"cc[ -]by(-sa)?[ -]\d", short)
    if m:
        return "by-sa" if m.group(1) else "by"
    return None


def strip_html(s):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", s or "")).strip()


def search_commons(query, limit=120):
    out, cont = [], {}
    while len(out) < limit:
        params = {"action": "query", "format": "json", "generator": "search", "gsrnamespace": 6,
                  "gsrsearch": f"{query} filetype:bitmap", "gsrlimit": 50, "prop": "imageinfo",
                  "iiprop": "url|size|extmetadata|mime", "iiurlwidth": 400, **cont}
        d = get_json("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(params))
        for p in (d.get("query") or {}).get("pages", {}).values():
            ii = (p.get("imageinfo") or [{}])[0]
            meta = ii.get("extmetadata") or {}
            lic = commons_license(meta)
            if not lic or ii.get("mime") not in ("image/jpeg", "image/png", "image/webp"):
                continue
            if (ii.get("width") or 0) < 1000 or (ii.get("height") or 0) < 650 or ii["width"] < ii["height"]:
                continue  # chỉ nhận ảnh ngang đủ lớn
            out.append({"id": "c" + hashlib.sha1(p["title"].encode()).hexdigest()[:12], "source": "commons",
                        "title": p["title"], "thumb": ii.get("thumburl"), "file_url": ii.get("url"),
                        "landing": ii.get("descriptionurl"), "license": lic,
                        "license_name": strip_html((meta.get("LicenseShortName") or {}).get("value")),
                        "creator": strip_html((meta.get("Artist") or {}).get("value"))[:200],
                        "width": ii["width"], "height": ii["height"]})
        if "continue" not in d:
            break
        cont = d["continue"]
        time.sleep(1)
    return out


def search_openverse(query, pages=3):
    out = []
    for page in range(1, pages + 1):
        params = {"q": query, "license_type": "commercial", "page_size": 20, "page": page,
                  "aspect_ratio": "wide", "size": "large", "mature": "false"}
        d = get_json("https://api.openverse.org/v1/images/?" + urllib.parse.urlencode(params))
        for r in d.get("results", []):
            if r.get("license") not in OK_LICENSES:
                continue
            out.append({"id": "o" + r["id"].replace("-", "")[:12], "source": "openverse:" + (r.get("source") or ""),
                        "title": r.get("title") or "", "thumb": r.get("thumbnail"), "file_url": r.get("url"),
                        "landing": r.get("foreign_landing_url"), "license": r["license"],
                        "license_name": f"CC {r['license'].upper()} {r.get('license_version') or ''}".strip(),
                        "creator": (r.get("creator") or "")[:200], "width": r.get("width"), "height": r.get("height")})
        if page >= (d.get("page_count") or 0):
            break
        time.sleep(3.2)  # 20 request/phút
    return out


def cmd_search(work):
    os.makedirs(work, exist_ok=True)
    path = os.path.join(work, "candidates.json")
    cands = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    for bucket, (cq, oq) in BUCKETS.items():
        if bucket in cands:
            continue
        seen, items = set(), []
        for q in cq:
            for c in search_commons(q):
                if c["id"] not in seen:
                    seen.add(c["id"]); items.append(c)
        for q in oq:
            for c in search_openverse(q):
                if c["id"] not in seen:
                    seen.add(c["id"]); items.append(c)
        cands[bucket] = items
        print(bucket, len(items), flush=True)
        json.dump(cands, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)


def download(url, dest, tries=3):
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=25) as r:
                data = r.read()
            with open(dest, "wb") as f:
                f.write(data)
            return True
        except urllib.error.HTTPError as e:
            time.sleep(30 if e.code == 429 else 2)
        except Exception:
            time.sleep(2)
    return False


def interleave(items):
    """Xen kẽ Commons / Openverse để contact sheet đầu có cả hai nguồn."""
    a = [c for c in items if c["source"] == "commons"]
    b = [c for c in items if c["source"] != "commons"]
    out = []
    for i in range(max(len(a), len(b))):
        out += a[i:i + 1] + b[i:i + 1]
    return out


def cmd_thumbs(work, limit="90", only=None):
    from PIL import Image, ImageDraw
    cands = json.load(open(os.path.join(work, "candidates.json"), encoding="utf-8"))
    tdir = os.path.join(work, "thumbs")
    os.makedirs(tdir, exist_ok=True)
    for bucket, items in cands.items():
        if only and bucket not in only.split(","):
            continue
        items = interleave(items)[:int(limit)]
        todo = [c for c in items if c.get("thumb") and not os.path.exists(os.path.join(tdir, c["id"] + ".jpg"))]
        with ThreadPoolExecutor(8) as ex:
            list(ex.map(lambda c: download(c["thumb"], os.path.join(tdir, c["id"] + ".jpg"), tries=2), todo))
        tiles = []
        for c in items:
            p = os.path.join(tdir, c["id"] + ".jpg")
            try:
                im = Image.open(p).convert("RGB")
                im.thumbnail((300, 200))
                tiles.append((c["id"], im))
            except Exception:
                pass
        # contact sheet 6 cột × 5 hàng, nhãn = id
        per = 30
        for s in range(0, len(tiles), per):
            chunk = tiles[s:s + per]
            sheet = Image.new("RGB", (6 * 310, ((len(chunk) + 5) // 6) * 228), "white")
            d = ImageDraw.Draw(sheet)
            for i, (cid, im) in enumerate(chunk):
                x, y = (i % 6) * 310, (i // 6) * 228
                sheet.paste(im, (x + 5, y + 5))
                d.text((x + 6, y + 208), cid, fill="black")
            sheet.save(os.path.join(work, f"sheet_{bucket}_{s // per + 1}.jpg"), quality=80)
        print(bucket, len(tiles), flush=True)


def cmd_publish(work, fe_public):
    from PIL import Image
    cands = json.load(open(os.path.join(work, "candidates.json"), encoding="utf-8"))
    approved = json.load(open(os.path.join(work, "approved.json"), encoding="utf-8"))
    by_id = {c["id"]: c for items in cands.values() for c in items}
    root = os.path.join(fe_public, "images", "stock")
    credits_path = os.path.join(root, "CREDITS.json")
    credits = json.load(open(credits_path, encoding="utf-8")) if os.path.exists(credits_path) else {}
    raw = os.path.join(work, "raw")
    os.makedirs(raw, exist_ok=True)

    def source_url(c):
        if c["source"] == "commons" and c.get("thumb"):  # thumbnail 1280px thay vì file gốc nhiều MB
            return re.sub(r"/\d+px-", "/1280px-", c["thumb"])
        return c["file_url"]

    todo = {cid for ids in approved.values() for cid in ids if not os.path.exists(os.path.join(raw, cid))}
    with ThreadPoolExecutor(6) as ex:
        list(ex.map(lambda cid: download(source_url(by_id[cid]), os.path.join(raw, cid)), sorted(todo)))
    for bucket, ids in approved.items():
        os.makedirs(os.path.join(root, bucket), exist_ok=True)
        for cid in ids:
            c = by_id[cid]
            rel = f"/images/stock/{bucket}/{cid}.jpg"
            out = os.path.join(root, bucket, cid + ".jpg")
            if not os.path.exists(out):
                src = os.path.join(raw, cid)
                if not os.path.exists(src) and not download(source_url(c), src):
                    print("FAIL", cid, source_url(c), flush=True)
                    continue
                try:
                    im = Image.open(src).convert("RGB")
                except Exception:
                    print("BAD", cid, flush=True)
                    continue
                im.thumbnail((1280, 960))
                im.save(out, "JPEG", quality=78, optimize=True, progressive=True)
            credits[rel] = {"creator": c["creator"], "license": c["license_name"], "source": c["landing"],
                            "title": c["title"]}
        print(bucket, len(ids), flush=True)
    json.dump(dict(sorted(credits.items())), open(credits_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    cmd, args = sys.argv[1], sys.argv[2:]
    {"search": cmd_search, "thumbs": cmd_thumbs, "publish": cmd_publish}[cmd](*args)
