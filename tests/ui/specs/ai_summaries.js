// Tóm tắt review bằng AI: khối "Tóm tắt từ AI" trên trang khách sạn (VI / EN / tối / điện thoại) + trang admin Job AI.
// Ghi dữ liệu: bật tạm "Claude Test Hotel B", admin viết 3 review (duyệt luôn) rồi xoá đúng 3 review đó, tắt lại khách sạn.
// Cần dịch vụ AI (Python) chạy; hết hạn mức AI thì spec dừng sớm với thông báo.
const { FE, ADMIN, check, api, login, useAuth, launch, watch, shot, run } = require("../lib");

const REVIEWS = [
    [5, "Phòng sạch, nhân viên lễ tân rất thân thiện. Vị trí gần biển, đi bộ 5 phút."],
    [4, "Bữa sáng ngon nhưng ít món. Phòng rộng, giường êm. Wifi đôi lúc chập chờn."],
    [4, "Hồ bơi đẹp, phù hợp gia đình có trẻ nhỏ. Bãi đỗ xe hơi chật."],
];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function setHotelActive(auth, hotel, active) {
    const keys = ["destinationId", "name", "description", "address", "starRating", "checkInTime", "checkOutTime", "phone",
        "email", "cancellationPolicy", "cancellationHours", "breakfastIncluded", "petFriendly", "coverImageUrl",
        "latitude", "longitude", "managedById"];
    const body = Object.fromEntries(keys.map((k) => [k, hotel[k] ?? null]));
    for (const k of ["checkInTime", "checkOutTime"]) if (body[k]) body[k] = body[k].slice(0, 5);
    body.isActive = active;
    await api(`/api/admin/hotels/${hotel.id}/`, { method: "PUT", body: JSON.stringify(body), headers: { "Content-Type": "application/json", Authorization: `Bearer ${auth.accessToken}` } });
}

/** Chờ hết job pending/running của khách sạn; trả lỗi gần nhất nếu hết hạn mức. */
async function waitJobs(auth, hotelId, timeoutMs = 150000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const jobs = await api(`/api/admin/ai/jobs/?type=summarize_reviews&entityId=${hotelId}&limit=50`,
            { headers: { Authorization: `Bearer ${auth.accessToken}` } });
        const open = jobs.filter((j) => j.status === "pending" || j.status === "running");
        if (!open.length) return null;
        const quota = open.find((j) => (j.errorMessage || "").startsWith("ai_quota"));
        if (quota) return "ai_quota";
        await sleep(2000);
    }
    return "timeout";
}

run(async () => {
    const adminAuth = await login(ADMIN);
    const auth = { headers: { Authorization: `Bearer ${adminAuth.accessToken}` } };
    const found = (await api("/api/admin/hotels/?q=Claude%20Test%20Hotel%20B&page=1&limit=5", auth)) || [];
    const hotelRef = found.find((h) => h.name === "Claude Test Hotel B");
    check(!!hotelRef, "tìm thấy Claude Test Hotel B");
    const hotel = await api(`/api/admin/hotels/${hotelRef.id}/`, auth);
    const wasActive = hotel.isActive;
    const postIds = [];
    const browser = await launch();
    try {
        await setHotelActive(adminAuth, hotel, true);
        for (const [rating, content] of REVIEWS) {
            const post = await api("/api/posts/", { method: "POST", ...auth, headers: { ...auth.headers, "Content-Type": "application/json" },
                body: JSON.stringify({ entityType: "hotel", entityId: hotel.id, rating, content }) });
            if (!post || !post.id) throw new Error(`không tạo được review: ${JSON.stringify(post)}`);
            postIds.push(post.id);
        }
        const waitResult = await waitJobs(adminAuth, hotel.id);
        if (waitResult) {
            check(false, `job tóm tắt chưa xong: ${waitResult}`);
            return;
        }
        const vi = await api(`/api/ai/summaries/?entityType=hotel&entityId=${hotel.id}&lang=vi`);
        const en = await api(`/api/ai/summaries/?entityType=hotel&entityId=${hotel.id}&lang=en`);
        check(vi && vi.overview && en && en.overview, "API: có bản tóm tắt vi + en");

        // ---------- khách: trang khách sạn tiếng Việt
        const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
        await ctx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
        const page = await ctx.newPage();
        const w = watch(page);
        await page.goto(`${FE}/hotel/${hotel.id}`, { waitUntil: "networkidle" });
        const box = page.getByTestId("ai-review-summary");
        await box.waitFor({ timeout: 15000 });
        await box.scrollIntoViewIfNeeded();
        await sleep(800); // hiệu ứng hiện dần
        check(await box.isVisible(), "VI: khối Tóm tắt từ AI hiển thị trong phần đánh giá");
        const text = await box.innerText();
        check(text.includes("Tóm tắt từ AI") && text.includes(vi.overview), "VI: tiêu đề + tổng quan đúng bản tiếng Việt");
        check(text.includes("Dựa trên 3 đánh giá"), "VI: hiện số đánh giá làm cơ sở");
        check((await box.locator("li").count()) === vi.pros.length + vi.cons.length, `VI: đủ ${vi.pros.length} điểm khen + ${vi.cons.length} lưu ý`);
        check(!text.includes("Đang cập nhật"), "VI: chưa có review mới nên không báo đang cập nhật");
        await box.screenshot({ path: shot("ai_summary_vi.png") });

        // EN: đổi ngôn ngữ → tải lại bản tiếng Anh
        await page.evaluate(() => localStorage.setItem("tripnova.lang", "en"));
        await ctx.addInitScript(() => localStorage.setItem("tripnova.lang", "en"));
        await page.reload({ waitUntil: "networkidle" });
        await box.waitFor({ timeout: 15000 });
        const textEn = await box.innerText();
        check(textEn.includes("AI review summary") && textEn.includes(en.overview), "EN: tiêu đề + tổng quan tiếng Anh");
        check(textEn.includes("Based on 3 reviews"), "EN: số nhiều đúng (Based on 3 reviews)");

        // Tối
        await page.evaluate(() => localStorage.setItem("tripnova.theme", "dark"));
        await page.reload({ waitUntil: "networkidle" });
        await box.waitFor({ timeout: 15000 });
        await box.scrollIntoViewIfNeeded();
        await sleep(800);
        const colors = await box.evaluate((el) => {
            const p = el.querySelector("p");
            const s = getComputedStyle(p);
            return { color: s.color, bg: getComputedStyle(document.body).backgroundColor };
        });
        check(colors.color !== colors.bg, `tối: chữ khác màu nền (${colors.color} / ${colors.bg})`);
        await box.screenshot({ path: shot("ai_summary_dark.png") });
        check(w.errors().length === 0, `khách: không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
        check(w.serverErrors().length === 0, "khách: không 5xx");

        // Khách sạn chưa có tóm tắt: không hiện khối, không lỗi
        const others = await api("/api/hotels/?page=1&limit=1");
        await page.goto(`${FE}/hotel/${others[0].id}`, { waitUntil: "networkidle" });
        check((await page.getByTestId("ai-review-summary").count()) === 0, "khách sạn chưa có tóm tắt: không hiện khối");

        // ---------- điện thoại
        const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
        const mp = await mobile.newPage();
        await mp.goto(`${FE}/hotel/${hotel.id}`, { waitUntil: "networkidle" });
        const mbox = mp.getByTestId("ai-review-summary");
        await mbox.waitFor({ timeout: 15000 });
        await mbox.scrollIntoViewIfNeeded();
        await sleep(800);
        const mrect = await mbox.boundingBox();
        check(mrect && mrect.x >= 0 && mrect.x + mrect.width <= 391, `điện thoại: khối nằm trong màn hình (${mrect && Math.round(mrect.width)}px)`);
        const overflow = await mp.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
        check(overflow <= 1, `điện thoại: không cuộn ngang (${overflow}px)`);
        await mbox.screenshot({ path: shot("ai_summary_mobile.png") });

        // ---------- admin: trang Job AI
        const adminCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
        await useAuth(adminCtx, adminAuth);
        await adminCtx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
        const ap = await adminCtx.newPage();
        const aw = watch(ap);
        await ap.goto(`${FE}/admin/ai-jobs`, { waitUntil: "networkidle" });
        check(await ap.getByRole("link", { name: /Job AI/ }).first().isVisible(), "admin: menu có mục Job AI");
        check(await ap.getByTestId("ai-job-stats").isVisible(), "admin: hiện số job theo trạng thái");
        const table = ap.getByTestId("ai-jobs-table");
        check((await table.innerText()).includes("Claude Test Hotel B"), "admin: bảng job có khách sạn test");
        await ap.locator("select").first().selectOption("done");
        await ap.waitForLoadState("networkidle");
        const statuses = await table.locator("tbody tr td:nth-child(4)").allInnerTexts();
        check(statuses.length > 0 && statuses.every((s) => s.includes("Hoàn tất")), "admin: lọc trạng thái Hoàn tất");
        await ap.screenshot({ path: shot("ai_jobs_admin.png") });

        await ap.getByRole("button", { name: "Tóm tắt đánh giá" }).click();
        await ap.waitForLoadState("networkidle");
        check(ap.url().includes("tab=summaries"), "admin: tab tóm tắt nhớ trên URL");
        const card = ap.getByTestId("ai-summaries").locator("article").filter({ hasText: "Claude Test Hotel B" }).first();
        await card.waitFor({ timeout: 10000 });
        check((await card.innerText()).includes(vi.overview) || (await card.innerText()).includes(en.overview), "admin: thẻ tóm tắt có nội dung");
        await card.getByRole("button", { name: "Tạo lại" }).click();
        await ap.getByText("Đã xếp job tạo lại.").waitFor({ timeout: 10000 });
        check(true, "admin: Tạo lại → thông báo đã xếp job");
        await ap.screenshot({ path: shot("ai_summaries_admin.png") });
        check(aw.errors().length === 0, `admin: không lỗi console (${aw.errors().slice(0, 3).join(" | ")})`);
        check(aw.serverErrors().length === 0, "admin: không 5xx");
        await waitJobs(adminAuth, hotel.id);
    } finally {
        for (const id of postIds) {
            await fetch(`${require("../lib").BE}/api/posts/${id}/`, { method: "DELETE", headers: auth.headers });
        }
        const left = await waitJobs(adminAuth, hotel.id);
        const after = await fetch(`${require("../lib").BE}/api/ai/summaries/?entityType=hotel&entityId=${hotel.id}`);
        check(after.status === 204, `dọn dẹp: xoá review test → không còn tóm tắt (${after.status}, job: ${left || "xong"})`);
        if (!wasActive) await setHotelActive(adminAuth, hotel, false);
        await browser.close();
    }
});
