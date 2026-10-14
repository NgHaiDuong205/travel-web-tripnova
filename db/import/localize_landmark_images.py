#!/usr/bin/env python3
"""
Tải ảnh bìa địa danh đang hotlink Wikimedia Commons về FE public/images/landmarks (Wikimedia chặn 429 khi trình duyệt
tải nhiều ảnh cùng lúc), ghi CREDITS.json (tác giả, giấy phép, link gốc) và sinh SQL đổi cover_image_url sang đường dẫn tĩnh.

    python db/import/localize_landmark_images.py <landmarks.csv> <fe_public_dir> <out.sql>

landmarks.csv = id,cover_image_url (chỉ các dòng cover_image_url LIKE 'https://upload.wikimedia.org/%')
Chạy lại được: ảnh đã tải thì bỏ qua; ảnh lỗi giữ nguyên URL cũ (không có trong SQL).
"""
import csv
import io
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

UA = "TripNovaImporter/1.0 (travel demo project; contact: admin@tripnova.local)"
WIDTH = 800


def get(url, tries=6):
    for i in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=40) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code == 404 or i == tries - 1:
                raise
            time.sleep(90 if e.code == 429 else 5 * (i + 1))
        except Exception:
            if i == tries - 1:
                raise
            time.sleep(5 * (i + 1))


def strip_html(s):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", s or "")).strip()


def file_title(url):
    # .../thumb/a/ab/<File>/960px-<File>
    m = re.search(r"/commons/thumb/[0-9a-f]/[0-9a-f]{2}/([^/]+)/", url)
    return "File:" + urllib.parse.unquote(m.group(1)).replace("_", " ") if m else None


def main(csv_path, fe_public, out_path):
    from PIL import Image
    rows = [r for r in csv.reader(open(csv_path, encoding="utf-8")) if len(r) == 2]
    root = os.path.join(fe_public, "images", "landmarks")
    os.makedirs(root, exist_ok=True)
    credits_path = os.path.join(root, "CREDITS.json")
    credits = json.load(open(credits_path, encoding="utf-8")) if os.path.exists(credits_path) else {}

    titles = {lid: file_title(url) for lid, url in rows}
    info = {}
    uniq = sorted({t for t in titles.values() if t})
    for i in range(0, len(uniq), 50):
        d = json.loads(get("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({
            "action": "query", "format": "json", "titles": "|".join(uniq[i:i + 50]), "prop": "imageinfo",
            "iiprop": "url|extmetadata", "iiurlwidth": WIDTH})))
        q = d.get("query") or {}
        alias = {x["from"]: x["to"] for x in q.get("normalized") or []}
        by_title = {p.get("title"): p for p in (q.get("pages") or {}).values()}
        for t in uniq[i:i + 50]:
            p = by_title.get(alias.get(t, t)) or {}
            ii = (p.get("imageinfo") or [{}])[0]
            if ii.get("thumburl"):
                meta = ii.get("extmetadata") or {}
                info[t] = {"thumb": ii["thumburl"], "source": ii.get("descriptionurl"),
                           "creator": strip_html((meta.get("Artist") or {}).get("value"))[:200],
                           "license": strip_html((meta.get("LicenseShortName") or {}).get("value"))}
        time.sleep(1)
    print("imageinfo", len(info), "/", len(uniq), flush=True)

    updates = []
    for n, (lid, url) in enumerate(rows):
        t = titles.get(lid)
        meta = info.get(t)
        if not meta:
            continue
        rel = f"/images/landmarks/{lid}.jpg"
        out = os.path.join(root, f"{lid}.jpg")
        if not os.path.exists(out):
            try:
                data = get(meta["thumb"])
                im = Image.open(io.BytesIO(data)).convert("RGB")
                im.thumbnail((WIDTH, 600))
                im.save(out, "JPEG", quality=72, optimize=True, progressive=True)
            except Exception as e:
                print("FAIL", lid, t, e, flush=True)
                continue
            time.sleep(1.5)  # tải chậm để không bị chặn
        credits[rel] = {"creator": meta["creator"], "license": meta["license"], "source": meta["source"], "title": t}
        updates.append((lid, url, rel))
        if n % 50 == 0:
            print("downloaded", n + 1, "/", len(rows), flush=True)

    json.dump(dict(sorted(credits.items())), open(credits_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("-- Sinh bởi db/import/localize_landmark_images.py — KHÔNG sửa tay.\nSET client_encoding = 'UTF8';\n")
        for lid, url, rel in updates:
            f.write("UPDATE landmarks SET cover_image_url = '%s' WHERE id = '%s' AND cover_image_url = '%s';\n"
                    % (rel, lid, url.replace("'", "''")))
    print("updates", len(updates), flush=True)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
