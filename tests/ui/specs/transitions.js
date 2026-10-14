// Chuyển cảnh giữa các trang (View Transitions, components/common/PageTransitions): điều hướng đúng, ảnh thẻ "bay"
// sang ảnh lớn trang chi tiết, không phá onClick riêng của Link (ghi lượt bấm tìm kiếm), nút trong thẻ, Ctrl+click,
// nút Back và chế độ giảm chuyển động. Chỉ đọc dữ liệu.
const { FE, USER, check, login, useAuth, launch, watch, shot, run } = require("../lib");

// Đếm số lần startViewTransition + ghi lại ảnh có tên tn-hero ngay sau khi trang mới render.
const INSTRUMENT = () => {
    window.__vt = { count: 0, hero: [] };
    const original = document.startViewTransition?.bind(document);
    if (!original) return;
    document.startViewTransition = (cb) => {
        window.__vt.count += 1;
        const vt = original(cb);
        vt.updateCallbackDone.then(() => {
            const img = Array.from(document.images).find((i) => i.style.viewTransitionName === "tn-hero");
            window.__vt.hero.push(img ? img.getAttribute("src") : null);
        });
        return vt;
    };
};

const vtCount = (page) => page.evaluate(() => window.__vt.count);
const settled = (page) => page.waitForFunction(() => !document.documentElement.classList.contains("tn-vt"));

run(async () => {
    const browser = await launch();
    const auth = await login(USER, true);
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await useAuth(context, auth);
    await context.addInitScript(INSTRUMENT);
    const page = await context.newPage();
    const w = watch(page);

    check(await page.evaluate(() => typeof document.startViewTransition === "function"), "Edge hỗ trợ View Transitions");

    // ---------- thẻ khách sạn → trang chi tiết, ảnh bay
    await page.goto(`${FE}/hotels`, { waitUntil: "networkidle" });
    const card = page.locator('main a[href^="/hotel/"]:has(img)').first();
    await card.waitFor();
    const href = await card.getAttribute("href");
    const cardImg = await card.locator("img").first().getAttribute("src");
    await page.evaluate(() => window.scrollTo(0, 300));
    await card.locator("img").first().click();
    await page.waitForTimeout(220);
    await page.screenshot({ path: shot("transition_mid.png") });
    await page.waitForURL(`**${href}`);
    await settled(page);
    check((await vtCount(page)) === 1, "bấm thẻ khách sạn → 1 view transition");
    const hero = await page.evaluate(() => window.__vt.hero[0]);
    check(!!hero, `ảnh lớn trang chi tiết được gắn tn-hero (${hero})`);
    check(cardImg && hero && cardImg.split("?")[0] === hero.split("?")[0], "ảnh bay đúng: cùng ảnh bìa với thẻ");
    check(await page.evaluate(() => window.scrollY) === 0, "trang mới bắt đầu từ đầu trang");
    check(await page.evaluate(() => !document.querySelector('img[style*="tn-hero"]')), "dọn tên tn-hero sau chuyển cảnh");
    await page.screenshot({ path: shot("transition_after.png") });

    // ---------- menu header
    await page.locator("nav.fixed.top-0").getByRole("link", { name: /^(Destinations|Điểm đến)$/ }).first().click();
    await page.waitForURL("**/destinations");
    await settled(page);
    check((await vtCount(page)) === 2, "menu header → chuyển cảnh");
    check(await page.locator("main").count() === 1, "trang điểm đến hiển thị");

    // ---------- Back của trình duyệt (không chặn, điều hướng bình thường)
    await page.goBack();
    await page.waitForURL(`**${href}`);
    check(true, "Back quay lại trang khách sạn");

    // ---------- nút trong thẻ (tim yêu thích) không làm chuyển trang
    await page.goto(`${FE}/hotels`, { waitUntil: "networkidle" });
    const before = await vtCount(page);
    const heart = page.locator('main a[href^="/hotel/"] button').first();
    if (await heart.count()) {
        await heart.click();
        await page.waitForTimeout(400);
        check(new URL(page.url()).pathname === "/hotels", "bấm nút trong thẻ: vẫn ở /hotels");
        check((await vtCount(page)) === before, "bấm nút trong thẻ: không chuyển cảnh");
        await heart.click(); // trả trạng thái yêu thích như cũ
        await page.waitForTimeout(400);
    } else {
        check(true, "thẻ không có nút bên trong (bỏ qua)");
    }

    // ---------- Ctrl+click: không chặn (mở tab mới)
    const popupPromise = context.waitForEvent("page", { timeout: 5000 }).catch(() => null);
    await page.locator('main a[href^="/hotel/"]:has(img)').first().click({ modifiers: ["Control"] });
    const popup = await popupPromise;
    check(!!popup && new URL(page.url()).pathname === "/hotels", "Ctrl+click mở tab mới, trang hiện tại giữ nguyên");
    if (popup) await popup.close();

    // ---------- Link có onClick riêng (ghi lượt bấm tìm kiếm) vẫn chạy
    await page.goto(`${FE}/search?q=Hoi%20An`, { waitUntil: "networkidle" });
    const result = page.locator("main a[href]").filter({ hasText: /Hội An|Hoi An/ }).first();
    if (await result.count()) {
        const clickReq = page.waitForRequest((r) => r.url().includes("/api/search/click/"), { timeout: 5000 }).catch(() => null);
        const from = page.url();
        await result.click();
        check(!!(await clickReq), "trang Tìm kiếm: vẫn gửi /api/search/click/");
        await page.waitForURL((u) => u.toString() !== from);
        await settled(page);
        check(true, "trang Tìm kiếm: chuyển sang kết quả");
    } else {
        check(false, "trang Tìm kiếm: có kết quả để bấm");
    }

    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, "không 5xx");

    // ---------- giảm chuyển động: không chuyển cảnh nhưng vẫn điều hướng
    const calm = await browser.newContext({ viewport: { width: 1280, height: 800 }, reducedMotion: "reduce" });
    await calm.addInitScript(INSTRUMENT);
    const cp = await calm.newPage();
    await cp.goto(`${FE}/hotels`, { waitUntil: "networkidle" });
    const calmCard = cp.locator('main a[href^="/hotel/"]:has(img)').first();
    const calmHref = await calmCard.getAttribute("href");
    await calmCard.click();
    await cp.waitForURL(`**${calmHref}`);
    check((await vtCount(cp)) === 0, "giảm chuyển động: không dùng view transition, vẫn sang trang");

    // ---------- điện thoại
    const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
    await mobile.addInitScript(INSTRUMENT);
    const mp = await mobile.newPage();
    const mw = watch(mp);
    await mp.goto(`${FE}/hotels`, { waitUntil: "networkidle" });
    const mCard = mp.locator('main a[href^="/hotel/"]:has(img)').first();
    const mHref = await mCard.getAttribute("href");
    await mCard.locator("img").first().click();
    await mp.waitForURL(`**${mHref}`);
    await settled(mp);
    check((await vtCount(mp)) === 1, "điện thoại: chuyển cảnh khi bấm thẻ");
    check(mw.errors().length === 0, "điện thoại: không lỗi console");

    await browser.close();
});
