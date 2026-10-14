// Ảnh khách sạn / hạng phòng (kho ảnh stock) + điểm tham quan OSM + quản lý ảnh hạng phòng ở admin.
// Chỉ đọc dữ liệu thật; thao tác ghi chỉ trên "Claude Test Hotel B" (ảnh test tự thêm rồi tự xoá).
const { FE, ADMIN, check, api, login, useAuth, launch, watch, shot, run } = require("../lib");

/** Ảnh đã tải xong và có kích thước thật (không vỡ). */
async function imageOk(locator) {
    await locator.first().waitFor({ state: "visible", timeout: 15000 });
    return locator.first().evaluate((img) => new Promise((resolve) => {
        const done = () => resolve(img.complete && img.naturalWidth > 0);
        if (img.complete) done(); else { img.addEventListener("load", done); img.addEventListener("error", done); }
    }));
}

run(async () => {
    const browser = await launch();
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();
    const w = watch(page);

    // ---------- chọn khách sạn VN đang hoạt động có phòng
    const hotels = await api("/api/hotels/?page=1&limit=40&name=Hoi%20An");
    let hotel = null;
    let rooms = [];
    for (const h of hotels || []) {
        const r = await api(`/api/hotels/${h.id}/rooms/`);
        if (Array.isArray(r) && r.length) { hotel = h; rooms = r; break; }
    }
    check(!!hotel, `tìm được khách sạn có phòng (${hotel && hotel.name})`);
    const images = await api(`/api/hotels/${hotel.id}/images/`);
    check(images.length >= 6, `API: khách sạn có ${images.length} ảnh (≥ 6)`);

    // ---------- trang khách sạn: hero + lightbox
    await page.goto(`${FE}/hotel/${hotel.id}`, { waitUntil: "networkidle" });
    check(await imageOk(page.locator("main img")), "trang khách sạn: ảnh hero tải được");
    const viewAll = page.getByText(/View all \d+ photos|Xem tất cả \d+ ảnh/);
    check(await viewAll.count() > 0, "nút xem tất cả ảnh");
    await viewAll.first().click();
    const dialog = page.locator('[role="dialog"]');
    await dialog.waitFor();
    const counter = await dialog.locator("span").first().innerText();
    const total = Number((counter.match(/\/\s*(\d+)/) || [])[1]);
    check(total >= 6, `lightbox khách sạn có ${total} ảnh`);
    check(await imageOk(dialog.locator("img")), "lightbox: ảnh hiển thị");
    const caption = await dialog.locator("figcaption").allInnerTexts();
    await page.screenshot({ path: shot("images_hotel_lightbox.png") });
    // chuyển tới ảnh minh hoạ (stock) để thấy ghi nguồn
    let sawCredit = caption.some((c) => /minh hoạ/i.test(c));
    for (let i = 0; i < total && !sawCredit; i += 1) {
        await page.keyboard.press("ArrowRight");
        sawCredit = (await dialog.locator("figcaption").allInnerTexts()).some((c) => /minh hoạ/i.test(c));
    }
    check(sawCredit, "ảnh minh hoạ có ghi tác giả / giấy phép");
    await page.keyboard.press("Escape");

    // ---------- trang phòng: 3 ô ảnh khác nhau + lightbox ≥ 2 ảnh có caption
    const room = rooms[0];
    const detail = await api(`/api/hotels/${hotel.id}/rooms/${room.id}/`);
    check((detail.images || []).length >= 2 && detail.imageUrl, `API phòng: ${detail.images.length} ảnh + imageUrl`);
    await page.goto(`${FE}/hotel/${hotel.id}/room/${room.id}`, { waitUntil: "networkidle" });
    const heroImgs = page.locator("main .grid img");
    check(await imageOk(heroImgs), "trang phòng: ảnh hero tải được");
    const srcs = await heroImgs.evaluateAll((els) => els.slice(0, 3).map((e) => e.getAttribute("src")));
    check(srcs.every((s) => s && !/googleusercontent/.test(s)), "không còn ảnh Google cứng");
    check(new Set(srcs).size >= 2, `ô ảnh khác nhau (${new Set(srcs).size})`);
    check(srcs[0] === detail.images[0].url, "ảnh lớn = ảnh đầu của hạng phòng");
    await page.screenshot({ path: shot("images_room.png") });
    await page.getByText(/View all \d+ photos|Xem tất cả \d+ ảnh|View Gallery|Xem thư viện ảnh/).first().click();
    await dialog.waitFor();
    const roomCounter = await dialog.locator("span").first().innerText();
    check(Number((roomCounter.match(/\/\s*(\d+)/) || [])[1]) >= 2, `lightbox phòng: ${roomCounter}`);
    check((await dialog.locator("figcaption").allInnerTexts()).some((c) => /minh hoạ/i.test(c)), "lightbox phòng có ghi nguồn");
    await page.keyboard.press("Escape");

    // ---------- điểm tham quan mới từ OSM (ảnh tải về FE, không hotlink Wikimedia)
    const landmarks = await api("/api/landmarks/?page=1&limit=200&category=museum");
    const local = (landmarks || []).find((l) => (l.coverImageUrl || "").startsWith("/images/landmarks/"));
    check(!!local, `có địa danh dùng ảnh tĩnh FE (${local && local.name})`);
    if (local) {
        await page.goto(`${FE}/destinations/${local.destinationId}/landmarks/${local.id}`, { waitUntil: "networkidle" });
        check(await imageOk(page.locator(`img[src="${local.coverImageUrl}"]`)), "trang địa danh: ảnh tải được");
        await page.screenshot({ path: shot("images_landmark.png") });
    }
    const wikimediaLeft = (landmarks || []).filter((l) => (l.coverImageUrl || "").includes("upload.wikimedia.org"));
    check(wikimediaLeft.length === 0, `không còn hotlink Wikimedia (${wikimediaLeft.length})`);

    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, `không 5xx (${w.serverErrors().join(", ")})`);

    // ---------- admin: thư viện ảnh hạng phòng
    const adminAuth = await login(ADMIN);
    const adminCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await useAuth(adminCtx, adminAuth);
    const ap = await adminCtx.newPage();
    const aw = watch(ap);
    const testHotels = await api("/api/admin/hotels/?q=Claude%20Test%20Hotel%20B&page=1&limit=5",
        { headers: { Authorization: `Bearer ${adminAuth.accessToken}` } });
    const testHotel = (testHotels || []).find((h) => h.name === "Claude Test Hotel B");
    check(!!testHotel, "tìm thấy Claude Test Hotel B");
    await ap.goto(`${FE}/admin/hotels/${testHotel.id}`, { waitUntil: "networkidle" });
    const photosBtn = ap.getByRole("button", { name: /^(Photos|Ảnh)$/ }).first();
    await photosBtn.click();
    const urlInput = ap.locator('input[type="url"][placeholder="https://"]').last();
    await urlInput.waitFor();
    const before = await ap.locator("article img").count();
    const testUrl = "/images/stock/room_double/" + (detail.images[0].url.split("/").pop());
    await urlInput.fill(testUrl);
    await ap.getByRole("button", { name: /Add URL|Thêm URL|Thêm từ URL/i }).last().click();
    await ap.waitForFunction((n) => document.querySelectorAll("article img").length === n + 1, before);
    check(true, "admin thêm ảnh hạng phòng bằng URL /images/stock/...");
    await ap.screenshot({ path: shot("images_admin_roomtype.png") });
    ap.once("dialog", (d) => d.accept());
    await ap.locator("article").filter({ has: ap.locator(`img[src="${testUrl}"]`) }).last()
        .getByRole("button", { name: /^(Delete|Xoá|Xóa)$/ }).click();
    await ap.waitForFunction((n) => document.querySelectorAll("article img").length === n, before);
    check(true, "admin xoá ảnh test");
    check(aw.errors().length === 0, `admin: không lỗi console (${aw.errors().slice(0, 3).join(" | ")})`);
    check(aw.serverErrors().length === 0, "admin: không 5xx");

    // ---------- điện thoại: trang phòng
    const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
    const mp = await mobile.newPage();
    await mp.goto(`${FE}/hotel/${hotel.id}/room/${room.id}`, { waitUntil: "networkidle" });
    check(await imageOk(mp.locator("main .grid img")), "điện thoại: ảnh phòng tải được");
    const overflow = await mp.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    check(overflow <= 1, `điện thoại: không cuộn ngang (${overflow}px)`);

    await browser.close();
});
