// Gợi ý cá nhân hoá: "Dành cho bạn" trên trang chủ (đăng nhập / chưa đăng nhập, VI / EN, điện thoại),
// "Khách sạn tương tự" trên trang khách sạn, trang chi tiết ghi lượt xem (POST /api/interactions/).
// Ghi dữ liệu: vài lượt xem của tài khoản test.
const { FE, USER, check, api, login, useAuth, launch, watch, shot, run } = require("../lib");

run(async () => {
    const auth = await login(USER, true);
    const browser = await launch();

    // ---------- khách chưa đăng nhập: không có khối Dành cho bạn
    const guest = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const gp = await guest.newPage();
    await gp.goto(`${FE}/`, { waitUntil: "networkidle" });
    check((await gp.getByTestId("for-you").count()) === 0, "chưa đăng nhập: không hiện Dành cho bạn");

    // ---------- đã đăng nhập (VI)
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await useAuth(ctx, auth);
    await ctx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    const page = await ctx.newPage();
    const w = watch(page);
    await page.goto(`${FE}/`, { waitUntil: "networkidle" });
    const section = page.getByTestId("for-you");
    await section.waitFor({ timeout: 15000 });
    await section.scrollIntoViewIfNeeded();
    await page.waitForTimeout(1000); // hiệu ứng hiện dần
    const cards = section.getByTestId("recommendation-card");
    const count = await cards.count();
    check(count >= 4 && count <= 8, `trang chủ: Dành cho bạn có ${count} thẻ`);
    const text = await section.innerText();
    check(/Dành cho bạn|Gợi ý nổi bật/.test(text), "tiêu đề tiếng Việt");
    check(/Cho chuyến đi|Vì bạn quan tâm|Giống nơi bạn thích|Phổ biến/.test(text), "thẻ có lý do gợi ý");
    const types = new Set(await cards.locator("span.absolute.top-3.left-3").allInnerTexts());
    check(types.size >= 2, `trộn nhiều loại (${[...types].join(", ")})`);
    const broken = await cards.locator("img").evaluateAll((imgs) => imgs.filter((i) => i.complete && i.naturalWidth === 0).length);
    check(broken === 0, `ảnh thẻ không vỡ (${broken} vỡ)`);
    await section.screenshot({ path: shot("for_you_vi.png") });

    // bấm thẻ đầu → mở đúng trang chi tiết
    const href = await cards.first().getAttribute("href");
    await cards.first().click();
    await page.waitForURL((url) => url.pathname === href, { timeout: 15000 });
    check(true, `bấm thẻ → ${href}`);

    // ---------- trang khách sạn: ghi lượt xem + khách sạn tương tự
    const hotels = await api("/api/hotels/?page=1&limit=1&name=Da%20Nang");
    const hotel = hotels[0];
    const viewReq = page.waitForRequest((r) => r.url().includes("/api/interactions/") && r.method() === "POST", { timeout: 15000 });
    await page.goto(`${FE}/hotel/${hotel.id}`, { waitUntil: "networkidle" });
    const req = await viewReq;
    const body = JSON.parse(req.postData());
    check(body.entityType === "hotel" && body.entityId === hotel.id && body.action === "view", "trang khách sạn ghi lượt xem");
    const similar = page.getByTestId("similar-hotels");
    await similar.waitFor({ timeout: 15000 });
    await similar.scrollIntoViewIfNeeded();
    await page.waitForTimeout(1000);
    const simCards = similar.getByTestId("recommendation-card");
    check((await simCards.count()) >= 1 && (await simCards.count()) <= 4, `Khách sạn tương tự: ${await simCards.count()} thẻ`);
    const simHrefs = await simCards.evaluateAll((els) => els.map((e) => e.getAttribute("href")));
    check(!simHrefs.includes(`/hotel/${hotel.id}`), "không gợi ý chính khách sạn đang xem");
    check((await similar.innerText()).includes("Khách sạn tương tự"), "tiêu đề Khách sạn tương tự");
    await similar.screenshot({ path: shot("similar_hotels.png") });

    // các trang chi tiết khác cũng ghi lượt xem
    const tours = await api("/api/tours/?page=1&limit=1");
    if (tours && tours.length) {
        const tourReq = page.waitForRequest((r) => r.url().includes("/api/interactions/") && r.method() === "POST", { timeout: 15000 });
        await page.goto(`${FE}/tours/${tours[0].id}`, { waitUntil: "networkidle" });
        check(JSON.parse((await tourReq).postData()).entityType === "tour", "trang tour ghi lượt xem");
    }
    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, "không 5xx");

    // ---------- tiếng Anh + tối + điện thoại
    const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
    await useAuth(mobile, auth);
    await mobile.addInitScript(() => {
        localStorage.setItem("tripnova.lang", "en");
        localStorage.setItem("tripnova.theme", "dark");
    });
    const mp = await mobile.newPage();
    await mp.goto(`${FE}/`, { waitUntil: "networkidle" });
    const msection = mp.getByTestId("for-you");
    await msection.waitFor({ timeout: 15000 });
    await msection.scrollIntoViewIfNeeded();
    await mp.waitForTimeout(1000);
    check(/Recommended for you|Popular picks/.test(await msection.innerText()), "EN: tiêu đề tiếng Anh");
    const overflow = await mp.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    check(overflow <= 1, `điện thoại: không cuộn ngang (${overflow}px)`);
    await msection.getByTestId("recommendation-card").first().screenshot({ path: shot("for_you_mobile_dark.png") });

    await browser.close();
});
