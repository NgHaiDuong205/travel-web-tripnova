#!/usr/bin/env python3
"""
Sinh SQL nạp khách sạn Việt Nam từ bộ crawl csdl.vietnamtourism.gov.vn (thư mục `cslt/`).

    python db/import/import_vn_hotels.py <thư mục vietnamtourism_v1> <file .sql đầu ra>
    psql -v ON_ERROR_STOP=1 -1 -d travel-web-project -f <file .sql>

Nguyên tắc:
- Chạy lại được: mọi id là uuid5 cố định theo id nguồn, INSERT ... ON CONFLICT DO NOTHING
  (chạy lần 2 không tạo thêm, không ghi đè dữ liệu admin đã sửa).
- Ngẫu nhiên có seed theo id nguồn → cùng đầu vào luôn ra cùng kết quả.
- Khách sạn gắn vào điểm đến VN có sẵn (theo từ khoá địa chỉ: Sa Pa, Hội An, ...), không khớp thì
  theo tỉnh (tỉnh chưa có điểm đến → tạo mới). Quốc gia `VNM` thiếu thì tạo.
- Giá lưu USD (app.booking.currency); giá nguồn là VND → quy đổi theo VND_PER_USD
  (FE dùng cùng tỷ giá để hiển thị VND khi chọn tiếng Việt).
- Thiếu hạng sao / giá / mô tả / ảnh → sinh ngẫu nhiên hợp lý / mô tả mẫu / ảnh mặc định.
"""
import json
import os
import random
import re
import sys
import unicodedata
import uuid

VND_PER_USD = 26000
DEFAULT_HOTEL_IMAGE = "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&q=80"
DEFAULT_ROOM_IMAGE = "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=1200&q=80"
MAX_ROOMS_PER_HOTEL = 40
MAX_IMAGES_PER_HOTEL = 30
NS = uuid.UUID("6f1c0a52-7d0e-4c55-9a51-2b1f1e0c9a10")


def uid(*parts):
    return str(uuid.uuid5(NS, ":".join(str(p) for p in parts)))


def unaccent(s):
    s = s.replace("đ", "d").replace("Đ", "D")
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")


def norm(s):
    return re.sub(r"\s+", " ", unaccent(s or "").lower()).strip()


# ---------------------------------------------------------------- điểm đến
# key nội bộ -> (tên điểm đến trong DB, vĩ độ, kinh độ, nổi bật?)
DESTINATIONS = {
    # Điểm đến đã có trong DB (chỉ tạo nếu DB khác chưa có)
    "Hanoi": ("Hanoi", 21.0285, 105.8542, True),
    "Ho Chi Minh City": ("Ho Chi Minh City", 10.7769, 106.7009, True),
    "Da Nang": ("Da Nang", 16.0544, 108.2022, True),
    "Hue": ("Hue", 16.4637, 107.5909, True),
    "Can Tho": ("Can Tho", 10.0452, 105.7469, False),
    "Ninh Binh": ("Ninh Binh", 20.2506, 105.9745, True),
    "Tay Ninh": ("Tay Ninh", 11.3100, 106.0983, False),
    "An Giang": ("An Giang", 10.3866, 105.4352, False),
    "Ben Tre": ("Ben Tre", 10.2434, 106.3756, False),
    "Ca Mau": ("Ca Mau", 9.1769, 105.1524, False),
    "Ha Giang": ("Ha Giang", 22.8233, 104.9836, True),
    "Sa Pa": ("Sa Pa", 22.3364, 103.8438, True),
    "Bac Ha": ("Bac Ha", 22.5390, 104.2900, False),
    "Hoi An": ("Hoi An", 15.8801, 108.3380, True),
    "Phu Quoc": ("Phu Quoc", 10.2899, 103.9840, True),
    "Ha Long Bay": ("Ha Long Bay", 20.9101, 107.1839, True),
    "Cat Ba Island": ("Cat Ba Island", 20.7270, 107.0480, False),
    "Da Lat": ("Da Lat", 11.9404, 108.4583, True),
    "Nha Trang": ("Nha Trang", 12.2388, 109.1967, True),
    "Mui Ne": ("Mui Ne", 10.9333, 108.2833, True),
    "Phan Thiet": ("Phan Thiet", 10.9280, 108.1021, False),
    "Vung Tau": ("Vung Tau", 10.3460, 107.0843, False),
    "Con Dao": ("Con Dao", 8.6833, 106.6000, False),
    "Quy Nhon": ("Quy Nhon", 13.7830, 109.2197, False),
    "Tam Dao": ("Tam Dao", 21.4569, 105.6460, False),
    "Mai Chau": ("Mai Chau", 20.6640, 105.0830, False),
    "Ba Vi": ("Ba Vi", 21.0790, 105.3720, False),
    "Ly Son Island": ("Ly Son Island", 15.3800, 109.1170, False),
    "Phong Nha": ("Phong Nha", 17.5900, 106.2830, True),
    # Tỉnh chưa có điểm đến → tạo mới (toạ độ tỉnh lỵ)
    "Quang Ninh": ("Quang Ninh", 21.0064, 107.2925, False),
    "Khanh Hoa": ("Khanh Hoa", 11.9214, 109.1591, False),
    "Binh Thuan": ("Binh Thuan", 10.6800, 107.7700, False),
    "Lam Dong": ("Lam Dong", 11.5480, 107.8077, False),
    "Ba Ria - Vung Tau": ("Ba Ria - Vung Tau", 10.4963, 107.1684, False),
    "Nghe An": ("Nghe An", 18.6734, 105.6923, False),
    "Thanh Hoa": ("Thanh Hoa", 19.8067, 105.7852, False),
    "Quang Nam": ("Quang Nam", 15.5736, 108.4740, False),
    "Ha Tinh": ("Ha Tinh", 18.3428, 105.9057, False),
    "Son La": ("Son La", 21.3256, 103.9188, False),
    "Dong Thap": ("Dong Thap", 10.4602, 105.6329, False),
    "Lao Cai": ("Lao Cai", 22.4856, 103.9707, False),
    "Hai Phong": ("Hai Phong", 20.8449, 106.6881, False),
    "Dak Lak": ("Dak Lak", 12.6667, 108.0500, False),
    "Gia Lai": ("Gia Lai", 13.9833, 108.0000, False),
    "Phu Tho": ("Phu Tho", 21.3227, 105.4019, False),
    "Tuyen Quang": ("Tuyen Quang", 21.8233, 105.2140, False),
    "Cao Bang": ("Cao Bang", 22.6657, 106.2570, False),
    "Kon Tum": ("Kon Tum", 14.3497, 108.0005, False),
    "Phu Yen": ("Phu Yen", 13.0955, 109.3209, False),
    "Quang Tri": ("Quang Tri", 16.8163, 107.1003, False),
    "Bac Ninh": ("Bac Ninh", 21.1861, 106.0763, False),
    "Lang Son": ("Lang Son", 21.8537, 106.7615, False),
    "Thai Binh": ("Thai Binh", 20.4463, 106.3366, False),
    "Bac Kan": ("Bac Kan", 22.1470, 105.8348, False),
    "Ninh Thuan": ("Ninh Thuan", 11.5649, 108.9886, False),
    "Vinh Phuc": ("Vinh Phuc", 21.3089, 105.6049, False),
    "Hai Duong": ("Hai Duong", 20.9373, 106.3146, False),
    "Lai Chau": ("Lai Chau", 22.3964, 103.4582, False),
    "Hoa Binh": ("Hoa Binh", 20.8172, 105.3376, False),
    "Thai Nguyen": ("Thai Nguyen", 21.5942, 105.8482, False),
    "Dak Nong": ("Dak Nong", 12.0042, 107.6907, False),
    "Dien Bien": ("Dien Bien", 21.3860, 103.0230, False),
    "Dong Nai": ("Dong Nai", 10.9574, 106.8427, False),
    "Quang Ngai": ("Quang Ngai", 15.1214, 108.8044, False),
    "Hung Yen": ("Hung Yen", 20.6464, 106.0511, False),
    "Binh Duong": ("Binh Duong", 10.9804, 106.6519, False),
    "Bac Lieu": ("Bac Lieu", 9.2940, 105.7216, False),
    "Soc Trang": ("Soc Trang", 9.6025, 105.9739, False),
    "Nam Dinh": ("Nam Dinh", 20.4388, 106.1621, False),
    "Vinh Long": ("Vinh Long", 10.2537, 105.9722, False),
    "Hau Giang": ("Hau Giang", 9.7845, 105.4701, False),
    "Tra Vinh": ("Tra Vinh", 9.9347, 106.3453, False),
    "Binh Phuoc": ("Binh Phuoc", 11.5349, 106.8832, False),
    "Ha Nam": ("Ha Nam", 20.5411, 105.9139, False),
}

# Tỉnh (phần cuối địa chỉ, đã bỏ dấu, bỏ "thanh pho"/"tinh") -> key điểm đến
PROVINCES = {
    "ho chi minh": "Ho Chi Minh City", "ha noi": "Hanoi", "da nang": "Da Nang", "hue": "Hue",
    "thua thien hue": "Hue", "can tho": "Can Tho", "ninh binh": "Ninh Binh", "tay ninh": "Tay Ninh",
    "an giang": "An Giang", "ben tre": "Ben Tre", "ca mau": "Ca Mau", "ha giang": "Ha Giang",
    "quang ninh": "Quang Ninh", "khanh hoa": "Khanh Hoa", "binh thuan": "Binh Thuan", "lam dong": "Lam Dong",
    "ba ria - vung tau": "Ba Ria - Vung Tau", "nghe an": "Nghe An", "thanh hoa": "Thanh Hoa",
    "quang nam": "Quang Nam", "ha tinh": "Ha Tinh", "son la": "Son La", "dong thap": "Dong Thap",
    "lao cai": "Lao Cai", "hai phong": "Hai Phong", "dac lak": "Dak Lak", "dak lak": "Dak Lak",
    "gia lai": "Gia Lai", "phu tho": "Phu Tho", "tuyen quang": "Tuyen Quang", "cao bang": "Cao Bang",
    "kon tum": "Kon Tum", "phu yen": "Phu Yen", "quang tri": "Quang Tri", "bac ninh": "Bac Ninh",
    "lang son": "Lang Son", "thai binh": "Thai Binh", "bac kan": "Bac Kan", "ninh thuan": "Ninh Thuan",
    "vinh phuc": "Vinh Phuc", "hai duong": "Hai Duong", "lai chau": "Lai Chau", "hoa binh": "Hoa Binh",
    "thai nguyen": "Thai Nguyen", "dac nong": "Dak Nong", "dak nong": "Dak Nong", "dien bien": "Dien Bien",
    "dong nai": "Dong Nai", "quang ngai": "Quang Ngai", "hung yen": "Hung Yen", "binh duong": "Binh Duong",
    "bac lieu": "Bac Lieu", "soc trang": "Soc Trang", "nam dinh": "Nam Dinh", "vinh long": "Vinh Long",
    "hau giang": "Hau Giang", "tra vinh": "Tra Vinh", "binh phuoc": "Binh Phuoc", "ha nam": "Ha Nam",
}

# Đơn vị cấp huyện/thành phố thuộc tỉnh xuất hiện ở cuối địa chỉ -> key tỉnh
DISTRICTS = {
    "sa pa": "Lao Cai", "lao cai": "Lao Cai", "bao thang": "Lao Cai", "bac ha": "Lao Cai",
    "muong khuong": "Lao Cai", "bat xat": "Lao Cai",
    "pleiku": "Gia Lai", "an khe": "Gia Lai", "chu se": "Gia Lai", "kbang": "Gia Lai", "krong pa": "Gia Lai",
    "chu puh": "Gia Lai", "mang yang": "Gia Lai", "chu pah": "Gia Lai", "quy nhon": "Gia Lai",
    "quy nhon nam": "Gia Lai",
    "dong ha": "Quang Tri", "huong hoa": "Quang Tri", "vinh linh": "Quang Tri", "gio linh": "Quang Tri",
    "quang tri": "Quang Tri", "cam lo": "Quang Tri", "hai lang": "Quang Tri", "trieu phong": "Quang Tri",
    "long xuyen": "An Giang", "chau doc": "An Giang", "tinh bien": "An Giang", "chau phu": "An Giang",
    "thoai son": "An Giang",
    "cao lanh": "Dong Thap", "sa dec": "Dong Thap", "hong ngu": "Dong Thap", "thap muoi": "Dong Thap",
    "tan hong": "Dong Thap", "thanh binh": "Dong Thap", "lap vo": "Dong Thap", "lai vung": "Dong Thap",
    "tay ninh": "Tay Ninh", "trang bang": "Tay Ninh", "go dau": "Tay Ninh", "duong minh chau": "Tay Ninh",
    "hoa thanh": "Tay Ninh", "tan bien": "Tay Ninh", "ben cau": "Tay Ninh",
    "bac ninh": "Bac Ninh", "tu son": "Bac Ninh", "yen phong": "Bac Ninh", "thuan thanh": "Bac Ninh",
    "tien du": "Bac Ninh", "gia binh": "Bac Ninh", "luong tai": "Bac Ninh",
}
# Tên huyện trùng ở nhiều tỉnh → lấy tỉnh của bản ghi gần nhất theo id nguồn
AMBIGUOUS_DISTRICTS = {"chau thanh", "tan chau", "cho moi", "tam nong"}

# Từ khoá trong địa chỉ (trừ phần tỉnh) -> điểm đến cụ thể; thứ tự quan trọng (Mũi Né trước Phan Thiết)
KEYWORDS = [
    (r"\bsa ?pa\b", "Sa Pa"), (r"\bbac ha\b", "Bac Ha"), (r"\bhoi an\b", "Hoi An"),
    (r"\bphu quoc\b", "Phu Quoc"), (r"\bcat ba\b", "Cat Ba Island"),
    (r"\b(ha long|bai chay|tuan chau|hon gai)\b", "Ha Long Bay"),
    (r"\bda lat\b", "Da Lat"), (r"\bnha trang\b", "Nha Trang"), (r"\bmui ne\b", "Mui Ne"),
    (r"\bphan thiet\b", "Phan Thiet"), (r"\bcon dao\b", "Con Dao"), (r"\bvung tau\b", "Vung Tau"),
    (r"\bqu[yi] nhon\b", "Quy Nhon"), (r"\btam dao\b", "Tam Dao"), (r"\bmai chau\b", "Mai Chau"),
    (r"\bba vi\b", "Ba Vi"), (r"\bly son\b", "Ly Son Island"), (r"\b(phong nha|bo trach)\b", "Phong Nha"),
]
PREMIUM_DESTINATIONS = {"Ha Long Bay", "Phu Quoc", "Hoi An", "Da Nang", "Nha Trang", "Hanoi",
                        "Ho Chi Minh City", "Sa Pa", "Da Lat", "Con Dao", "Mui Ne"}


def province_of(address):
    last = norm(address.split(",")[-1])
    last = re.sub(r"^(thanh pho|tinh|thi xa|huyen|quan|p\.|phuong)\s+", "", last).strip()
    if last in PROVINCES:
        return PROVINCES[last]
    if last in AMBIGUOUS_DISTRICTS:
        return None
    if last in DISTRICTS:
        return DISTRICTS[last]
    # "219 Trung Kính" & các trường hợp lạ: tìm tên tỉnh ở bất kỳ đâu trong địa chỉ
    full = norm(address)
    for name, key in PROVINCES.items():
        if re.search(r"\b" + re.escape(name) + r"\b", full):
            return key
    if "trung kinh" in full:
        return "Hanoi"
    return None


def destination_of(address, province):
    text = norm(address).replace("ba ria - vung tau", "")  # tên tỉnh cũ chứa "Vũng Tàu"
    for pattern, key in KEYWORDS:
        if re.search(pattern, text):
            return key
    return province


# ---------------------------------------------------------------- loại hình / hạng phòng
def kind_of(rec):
    lh = rec.get("loai_hinh") or ""
    n = norm(rec["name"])
    if "tau thuy" in norm(lh) or re.search(r"\b(du thuyen|cruise|tau)\b", n):
        return "cruise"
    if "biet thu" in norm(lh) or re.search(r"\b(villa|biet thu)\b", n):
        return "villa"
    if "can ho" in norm(lh) or re.search(r"\b(apartment|can ho|condotel)\b", n):
        return "apartment"
    if "cam trai" in norm(lh) or re.search(r"\b(camping|glamping|cam trai)\b", n):
        return "camp"
    if "nha nghi" in norm(lh) or re.search(r"^nha nghi\b", n) or re.search(r"\b(motel|guest ?house)\b", n):
        return "guesthouse"
    if "nha o co phong" in norm(lh) or re.search(r"\bhomestay\b", n):
        return "homestay"
    if re.search(r"\bresort\b|khu nghi duong", n):
        return "resort"
    return "hotel"


KIND_VI = {
    "hotel": "khách sạn", "resort": "khu nghỉ dưỡng", "guesthouse": "nhà nghỉ", "homestay": "homestay",
    "villa": "biệt thự du lịch", "apartment": "căn hộ du lịch", "cruise": "du thuyền lưu trú",
    "camp": "khu cắm trại du lịch",
}

# (tên, sức chứa, giường, diện tích min-max, hệ số giá)
RT = {
    "std_double": ("Standard Double Room", 2, "1 Double Bed", (16, 22), 1.0),
    "std_twin": ("Standard Twin Room", 2, "2 Single Beds", (16, 22), 1.0),
    "superior": ("Superior Room", 2, "1 Queen Bed", (22, 28), 1.25),
    "deluxe": ("Deluxe Room", 2, "1 King Bed", (28, 36), 1.55),
    "deluxe_twin": ("Deluxe Twin Room", 2, "2 Single Beds", (28, 36), 1.55),
    "family": ("Family Room", 4, "2 Double Beds", (32, 42), 1.8),
    "junior_suite": ("Junior Suite", 3, "1 King Bed", (42, 55), 2.3),
    "premier": ("Premier Deluxe Room", 2, "1 King Bed", (35, 45), 1.9),
    "exec_suite": ("Executive Suite", 3, "1 King Bed", (55, 75), 2.8),
    "cabin_deluxe": ("Deluxe Cabin", 2, "1 Double Bed", (20, 28), 1.0),
    "cabin_family": ("Family Cabin", 4, "2 Double Beds", (28, 35), 1.6),
    "cabin_suite": ("Suite Cabin", 2, "1 King Bed", (35, 50), 2.2),
    "villa_2": ("Two-Bedroom Villa", 4, "2 Queen Beds", (90, 140), 1.0),
    "villa_3": ("Three-Bedroom Villa", 6, "3 Queen Beds", (140, 220), 1.5),
    "studio": ("Studio Apartment", 2, "1 Queen Bed", (28, 38), 1.0),
    "apt_1": ("One-Bedroom Apartment", 3, "1 King Bed", (45, 60), 1.4),
    "apt_2": ("Two-Bedroom Apartment", 5, "2 Queen Beds", (70, 95), 2.0),
    "tent": ("Glamping Tent", 2, "1 Double Bed", (12, 20), 1.0),
    "tent_family": ("Family Tent", 4, "2 Double Beds", (20, 30), 1.6),
}


def room_type_keys(kind, star, rng):
    if kind == "cruise":
        return ["cabin_deluxe", "cabin_family", "cabin_suite"] if star >= 3 else ["cabin_deluxe", "cabin_family"]
    if kind == "villa":
        return ["villa_2", "villa_3"]
    if kind == "apartment":
        return ["studio", "apt_1", "apt_2"][: 2 + (star >= 3)]
    if kind == "camp":
        return ["tent", "tent_family"]
    if kind in ("guesthouse", "homestay"):
        return ["std_double", "std_twin"] + (["family"] if rng.random() < 0.5 else [])
    if star == 1:
        return ["std_double", "std_twin"]
    if star == 2:
        return ["std_double", "std_twin"] + (["family"] if rng.random() < 0.6 else [])
    if star == 3:
        return ["std_double", "superior", "deluxe"] + (["family"] if rng.random() < 0.5 else [])
    if star == 4:
        return ["superior", "deluxe", "deluxe_twin", "family"] if rng.random() < 0.5 else \
               ["superior", "deluxe", "family", "junior_suite"]
    return ["deluxe", "premier", "junior_suite", "exec_suite"]


# Giá phòng rẻ nhất (VND/đêm) khi nguồn không có giá
BASE_PRICE_VND = {1: (200_000, 400_000), 2: (300_000, 650_000), 3: (600_000, 1_300_000),
                  4: (1_200_000, 2_500_000), 5: (2_200_000, 5_000_000)}
KIND_PRICE_FACTOR = {"villa": 2.2, "apartment": 1.1, "cruise": 2.5, "camp": 0.7, "homestay": 0.9,
                     "guesthouse": 0.9, "resort": 1.25, "hotel": 1.0}


def parse_price(rec):
    raw = ((rec.get("fields") or {}).get("Giá") or [None])[0]
    if not raw:
        return None
    nums = [int(x.replace(".", "").replace(",", "")) for x in re.findall(r"\d[\d.,]*", raw)]
    nums = [n for n in nums if 100_000 <= n <= 50_000_000]
    if not nums:
        return None
    lo, hi = min(nums), max(nums)
    return lo, min(max(hi, lo), lo * 5)


def star_from_price(lo, rng):
    if lo < 300_000:
        return rng.choice([1, 2])
    if lo < 600_000:
        return rng.choice([1, 2, 3])
    if lo < 1_200_000:
        return rng.choice([2, 3, 4])
    if lo < 2_500_000:
        return rng.choice([3, 4, 5])
    return rng.choice([4, 5])


def round_vnd(v):
    return int(round(v / 10_000) * 10_000)


# ---------------------------------------------------------------- tiện nghi
SERVICE_TO_AMENITY = {
    "wifi": "Free Wifi", "truyen hinh cap": "Flat Screen TV", "giat ui": "Laundry Service",
    "nha hang": "Restaurant", "cho do xe": "Free Parking", "dich vu van phong": "Business Center",
    "phong hoi thao, hoi nghi": "Business Center", "bar": "Bar", "doi ngoai te": "Currency Exchange",
    "massage": "Massage", "be boi": "Swimming Pool", "xe dua don san bay": "Airport Shuttle", "spa": "Spa",
    "xong hoi": "Sauna", "fitness center": "Gym", "san tennis": "Tennis Court",
}
SERVICE_CAR = "cho thue xe"
AMENITY_VI = {
    "Free Wifi": "wifi miễn phí", "Restaurant": "nhà hàng", "Swimming Pool": "bể bơi", "Spa": "spa",
    "Gym": "phòng gym", "Bar": "quầy bar", "Airport Shuttle": "xe đưa đón sân bay", "Massage": "massage",
    "Sauna": "phòng xông hơi", "Free Parking": "chỗ đỗ xe", "Business Center": "phòng hội nghị",
    "Laundry Service": "giặt ủi", "Tennis Court": "sân tennis", "Car Rental": "cho thuê xe",
    "Kids Playground": "khu vui chơi trẻ em", "Room Service": "phục vụ phòng",
}
BED_VI = {
    "1 Double Bed": "1 giường đôi", "2 Single Beds": "2 giường đơn", "1 Queen Bed": "1 giường queen",
    "1 King Bed": "1 giường king", "2 Double Beds": "2 giường đôi", "2 Queen Beds": "2 giường queen",
    "3 Queen Beds": "3 giường queen",
}


def hotel_amenities(rec, star, rng):
    services = [norm(s) for s in rec.get("dich_vu") or []]
    out = {"Air Conditioning", "Private Bathroom", "Daily Housekeeping", "Smoke Detector", "Fire Extinguisher"}
    if services:
        for s in services:
            if s in SERVICE_TO_AMENITY:
                out.add(SERVICE_TO_AMENITY[s])
            elif s.startswith(SERVICE_CAR):
                out.add("Car Rental")
    else:  # nguồn không ghi dịch vụ → sinh theo hạng sao
        out |= {"Free Wifi", "Flat Screen TV"}
        pool = {1: ["Free Parking"], 2: ["Free Parking", "Laundry Service", "Restaurant"],
                3: ["Free Parking", "Laundry Service", "Restaurant", "Airport Shuttle", "Bar"],
                4: ["Restaurant", "Bar", "Swimming Pool", "Spa", "Gym", "Airport Shuttle", "Laundry Service"],
                5: ["Restaurant", "Bar", "Swimming Pool", "Spa", "Gym", "Airport Shuttle", "Laundry Service",
                    "Sauna", "Massage", "Business Center"]}[star]
        out |= {a for a in pool if rng.random() < 0.75}
    if star >= 2:
        out.add("24-Hour Front Desk")
    if star >= 3:
        out |= {"Mini Bar", "Room Service", "Safe Box", "CCTV Security"}
    if star >= 4:
        out |= {"Coffee Maker", "High-Speed Internet"}
    return sorted(out)


def room_type_amenities(key, star, rng):
    out = ["Air Conditioning", "Private Bathroom", "Flat Screen TV", "Free Wifi"]
    if star >= 3:
        out += ["Mini Bar", "Safe Box"]
    if star >= 4:
        out.append("Coffee Maker")
    if key not in ("std_double", "std_twin", "tent", "tent_family") and rng.random() < 0.5:
        out.append("Balcony")
    return out


# ---------------------------------------------------------------- ảnh / mô tả
def clean_images(rec, common):
    """Giữ mọi ảnh nguồn (trừ ảnh dùng chung trong common_images.json); file tên chứa "logo"
    xếp sau để ảnh bìa ưu tiên ảnh chụp, KS chỉ có ảnh đó thì vẫn dùng làm bìa."""
    seen, out = set(), []
    for u in rec.get("images") or []:
        if u in common or u in seen or not u.startswith("http"):
            continue
        seen.add(u)
        out.append(u)
    out.sort(key=lambda u: "logo" in u.rsplit("/", 1)[-1].lower())  # sort ổn định: giữ thứ tự nguồn
    return out[:MAX_IMAGES_PER_HOTEL]


def clean_text(s):
    s = (s or "").replace("\x00", "").replace("\r", "")
    s = re.sub(r"[ \t ]+", " ", s)
    s = re.sub(r"\n\s*\n+", "\n\n", s)
    return s.strip()


def generated_description(name, kind, star, address, rooms, amenities, rng):
    kind_vi = KIND_VI[kind]
    feats = [AMENITY_VI[a] for a in amenities if a in AMENITY_VI]
    rng.shuffle(feats)
    feats = feats[:5]
    feat_txt = (", ".join(feats[:-1]) + " và " + feats[-1]) if len(feats) > 1 else (feats[0] if feats else "")
    opener = rng.choice([
        f"{name} là {kind_vi} {star} sao tọa lạc tại {address}.",
        f"Tọa lạc tại {address}, {name} là {kind_vi} đạt chuẩn {star} sao.",
        f"{name} – {kind_vi} {star} sao nằm tại {address}.",
    ])
    body = f" Cơ sở có {rooms} phòng nghỉ được trang bị đầy đủ tiện nghi"
    body += f", cùng các dịch vụ như {feat_txt}." if feat_txt else "."
    closing = {
        "resort": " Không gian nghỉ dưỡng thoáng đãng, phù hợp cho kỳ nghỉ thư giãn cùng gia đình và bạn bè.",
        "cruise": " Du khách có thể ngắm cảnh, thưởng thức ẩm thực và nghỉ đêm ngay trên du thuyền.",
        "villa": " Biệt thự rộng rãi, riêng tư, lý tưởng cho nhóm bạn hoặc gia đình đông người.",
        "apartment": " Căn hộ có không gian sinh hoạt riêng, thuận tiện cho chuyến đi dài ngày.",
        "camp": " Trải nghiệm cắm trại gần gũi thiên nhiên, thích hợp cho các chuyến đi dã ngoại.",
        "homestay": " Không gian ấm cúng, thân thiện, giúp du khách trải nghiệm đời sống địa phương.",
        "guesthouse": " Lựa chọn tiết kiệm, sạch sẽ và thuận tiện cho khách du lịch.",
    }.get(kind) or (
        " Với dịch vụ chuyên nghiệp và vị trí thuận lợi, đây là lựa chọn đáng cân nhắc cho cả khách công tác lẫn du lịch."
        if star >= 4 else
        " Vị trí thuận tiện để di chuyển tới các điểm tham quan, phù hợp cho khách du lịch và công tác."
    )
    return opener + body + closing


def room_description(rt_name, area, bed, occupancy, hotel_name):
    return (f"Phòng {rt_name} rộng khoảng {area} m², {BED_VI.get(bed, bed)}, "
            f"phù hợp cho tối đa {occupancy} khách tại {hotel_name}.")


# ---------------------------------------------------------------- SQL
def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, (int, float)):
        return repr(v)
    return "'" + str(v).replace("\x00", "").replace("'", "''") + "'"


def write_inserts(out, table, cols, rows, conflict="ON CONFLICT DO NOTHING", chunk=500):
    for i in range(0, len(rows), chunk):
        part = rows[i:i + chunk]
        out.write(f"INSERT INTO {table} ({', '.join(cols)}) VALUES\n")
        out.write(",\n".join("(" + ", ".join(r) + ")" for r in part))
        out.write(f"\n{conflict};\n")


def main(src_dir, out_path):
    common = set(json.load(open(os.path.join(src_dir, "common_images.json"), encoding="utf-8")))
    records = [json.loads(l) for l in open(os.path.join(src_dir, "cslt", "data.jsonl"), encoding="utf-8")]
    records.sort(key=lambda r: int(r["id"]))

    # 1. tỉnh của từng bản ghi (huyện trùng tên → tỉnh của bản ghi gần nhất theo id)
    addr = {}
    prov = {}
    for r in records:
        a = clean_text(((r.get("fields") or {}).get("Địa chỉ") or [""])[0])
        if a:
            addr[r["id"]] = a
            prov[r["id"]] = province_of(a)
    resolved = [int(i) for i, p in prov.items() if p]
    for i, p in list(prov.items()):
        if p is None and resolved:
            nearest = min(resolved, key=lambda x: abs(x - int(i)))
            prov[i] = prov[str(nearest)]

    # 2. ảnh bìa cho điểm đến mới: ảnh từ bộ "Điểm đến" cùng tỉnh
    dest_cover = {}
    dest_file = os.path.join(src_dir, "dest", "data.jsonl")
    if os.path.exists(dest_file):
        for d in sorted((json.loads(l) for l in open(dest_file, encoding="utf-8")),
                        key=lambda d: -len(d.get("description") or "")):
            a = ((d.get("fields") or {}).get("Địa chỉ") or [""])[0]
            imgs = clean_images(d, common)
            if a and imgs:
                p = province_of(a)
                if p and p not in dest_cover:
                    dest_cover[p] = imgs[0]

    hotels, rts, rooms, images, h_amen, rt_amen = [], [], [], [], [], []
    seen = set()
    used_dest = {}
    stats = {"skipped_no_address": 0, "skipped_duplicate": 0, "skipped_no_province": 0,
             "star_random": 0, "price_random": 0, "desc_generated": 0, "image_default": 0}

    for r in records:
        sid = r["id"]
        name = clean_text(r.get("name"))[:200]
        address = addr.get(sid)
        if not address or not name:
            stats["skipped_no_address"] += 1
            continue
        dup_key = (norm(name), norm(address))
        if dup_key in seen:
            stats["skipped_duplicate"] += 1
            continue
        seen.add(dup_key)
        province = prov.get(sid)
        if not province:
            stats["skipped_no_province"] += 1
            continue
        dest = destination_of(address, province)
        kind = kind_of(r)
        if kind == "cruise" and province == "Quang Ninh" and dest == "Quang Ninh":
            dest = "Ha Long Bay"
        rng = random.Random(f"tripnova-cslt-{sid}")
        hid = uid("cslt", sid)

        # hạng sao
        m = re.match(r"(\d)", r.get("hang_sao") or "")
        price = parse_price(r)
        if m:
            star = max(1, min(5, int(m.group(1))))
        else:
            stats["star_random"] += 1
            if kind in ("guesthouse", "homestay", "camp"):
                star = rng.choice([1, 2])
            elif price:
                star = star_from_price(price[0], rng)
            else:
                star = rng.choices([1, 2, 3, 4, 5], weights=[15, 30, 30, 15, 10])[0]

        # hạng phòng + giá
        keys = room_type_keys(kind, star, rng)
        mults = [RT[k][4] for k in keys]
        if price:
            lo, hi = price
            if hi <= lo:
                hi = lo * max(mults)
        else:
            stats["price_random"] += 1
            a, b = BASE_PRICE_VND[star]
            lo = rng.uniform(a, b) * KIND_PRICE_FACTOR[kind] * (1.15 if dest in PREMIUM_DESTINATIONS else 1.0)
            hi = lo * max(mults)
        span = max(mults) - min(mults)

        # số phòng thực tế (nguồn hoặc ngẫu nhiên), giới hạn số bản ghi `rooms` sinh ra
        try:
            src_rooms = int(float(((r.get("fields") or {}).get("Số phòng") or ["0"])[0]))
        except ValueError:
            src_rooms = 0
        if not 1 <= src_rooms <= 2000:
            src_rooms = {1: rng.randint(6, 15), 2: rng.randint(10, 25), 3: rng.randint(20, 45),
                         4: rng.randint(40, 120), 5: rng.randint(80, 300)}[star]
            if kind in ("villa", "camp"):
                src_rooms = rng.randint(2, 8)
            elif kind == "cruise":
                src_rooms = rng.randint(6, 30)
        n_rooms = max(len(keys), min(src_rooms, MAX_ROOMS_PER_HOTEL))

        imgs = clean_images(r, common)
        cover = imgs[0] if imgs else DEFAULT_HOTEL_IMAGE
        if not imgs:
            stats["image_default"] += 1

        amen = hotel_amenities(r, star, rng)
        desc = clean_text(r.get("description"))
        if len(desc) < 60:
            stats["desc_generated"] += 1
            desc = generated_description(name, kind, star, address, src_rooms, amen, rng)

        fields = r.get("fields") or {}
        phone = next((p.strip() for p in (fields.get("Điện thoại cố định") or []) + (fields.get("Điện thoại di động") or [])
                      if p and len(p.strip()) <= 20), None)
        email = next((e.strip() for e in fields.get("Email") or [] if "@" in e and len(e.strip()) <= 255), None)
        if star <= 2:
            policy, hours = "free", 24
        elif star == 3:
            policy, hours = rng.choice([("free", 48), ("free", 48), ("partial", 48)])
        else:
            policy, hours = rng.choice([("free", 48), ("partial", 72), ("partial", 72), ("strict", 72)])
        breakfast = rng.random() < (0.75 if star >= 3 and "Restaurant" in amen else 0.25)
        pets = rng.random() < 0.08

        if not used_dest.get(dest):  # ảnh khách sạn đầu tiên có ảnh thật → ảnh dự phòng cho điểm đến mới
            used_dest[dest] = imgs[0] if imgs else None
        hotels.append([q(hid), f"(SELECT id FROM vn_dest WHERE key = {q(dest)})", q(name), q(desc), q(address),
                       q(star), q("14:00"), q("12:00"), q(phone), q(email), q(policy), q(hours), q(breakfast),
                       q(pets), q(n_rooms), q(cover), "true"])
        for a in amen:
            h_amen.append([q(hid), q(a)])
        for i, img in enumerate(imgs):
            images.append([q(uid("cslt", sid, "img", i)), q(hid), q(img), "NULL", q(i)])

        # chia phòng: hạng rẻ nhiều phòng hơn
        weights = list(range(len(keys), 0, -1))
        counts = [1] * len(keys)
        for _ in range(n_rooms - len(keys)):
            counts[rng.choices(range(len(keys)), weights=weights)[0]] += 1
        room_no = 0
        for i, k in enumerate(keys):
            rt_name, occ, bed, (amin, amax), mult = RT[k]
            vnd = lo if span == 0 else lo + (hi - lo) * (mult - min(mults)) / span
            usd = round(round_vnd(vnd) / VND_PER_USD, 2)
            area = rng.randint(amin, amax)
            rtid = uid("cslt", sid, "rt", k)
            rt_cover = imgs[(i + 1) % len(imgs)] if len(imgs) > 1 else DEFAULT_ROOM_IMAGE
            rts.append([q(rtid), q(hid), q(rt_name), q(room_description(rt_name, area, bed, occ, name)), q(occ),
                        q(bed), q(area), q(usd), q(rt_cover), "true"])
            for a in room_type_amenities(k, star, rng):
                rt_amen.append([q(rtid), q(a)])
            for _ in range(counts[i]):
                floor = room_no // 10 + 1
                number = f"{'C' if kind == 'cruise' else ''}{floor}{room_no % 10 + 1:02d}"
                rooms.append([q(uid("cslt", sid, "room", number)), q(rtid), q(number), q(floor), "'available'"])
                room_no += 1

    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        out.write("-- Sinh bởi db/import/import_vn_hotels.py — KHÔNG sửa tay.\nSET client_encoding = 'UTF8';\n\n")
        out.write("""INSERT INTO countries (id, continent_id, country_code, name, slug, image_url, description, latitude, longitude)
SELECT %s, c.id, 'VNM', 'Vietnam', 'vietnam', 'https://flagcdn.com/w320/vn.png', 'Country in Asia', 14.0583, 108.2772
FROM continents c WHERE c.code = 'AS'
  AND NOT EXISTS (SELECT 1 FROM countries WHERE country_code = 'VNM' OR slug = 'vietnam');

""" % q(uid("country", "VNM")))
        dest_rows = []
        for key in sorted(used_dest):
            name, lat, lng, popular = DESTINATIONS[key]
            img = dest_cover.get(key) or used_dest.get(key) or DEFAULT_HOTEL_IMAGE
            desc = f"Discover {name}, Vietnam: local stays, culture, food and landscapes."
            dest_rows.append(f"({q(uid('dest', key))}, {q(key)}, {q(name)}, {q(desc)}, {q(img)}, {q(popular)}, {lat}, {lng})")
        out.write("""INSERT INTO destinations (id, country_id, name, description, cover_image_url, is_popular, is_active, latitude, longitude)
SELECT v.id::uuid, c.id, v.name, v.description, v.cover, v.popular, true, v.lat, v.lng
FROM (VALUES
%s
) v(id, key, name, description, cover, popular, lat, lng)
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

        write_inserts(out, "hotels", ["id", "destination_id", "name", "description", "address", "star_rating",
                                      "check_in_time", "check_out_time", "phone", "email", "cancellation_policy",
                                      "cancellation_hours", "breakfast_included", "pet_friendly", "total_rooms",
                                      "cover_image_url", "is_active"], hotels, "ON CONFLICT (id) DO NOTHING")
        write_inserts(out, "room_types", ["id", "hotel_id", "name", "description", "max_occupancy", "bed_type",
                                          "area_sqm", "price_per_night", "cover_image_url", "is_active"], rts,
                      "ON CONFLICT (id) DO NOTHING")
        write_inserts(out, "rooms", ["id", "room_type_id", "room_number", "floor", "status"], rooms, chunk=2000)
        write_inserts(out, "hotel_images", ["id", "hotel_id", "url", "caption", "sort_order"], images,
                      "ON CONFLICT (id) DO NOTHING")
        for table, col, data in (("hotel_amenities", "hotel_id", h_amen), ("room_type_amenities", "room_type_id", rt_amen)):
            for i in range(0, len(data), 2000):
                part = data[i:i + 2000]
                out.write(f"INSERT INTO {table} ({col}, amenity_id)\nSELECT v.ref::uuid, a.id FROM (VALUES\n")
                out.write(",\n".join(f"({x[0]}, {x[1]})" for x in part))
                out.write(f"\n) v(ref, amenity) JOIN amenities a ON a.name = v.amenity\nON CONFLICT DO NOTHING;\n")

    stats.update(hotels=len(hotels), room_types=len(rts), rooms=len(rooms), images=len(images),
                 destinations=len(used_dest))
    print(json.dumps(stats, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
