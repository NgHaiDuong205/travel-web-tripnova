#!/usr/bin/env python3
"""
Geocode offline địa chỉ Việt Nam bằng bản đồ OpenStreetMap (Geofabrik vietnam-latest.osm.pbf).

    python db/import/vn_geocode.py build <vietnam-latest.osm.pbf> <index.pkl.gz>   # dựng chỉ mục (vài phút)

Dùng trong script import:  geo = Geocoder(<index.pkl.gz>); geo.locate(name, address, province_key, kind)
→ (lat, lng, precision) với precision 'poi' (khớp tên địa điểm) | 'street' (tâm đoạn đường) | 'place'
(xã/phường/thôn) hoặc None. Không trả toạ độ cấp tỉnh (quá thô cho bản đồ).

Mọi kết quả phải nằm trong bán kính quanh tâm tỉnh của bản ghi → tránh khớp nhầm cùng tên ở tỉnh khác.
Dữ liệu OSM © OpenStreetMap contributors (ODbL).
"""
import gzip
import math
import pickle
import re
import sys
from collections import defaultdict

from import_vn_hotels import DESTINATIONS, norm

PROVINCE_RADIUS_KM = 90
CHILD_RADIUS_KM = 30     # xã/thôn phải gần đơn vị lớn hơn đã khớp
STREET_RADIUS_KM = 15    # đoạn đường phải gần địa danh hành chính đã khớp
STREET_CLUSTER_KM = 6    # không có mốc hành chính: chỉ nhận đường nếu mọi đoạn trùng tên nằm gần nhau

# Tiền tố loại hình bỏ đi khi so tên (cả hai phía nguồn & OSM)
TYPE_WORDS = [
    "khu nghi duong", "khach san", "nha nghi", "nha khach", "nha hang", "quan an", "trung tam thuong mai",
    "sieu thi", "khu du lich", "khu vui choi giai tri", "khu vui choi", "cong vien", "bao tang", "nha tho",
    "ca phe", "homestay", "resort", "hotel", "motel", "hostel", "villa", "restaurant", "cafe", "coffee",
    "spa", "the", "boutique", "and", "&",
]
ADMIN_WORDS = ["thanh pho", "thi xa", "thi tran", "huyen", "quan", "phuong", "xa", "thon", "ap", "ban", "lang",
               "khu pho", "khu", "to dan pho", "to", "khoi", "xom", "doi", "p.", "tp.", "tp", "tx.", "q."]
STREET_WORDS = ["duong", "pho", "dai lo", "ngo", "hem", "kiet", "tinh lo", "quoc lo", "ql", "tl", "dt"]

LODGING = {("tourism", v) for v in ("hotel", "guest_house", "hostel", "motel", "apartment", "chalet", "camp_site",
                                    "caravan_site", "alpine_hut", "wilderness_hut")} | {("building", "hotel"),
                                                                                       ("leisure", "resort")}
FOOD = {("amenity", v) for v in ("restaurant", "cafe", "fast_food", "bar", "pub", "food_court", "biergarten",
                                 "ice_cream")}
PLACE_TYPES = {"city", "town", "village", "hamlet", "suburb", "quarter", "neighbourhood", "island", "islet",
               "locality", "isolated_dwelling", "municipality", "borough"}
STREET_HIGHWAYS = {"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                   "living_street", "pedestrian", "service", "road", "trunk_link", "primary_link",
                   "secondary_link", "tertiary_link"}


def _strip_words(text, words):
    text = re.sub(r"[^\w\s&.]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    changed = True
    while changed:
        changed = False
        for w in words:
            if text.startswith(w + " "):
                text, changed = text[len(w) + 1:], True
            if text.endswith(" " + w):
                text, changed = text[: -len(w) - 1], True
    return text.strip(" .")


def core_name(name):
    return _strip_words(norm(name), TYPE_WORDS)


def admin_core(segment):
    return _strip_words(norm(segment), ADMIN_WORDS)


def street_core(segment):
    s = norm(segment)
    s = re.sub(r"^(so nha|so|lo|can)\s+", "", s)
    s = re.sub(r"^[\d][\w/\-.]*\s+", "", s)  # số nhà 12, 12A, 34/5, 7-9
    return _strip_words(s, STREET_WORDS)


def km(a_lat, a_lng, b_lat, b_lng):
    dlat = math.radians(b_lat - a_lat)
    dlng = math.radians(b_lng - a_lng) * math.cos(math.radians((a_lat + b_lat) / 2))
    return 6371 * math.hypot(dlat, dlng)


def poi_group(tags):
    pairs = {(k, v) for k, v in tags.items()}
    if pairs & LODGING:
        return "lodging"
    if pairs & FOOD:
        return "food"
    if any(k in tags for k in ("tourism", "historic", "shop", "leisure", "natural")) or \
            tags.get("amenity") in ("place_of_worship", "marketplace", "theatre", "cinema", "arts_centre",
                                    "community_centre", "spa") or tags.get("building") in ("temple", "church", "pagoda"):
        return "attraction"
    return None


# ---------------------------------------------------------------- dựng chỉ mục
def build(pbf, out_path):
    import osmium

    pois = defaultdict(list)     # (group, core) -> [(lat, lng, tên gốc)]
    streets = defaultdict(list)  # core -> [(lat, lng)]
    places = defaultdict(list)   # core -> [(lat, lng, place)]

    def names(tags):
        out = set()
        for k in ("name", "name:vi", "name:en", "alt_name", "official_name", "short_name"):
            v = tags.get(k)
            if v:
                out.update(x.strip() for x in v.split(";") if x.strip())
        return out

    def add(tags, lat, lng):
        ns = names(tags)
        if not ns:
            return
        group = poi_group(tags)
        if group:
            for n in ns:
                c = core_name(n)
                if len(c) >= 4:
                    pois[(group, c)].append((lat, lng, n))
        if tags.get("place") in PLACE_TYPES:
            for n in ns:
                c = admin_core(n)
                if c:
                    places[c].append((lat, lng, tags.get("place")))

    class H(osmium.SimpleHandler):
        def node(self, n):
            if n.tags and n.location.valid():
                add(dict(n.tags), n.location.lat, n.location.lon)

        def way(self, w):
            tags = dict(w.tags)
            if not tags:
                return
            pts = [(nd.lat, nd.lon) for nd in w.nodes if nd.location.valid()]
            if not pts:
                return
            lat = sum(p[0] for p in pts) / len(pts)
            lng = sum(p[1] for p in pts) / len(pts)
            if tags.get("highway") in STREET_HIGHWAYS and tags.get("name"):
                for n in names(tags):
                    c = street_core(n)
                    if c:
                        streets[c].append((round(lat, 6), round(lng, 6)))
            add(tags, lat, lng)

    H().apply_file(pbf, locations=True, idx="flex_mem")
    with gzip.open(out_path, "wb") as f:
        pickle.dump({"pois": dict(pois), "streets": dict(streets), "places": dict(places)}, f, protocol=4)
    print(f"pois={len(pois)} streets={len(streets)} places={len(places)}")


# ---------------------------------------------------------------- tra cứu
class Geocoder:
    def __init__(self, index_path):
        with gzip.open(index_path, "rb") as f:
            idx = pickle.load(f)
        self.pois, self.streets, self.places = idx["pois"], idx["streets"], idx["places"]

    @staticmethod
    def _nearest(points, lat, lng, radius):
        best = None
        for p in points:
            d = km(lat, lng, p[0], p[1])
            if d <= radius and (best is None or d < best[0]):
                best = (d, p)
        return best[1] if best else None

    def _anchor(self, segments, lat, lng):
        """Đơn vị hành chính nhỏ nhất khớp được. Đi từ lớn → nhỏ (cuối → đầu địa chỉ), mỗi cấp phải nằm
        gần cấp trước; tên trùng (VD 2 nơi tên "Hạ Long") → thử mọi nhánh, chọn chuỗi khớp nhiều cấp nhất,
        hoà thì tổng khoảng cách nhỏ nhất. Được bỏ qua cấp không có trên bản đồ."""
        levels = [c for c in (admin_core(s) for s in reversed(segments)) if c]

        def search(i, anchor, radius):  # -> (số cấp khớp, -tổng km, điểm nhỏ nhất)
            if i == len(levels):
                return 0, 0.0, None
            best = search(i + 1, anchor, radius)
            for p in self.places.get(levels[i], ()):
                d = km(anchor[0], anchor[1], p[0], p[1])
                if d <= radius:
                    n, neg, pt = search(i + 1, (p[0], p[1]), CHILD_RADIUS_KM)
                    cand = (n + 1, neg - d, pt or (p[0], p[1]))
                    if cand[:2] > best[:2]:
                        best = cand
            return best

        return search(0, (lat, lng), PROVINCE_RADIUS_KM)[2]

    def locate(self, name, address, province_key, kind):
        """kind: 'lodging' | 'food' | 'attraction' (gồm cả mua sắm, giải trí, sức khoẻ, thể thao)."""
        _, plat, plng, _ = DESTINATIONS[province_key]
        segments = [s.strip() for s in (address or "").split(",") if s.strip()][:-1]  # bỏ phần tỉnh
        anchor = self._anchor(segments, plat, plng)
        ref = anchor or (plat, plng)

        groups = [kind] if kind != "attraction" else ["attraction", "food"]
        c = core_name(name or "")
        if len(c) >= 4:
            cands = [p for g in groups for p in self.pois.get((g, c), ())]
            hit = self._nearest(cands, ref[0], ref[1], CHILD_RADIUS_KM if anchor else PROVINCE_RADIUS_KM)
            if hit:
                return hit[0], hit[1], "poi"

        for seg in segments:
            sc = street_core(seg)
            if len(sc) < 3 or sc not in self.streets:
                continue
            pts = self.streets[sc]
            if anchor:
                hit = self._nearest(pts, anchor[0], anchor[1], STREET_RADIUS_KM)
                if hit:
                    return hit[0], hit[1], "street"
            else:
                near = [p for p in pts if km(plat, plng, p[0], p[1]) <= PROVINCE_RADIUS_KM]
                if near and all(km(near[0][0], near[0][1], p[0], p[1]) <= STREET_CLUSTER_KM for p in near):
                    return (sum(p[0] for p in near) / len(near), sum(p[1] for p in near) / len(near), "street")
        if anchor:
            return anchor[0], anchor[1], "place"
        return None


if __name__ == "__main__":
    if len(sys.argv) == 4 and sys.argv[1] == "build":
        build(sys.argv[2], sys.argv[3])
    else:
        sys.exit(__doc__)
