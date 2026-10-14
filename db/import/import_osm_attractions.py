#!/usr/bin/env python3
"""
Nạp ĐIỂM THAM QUAN Việt Nam (bảng landmarks) từ OpenStreetMap, làm giàu bằng Wikidata / Wikipedia.

    python db/import/import_osm_attractions.py extract <vietnam-latest.osm.pbf> <work_dir>
        → work_dir/osm_attractions.json (điểm có tên, đúng loại, toạ độ = node hoặc tâm vùng)
    python db/import/import_osm_attractions.py enrich <work_dir>
        → work_dir/wikidata.json, wikipedia.json (cache, chạy lại chỉ lấy phần thiếu)
    python db/import/import_osm_attractions.py photos <work_dir>
        → work_dir/commons_geo.json: ảnh Commons trong 300 m có tên file khớp tên điểm (điểm chưa có ảnh Wikidata)
    python db/import/import_osm_attractions.py sql <work_dir> <refs.csv> <out.sql>
        refs.csv = "lat,lng,destination_name" của khách sạn + địa danh VN đã có (xem db/import/README hoặc HANDOFF)
        → INSERT landmarks, chạy: psql -1 -v ON_ERROR_STOP=1 -f out.sql

- Không lấy quán ăn / cửa hàng — chỉ điểm tham quan (bảo tàng, di tích, đền chùa có tiếng, bãi biển, thác, hang,
  núi, vườn quốc gia, công viên chủ đề, chợ có tiếng, điểm ngắm cảnh, tourism=attraction).
- Loại yếu (chùa / công viên / núi / tượng đài bình thường) chỉ lấy khi có wikidata / wikipedia / heritage / tourism=*.
- Điểm đến: đa số phiếu của 9 khách sạn/địa danh gần nhất trong 30 km (refs.csv); không có điểm tham chiếu
  trong 30 km → bỏ (thường là phần lãnh thổ nước láng giềng nằm trong file Geofabrik).
- Trùng: cùng tên (bỏ dấu) trong 400 m → giữ 1; trùng địa danh đã có trong DB (tên giống + ≤ 500 m) → bỏ trong SQL.
- Mô tả: đoạn mở đầu Wikipedia tiếng Việt → mô tả Wikidata → câu mẫu. Ảnh: Wikidata P18 (Wikimedia Commons, 960px).
- id = uuid5("osm", n|w|r<id>) → chạy lại an toàn (ON CONFLICT DO NOTHING).
Dữ liệu © OpenStreetMap contributors (ODbL); Wikidata CC0; mô tả Wikipedia CC BY-SA; ảnh Commons theo giấy phép từng file.
"""
import collections
import csv
import hashlib
import json
import math
import os
import re
import sys
import time
import urllib.parse
import urllib.request

from import_vn_hotels import norm, q, uid, write_inserts
from import_vn_landmarks import CATEGORY_VI, category_of

UA = "TripNovaImporter/1.0 (travel demo project; contact: admin@tripnova.local)"
FOREIGN_SCRIPT = re.compile(r"[฀-໿ក-៿぀-ヿ㐀-鿿가-힯]")  # Thái/Lào/Khmer/CJK/Hàn
GENERIC_NAMES = {"chua", "den", "dinh", "mieu", "nha tho", "bai bien", "thac", "cong vien", "bao tang", "cho",
                 "viewpoint", "attraction", "temple", "pagoda", "church", "beach", "waterfall", "park", "museum"}

# tên rác (OSM gắn thẻ sai: quán ăn / cây xăng / lối ra... bị gắn tourism=museum|attraction)
JUNK_NAME = re.compile(
    r"\b(quan an|cafe|coffee|ca phe|tra sua|nha hang|restaurant|food|drinks|gas|station|xang|atm|bank|"
    r"ngan hang|parking|bai do xe|bai giu xe|toilet|wc|hotel|khach san|homestay|nha nghi|hostel|shop|store|cua hang|"
    r"karaoke|bar|pub|massage|exit|entrance|loi vao|loi ra|xuat phat|ticket|ban ve|office|cty|cong ty|factory|"
    r"dien thoai|doanh trai|resort|nut giao|nga [345]|route \d|tap hoa|sieu thi|mart|spa|banh plan|che tim|che thap cam)\b|\d+ ?k\b")
GENERIC_EN = {"lookout tower", "viewpoint", "view point", "museum", "temple", "pagoda", "tower", "monument", "memorial",
              "statue", "park", "garden", "lake", "waterfall", "cave", "beach", "island", "bridge", "old house",
              "house", "home", "grotto", "pool", "village", "cau"}


def junk_name(name, has_wikidata):
    n = norm(name)
    if has_wikidata:
        return False
    if re.search(r"^(bao tang|museum)|museum$", n) and not re.search(r"\d+ ?k\b", n):
        return False
    if JUNK_NAME.search(n) or n in GENERIC_EN:
        return True
    if name[:1].islower() or len(n) < 5:  # "lim non", "Yểng"
        return True
    return False


HISTORIC_STRONG = {"castle", "fort", "ruins", "archaeological_site", "citadel", "city_gate", "tomb", "palace", "manor",
                   "battlefield", "monastery", "temple", "building", "house", "heritage", "city_walls", "bunker"}


# ---------------------------------------------------------------- phân loại
def classify(t):
    """tags → (category, cần_nổi_bật) hoặc None. cần_nổi_bật = chỉ nhận khi có wikidata/wikipedia/heritage/tourism."""
    tourism, historic, natural, leisure = t.get("tourism"), t.get("historic"), t.get("natural"), t.get("leisure")
    amenity = t.get("amenity")
    if tourism in ("museum", "gallery"):
        return "museum", False
    if tourism in ("theme_park", "zoo", "aquarium") or leisure == "water_park":
        return "entertainment", False
    if natural == "beach":
        return "beach", False
    if natural == "cave_entrance":
        return "mountain", False
    if natural in ("peak", "volcano", "ridge"):
        return "mountain", True
    if t.get("waterway") == "waterfall" or natural == "waterfall":
        return "park", False
    if historic in HISTORIC_STRONG:
        return "historical", historic in ("building", "house")
    if historic in ("monument", "memorial"):
        return "historical", True
    if amenity == "place_of_worship":
        return "temple", True
    if leisure == "nature_reserve" or t.get("boundary") == "national_park":
        return "park", False
    if t.get("boundary") == "protected_area":
        return "park", True
    if leisure in ("park", "garden"):
        return "park", True
    if t.get("place") in ("island", "islet") or natural in ("bay", "water", "strait", "cape"):
        return "park", True
    if amenity == "marketplace":
        return "market", True
    if tourism == "viewpoint":
        return "park", False
    if t.get("man_made") == "lighthouse":
        return "other", True
    if tourism == "attraction":
        return None, False  # loại theo tên (category_of), mặc định other
    return None


def is_notable(t):
    return any(k in t for k in ("wikidata", "wikipedia", "heritage", "heritage:operator")) or \
        t.get("tourism") in ("attraction", "museum", "viewpoint")


def pick_name(t):
    for k in ("name:vi", "name", "official_name", "name:en"):
        v = (t.get(k) or "").strip()
        if v and not FOREIGN_SCRIPT.search(v):
            return v[:200]
    return None


KEEP_TAGS = ("wikidata", "wikipedia", "website", "contact:website", "phone", "contact:phone", "opening_hours",
             "addr:housenumber", "addr:street", "addr:place", "addr:subdistrict", "addr:district", "addr:city",
             "addr:province", "addr:full", "name:en", "description", "image", "wikimedia_commons")


def cmd_extract(pbf, work):
    import osmium

    found = []

    def take(kind, oid, tags, lat, lng, area):
        t = {k: v for k, v in tags}
        c = classify(t)
        if c is None:
            return
        cat, weak = c
        name = pick_name(t)
        if not name or norm(name) in GENERIC_NAMES or len(norm(name)) < 3:
            return
        if weak and not is_notable(t):
            return
        if cat is None:
            cat = category_of("dest", name)
            if cat == "restaurant":
                return
        found.append({"osm": f"{kind}{oid}", "name": name, "category": cat, "lat": round(lat, 7), "lng": round(lng, 7),
                      "area": area, "notable": is_notable(t), "tags": {k: t[k] for k in KEEP_TAGS if k in t}})

    class H(osmium.SimpleHandler):
        def node(self, n):
            if n.tags and n.location.valid():
                take("n", n.id, n.tags, n.location.lat, n.location.lon, False)

        def way(self, w):
            if not w.tags or w.is_closed():
                return  # vùng khép kín xử lý ở area()
            pts = [(nd.lat, nd.lon) for nd in w.nodes if nd.location.valid()]
            if pts:
                mid = pts[len(pts) // 2]
                take("w", w.id, w.tags, mid[0], mid[1], False)

        def area(self, a):
            if not a.tags:
                return
            pts = [(nd.lat, nd.lon) for ring in a.outer_rings() for nd in ring if nd.location.valid()]
            if not pts:
                return
            lat = sum(p[0] for p in pts) / len(pts)
            lng = sum(p[1] for p in pts) / len(pts)
            oid = a.orig_id()
            take("w" if a.from_way() else "r", oid, a.tags, lat, lng, True)

    H().apply_file(pbf, locations=True, idx="flex_mem")

    # gộp trùng: cùng tên bỏ dấu trong 400 m → giữ bản "tốt" nhất
    found.sort(key=lambda r: (not r["notable"], not r["tags"].get("wikidata"), not r["area"]))
    kept, grid = [], collections.defaultdict(list)
    for r in found:
        key = norm(r["name"])
        cell = (round(r["lat"], 2), round(r["lng"], 2))
        dup = False
        for dy in (-0.01, 0, 0.01):
            for dx in (-0.01, 0, 0.01):
                for o in grid[(round(cell[0] + dy, 2), round(cell[1] + dx, 2))]:
                    if o["key"] == key and haversine(o["lat"], o["lng"], r["lat"], r["lng"]) < 0.4:
                        dup = True
        if dup:
            continue
        grid[cell].append({"key": key, "lat": r["lat"], "lng": r["lng"]})
        kept.append(r)
    os.makedirs(work, exist_ok=True)
    json.dump(kept, open(os.path.join(work, "osm_attractions.json"), "w", encoding="utf-8"), ensure_ascii=False)
    print(json.dumps({"raw": len(found), "kept": len(kept),
                      "by_category": collections.Counter(r["category"] for r in kept),
                      "with_wikidata": sum(1 for r in kept if r["tags"].get("wikidata"))}, ensure_ascii=False, indent=1))


def haversine(a_lat, a_lng, b_lat, b_lng):
    p = math.pi / 180
    h = math.sin((b_lat - a_lat) * p / 2) ** 2 + math.cos(a_lat * p) * math.cos(b_lat * p) * math.sin((b_lng - a_lng) * p / 2) ** 2
    return 12742 * math.asin(math.sqrt(h))


# ---------------------------------------------------------------- Wikidata / Wikipedia
def get_json(url, tries=5):
    for i in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=60) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if i == tries - 1:
                raise
            time.sleep(60 if e.code == 429 else 5 * (i + 1))
        except Exception:
            if i == tries - 1:
                raise
            time.sleep(5 * (i + 1))


def cmd_enrich(work):
    rows = json.load(open(os.path.join(work, "osm_attractions.json"), encoding="utf-8"))
    wd_path, wp_path = os.path.join(work, "wikidata.json"), os.path.join(work, "wikipedia.json")
    wd = json.load(open(wd_path, encoding="utf-8")) if os.path.exists(wd_path) else {}
    wp = json.load(open(wp_path, encoding="utf-8")) if os.path.exists(wp_path) else {}
    qids = sorted({r["tags"]["wikidata"].strip() for r in rows
                   if re.fullmatch(r"Q\d+", (r["tags"].get("wikidata") or "").strip())} - set(wd))
    for i in range(0, len(qids), 50):
        d = get_json("https://www.wikidata.org/w/api.php?" + urllib.parse.urlencode({
            "action": "wbgetentities", "format": "json", "ids": "|".join(qids[i:i + 50]),
            "props": "labels|descriptions|claims|sitelinks", "languages": "vi|en", "sitefilter": "viwiki|enwiki"}))
        for qid, e in (d.get("entities") or {}).items():
            img = None
            for c in (e.get("claims") or {}).get("P18", []):
                img = ((c.get("mainsnak") or {}).get("datavalue") or {}).get("value")
                if img:
                    break
            wd[qid] = {"label_vi": ((e.get("labels") or {}).get("vi") or {}).get("value"),
                       "desc_vi": ((e.get("descriptions") or {}).get("vi") or {}).get("value"),
                       "desc_en": ((e.get("descriptions") or {}).get("en") or {}).get("value"),
                       "image": img,
                       "viwiki": ((e.get("sitelinks") or {}).get("viwiki") or {}).get("title"),
                       "enwiki": ((e.get("sitelinks") or {}).get("enwiki") or {}).get("title")}
        json.dump(wd, open(wd_path, "w", encoding="utf-8"), ensure_ascii=False)
        print("wikidata", min(i + 50, len(qids)), "/", len(qids), flush=True)
        time.sleep(1)

    # tiêu đề Wikipedia tiếng Việt: từ sitelink Wikidata, hoặc tag wikipedia=vi:...
    titles = set()
    for r in rows:
        t = viwiki_title(r, wd)
        if t and t not in wp:
            titles.add(t)
    titles = sorted(titles)
    for i in range(0, len(titles), 20):
        chunk = titles[i:i + 20]
        d = get_json("https://vi.wikipedia.org/w/api.php?" + urllib.parse.urlencode({
            "action": "query", "format": "json", "prop": "extracts", "exintro": 1, "explaintext": 1,
            "exsentences": 5, "redirects": 1, "titles": "|".join(chunk)}))
        qd = d.get("query") or {}
        alias = {x["from"]: x["to"] for x in (qd.get("normalized") or []) + (qd.get("redirects") or [])}
        pages = {p.get("title"): p.get("extract") for p in (qd.get("pages") or {}).values()}
        for t in chunk:
            k = t
            while k in alias:
                k = alias[k]
            wp[t] = pages.get(k) or ""
        json.dump(wp, open(wp_path, "w", encoding="utf-8"), ensure_ascii=False)
        print("wikipedia", min(i + 20, len(titles)), "/", len(titles), flush=True)
        time.sleep(1)


TYPE_PREFIX = re.compile(
    r"^(khu du lich|khu di tich|di tich|bao tang|nha tho|nha co|chua|den|dinh|mieu|lang|thac|hang|dong|nui|deo|ho|"
    r"bai bien|bai tam|bai|bien|cong vien|vuon quoc gia|cho|cau|thanh|cot co|tuong dai|dao|hon|ghenh|mui|"
    r"museum|pagoda|temple|church|beach|waterfall|cave|mountain|lake|park|bridge|island|market|the)\s+")
COMMON_WORDS = {"viet", "nam", "vietnam", "chua", "den", "the", "and", "of", "khu", "lang", "long", "son", "an", "hoa",
                "thanh", "tan", "phu", "binh", "my", "hai", "dong", "tay", "nam", "bac", "trung", "quoc", "gia"}


def name_core(name):
    n = norm(name)
    prev = None
    while prev != n:
        prev, n = n, TYPE_PREFIX.sub("", n)
    return n.strip()


def title_matches(name, file_title):
    """Tên file Commons có chứa phần "lõi" của tên địa điểm (bỏ tiền tố loại hình) không."""
    core = name_core(name)
    t = norm(re.sub(r"^File:|\.\w+$", "", file_title).replace("_", " "))
    t = re.sub(r"[^a-z0-9 ]+", " ", t)
    if len(core) >= 6 and core in t:
        return True
    core = re.sub(r"[^a-z0-9 ]+", " ", core)
    words = [w for w in core.split() if len(w) >= 3 and w not in COMMON_WORDS]
    return len(words) >= 2 and sum(1 for w in words if re.search(rf"\b{re.escape(w)}\b", t)) >= 2


def cmd_photos(work):
    """Ảnh cho điểm chưa có ảnh Wikidata: Commons geosearch 300 m quanh toạ độ, tên file phải khớp tên điểm."""
    rows = json.load(open(os.path.join(work, "osm_attractions.json"), encoding="utf-8"))
    wd = json.load(open(os.path.join(work, "wikidata.json"), encoding="utf-8")) if os.path.exists(os.path.join(work, "wikidata.json")) else {}
    path = os.path.join(work, "commons_geo.json")
    cache = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    todo = [r for r in rows if r["osm"] not in cache and not (wd.get((r["tags"].get("wikidata") or "").strip()) or {}).get("image")
            and not junk_name(r["name"], bool(r["tags"].get("wikidata")))]
    from concurrent.futures import ThreadPoolExecutor

    def search(r):
        try:
            d = get_json("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({
                "action": "query", "format": "json", "list": "geosearch", "gscoord": f"{r['lat']}|{r['lng']}",
                "gsradius": 300, "gsnamespace": 6, "gslimit": 50}))
        except Exception:
            return r["osm"], None  # lỗi mạng → để lần chạy sau thử lại
        return r["osm"], [g["title"] for g in (d.get("query") or {}).get("geosearch", [])
                          if re.search(r"\.(jpe?g|png|webp)$", g["title"], re.I) and title_matches(r["name"], g["title"])][:3]

    with ThreadPoolExecutor(6) as ex:
        for i, (osm_id, hits) in enumerate(ex.map(search, todo)):
            if hits is not None:
                cache[osm_id] = hits
            if i % 100 == 0 or i == len(todo) - 1:
                json.dump(cache, open(path, "w", encoding="utf-8"), ensure_ascii=False)
                print("photos", i + 1, "/", len(todo), "matched", sum(1 for v in cache.values() if v), flush=True)


def viwiki_title(r, wd):
    qid = (r["tags"].get("wikidata") or "").strip()
    if qid in wd and wd[qid].get("viwiki"):
        return wd[qid]["viwiki"]
    w = r["tags"].get("wikipedia") or ""
    return w[3:] if w.startswith("vi:") else None


def commons_thumb(filename, width=960):
    fn = filename.strip().replace(" ", "_")
    h = hashlib.md5(fn.encode("utf-8")).hexdigest()
    enc = urllib.parse.quote(fn)
    suffix = ".jpg" if fn.lower().endswith((".tif", ".tiff")) else ""
    return f"https://upload.wikimedia.org/wikipedia/commons/thumb/{h[0]}/{h[:2]}/{enc}/{width}px-{enc}{suffix}"


# ---------------------------------------------------------------- SQL
def clean_extract(s):
    s = re.sub(r"\s*\([^()]*\)", lambda m: "" if re.search(r"[̀-ͯ]|/|;|tiếng|chữ Hán|IPA", m.group(0)) else m.group(0), s or "")
    s = re.sub(r"\s+\n", "\n", s).strip()
    return s[:1800]


def address_of(t):
    if t.get("addr:full"):
        return t["addr:full"][:500]
    parts = [" ".join(x for x in (t.get("addr:housenumber"), t.get("addr:street")) if x)]
    parts += [t.get(k) for k in ("addr:place", "addr:subdistrict", "addr:district", "addr:city", "addr:province")]
    parts = [p for p in parts if p]
    return ", ".join(dict.fromkeys(parts))[:500] if parts else None


def website_of(t):
    w = (t.get("website") or t.get("contact:website") or "").strip().split(";")[0]
    if not w or " " in w or "." not in w:
        return None
    return (w if re.match(r"^https?://", w, re.I) else "http://" + w)[:240]


def cmd_sql(work, refs_csv, out_path):
    rows = json.load(open(os.path.join(work, "osm_attractions.json"), encoding="utf-8"))
    wd = json.load(open(os.path.join(work, "wikidata.json"), encoding="utf-8")) if os.path.exists(os.path.join(work, "wikidata.json")) else {}
    wp = json.load(open(os.path.join(work, "wikipedia.json"), encoding="utf-8")) if os.path.exists(os.path.join(work, "wikipedia.json")) else {}
    geo_path = os.path.join(work, "commons_geo.json")
    geo = json.load(open(geo_path, encoding="utf-8")) if os.path.exists(geo_path) else {}

    # lưới 0.1° các điểm tham chiếu (khách sạn + địa danh VN đã có, kèm điểm đến)
    grid = collections.defaultdict(list)
    for lat, lng, dest in csv.reader(open(refs_csv, encoding="utf-8")):
        lat, lng = float(lat), float(lng)
        grid[(int(math.floor(lat * 10)), int(math.floor(lng * 10)))].append((lat, lng, dest))

    def destination_of(lat, lng):
        ci, cj = int(math.floor(lat * 10)), int(math.floor(lng * 10))
        near = []
        for di in range(-3, 4):
            for dj in range(-3, 4):
                for p in grid[(ci + di, cj + dj)]:
                    d = haversine(lat, lng, p[0], p[1])
                    if d <= 30:
                        near.append((d, p[2]))
        if not near:
            return None
        near.sort()
        votes = collections.Counter()
        for d, dest in near[:9]:
            votes[dest] += 1 / (1 + d)
        return votes.most_common(1)[0][0]

    stats = collections.Counter()
    out_rows = []
    for r in rows:
        if junk_name(r["name"], bool(r["tags"].get("wikidata"))):
            stats["skip_junk_name"] += 1
            continue
        dest = destination_of(r["lat"], r["lng"])
        if not dest:
            stats["skip_outside_vn"] += 1
            continue
        t = r["tags"]
        w = wd.get((t.get("wikidata") or "").strip()) or {}
        name = w.get("label_vi") if w.get("label_vi") and not FOREIGN_SCRIPT.search(w["label_vi"]) and \
            norm(w["label_vi"]) == norm(r["name"]) else r["name"]
        extract = clean_extract(wp.get(viwiki_title(r, wd) or "", ""))
        if len(extract) >= 80:
            desc = extract
            stats["desc_wikipedia"] += 1
        elif w.get("desc_vi") and len(w["desc_vi"]) >= 10:
            desc = f"{name} – {w['desc_vi'].rstrip('.')}, thuộc {dest}."
            stats["desc_wikidata"] += 1
        else:
            desc = f"{name} là {CATEGORY_VI[r['category']]} tại {dest}, Việt Nam."
            stats["desc_generated"] += 1
        img = None
        if w.get("image"):
            img = commons_thumb(w["image"])
        elif (t.get("wikimedia_commons") or "").startswith("File:"):
            img = commons_thumb(t["wikimedia_commons"][5:])
        elif geo.get(r["osm"]):
            img = commons_thumb(geo[r["osm"]][0][5:])
            stats["image_geosearch"] += 1
        stats["with_image"] += 1 if img else 0
        stats[f"cat:{r['category']}"] += 1
        out_rows.append([q(uid("osm", r["osm"])), q(dest), q(name), q(desc), q(r["category"]), q(img),
                         q((t.get("opening_hours") or "")[:200] or None), q(address_of(t)),
                         q((t.get("phone") or t.get("contact:phone") or "").split(";")[0][:30] or None),
                         q(website_of(t)), q(r["lat"]), q(r["lng"])])

    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        out.write("-- Sinh bởi db/import/import_osm_attractions.py — KHÔNG sửa tay.\n"
                  "-- Dữ liệu © OpenStreetMap contributors (ODbL), Wikidata (CC0), Wikipedia (CC BY-SA).\n"
                  "SET client_encoding = 'UTF8';\n\n"
                  "CREATE TEMP TABLE osm_lm (id uuid, dest text, name text, description text, category text, cover text,\n"
                  "  opening_hours text, address text, phone text, website text, lat double precision, lng double precision) ON COMMIT DROP;\n")
        write_inserts(out, "osm_lm", ["id", "dest", "name", "description", "category", "cover", "opening_hours", "address",
                                       "phone", "website", "lat", "lng"], out_rows, conflict="")
        out.write("""
-- bỏ điểm trùng địa danh đã có: tên giống (bỏ dấu) và cách ≤ 500 m
DELETE FROM osm_lm o USING landmarks l
WHERE l.latitude IS NOT NULL AND abs(l.latitude - o.lat) < 0.005 AND abs(l.longitude - o.lng) < 0.005
  AND (chatbot.norm(l.name) = chatbot.norm(o.name)
       OR chatbot.norm(l.name) LIKE '%' || chatbot.norm(o.name) || '%'
       OR chatbot.norm(o.name) LIKE '%' || chatbot.norm(l.name) || '%');

INSERT INTO landmarks (id, destination_id, name, description, category, cover_image_url, opening_hours, address,
                       phone, website, latitude, longitude, is_active)
SELECT o.id, d.id, o.name, o.description, o.category::landmark_category, o.cover, o.opening_hours, o.address,
       o.phone, o.website, o.lat, o.lng, true
FROM osm_lm o
JOIN countries c ON c.country_code = 'VNM'
JOIN LATERAL (SELECT d.id FROM destinations d WHERE d.country_id = c.id AND d.name = o.dest
              ORDER BY d.created_at LIMIT 1) d ON true
ON CONFLICT (id) DO NOTHING;
""")
    stats["rows"] = len(out_rows)
    print(json.dumps(dict(sorted(stats.items())), ensure_ascii=False, indent=1))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    cmd = sys.argv[1]
    {"extract": cmd_extract, "enrich": cmd_enrich, "photos": cmd_photos, "sql": cmd_sql}[cmd](*sys.argv[2:])
