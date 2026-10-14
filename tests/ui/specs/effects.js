// Hiệu ứng site khách (SiteEffects + index.css): chữ hero trượt lên, parallax theo cuộn, thẻ nghiêng 3D theo con trỏ,
// nút "nam châm", ảnh hiện dần khi tải, độ mượt khi cuộn, chế độ giảm chuyển động. Chỉ đọc dữ liệu.
const { FE, check, launch, watch, shot, run } = require("../lib");

const vars = (loc) =>
    loc.evaluate((el) => ({
        cls: el.className,
        rx: parseFloat(el.style.getPropertyValue("--tn-rx")) || 0,
        ry: parseFloat(el.style.getPropertyValue("--tn-ry")) || 0,
        glare: el.style.getPropertyValue("--tn-glare"),
    }));

// Đếm khung hình trong lúc cuộn đều xuống, trả về FPS trung bình và khung chậm nhất (ms).
const scrollFps = (page) =>
    page.evaluate(
        () =>
            new Promise((resolve) => {
                const times = [];
                let y = 0;
                const step = (t) => {
                    times.push(t);
                    y += 18;
                    window.scrollTo(0, y);
                    if (times.length < 120) requestAnimationFrame(step);
                    else {
                        const gaps = times.slice(1).map((v, i) => v - times[i]);
                        resolve({ fps: 1000 / (gaps.reduce((a, b) => a + b, 0) / gaps.length), worst: Math.max(...gaps) });
                    }
                };
                requestAnimationFrame(step);
            })
    );

run(async () => {
    const browser = await launch();
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();
    const w = watch(page);

    // ---------- trang chủ: chữ hero, parallax, nam châm
    await page.goto(`${FE}/`, { waitUntil: "networkidle" });
    check(await page.locator("main h1 .tn-intro-line").count() === 2, "tiêu đề hero tách 2 dòng trượt lên");
    await page.waitForTimeout(1500);
    const introDone = await page.evaluate(() =>
        Array.from(document.querySelectorAll(".tn-intro, .tn-intro-line")).every((el) => getComputedStyle(el).opacity === "1")
    );
    check(introDone, "chữ hero hiện đủ sau khi chạy xong");
    const sda = await page.evaluate(() => CSS.supports("animation-timeline: scroll()"));
    check(sda, "Edge hỗ trợ scroll-driven animation");
    const tr0 = await page.evaluate(() => getComputedStyle(document.querySelector(".tn-parallax")).translate);
    await page.evaluate(() => window.scrollTo(0, 500));
    await page.waitForTimeout(200);
    const tr1 = await page.evaluate(() => getComputedStyle(document.querySelector(".tn-parallax")).translate);
    const fade = await page.evaluate(() => parseFloat(getComputedStyle(document.querySelector(".tn-scroll-fade")).opacity));
    check(tr0 !== tr1, `ảnh hero trôi parallax khi cuộn (${tr0} → ${tr1})`);
    check(fade < 1, `nội dung hero mờ dần khi cuộn qua (opacity ${fade.toFixed(2)})`);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(300);

    const magnet = page.locator("main [data-magnetic]").first();
    const mb = await magnet.boundingBox();
    await page.mouse.move(mb.x + mb.width * 0.95, mb.y + mb.height / 2, { steps: 4 });
    await page.waitForTimeout(450);
    const mt = await magnet.evaluate((el) => el.style.translate);
    check(/^[1-9]/.test(mt), `nút nam châm nhích về phía con trỏ (${mt})`);
    await page.mouse.move(5, 450, { steps: 3 });
    await page.waitForTimeout(100);
    check(await magnet.evaluate((el) => el.style.translate === ""), "rời nút: nút trở về chỗ cũ");

    // biểu tượng nảy lên khi khối dịch vụ hiện
    await page.locator(".tn-icon-pop").first().scrollIntoViewIfNeeded();
    await page.waitForTimeout(1200);
    const iconScale = await page.evaluate(() => getComputedStyle(document.querySelector(".tn-icon-pop")).scale);
    check(iconScale === "1" || iconScale === "none", `biểu tượng dịch vụ hiện đủ (scale ${iconScale})`);

    await page.evaluate(() => window.scrollTo(0, 0));
    const fps = await scrollFps(page);
    check(fps.fps > 40, `cuộn trang chủ mượt: ${fps.fps.toFixed(0)} FPS, khung chậm nhất ${fps.worst.toFixed(0)} ms`);

    // ---------- trang khách sạn: thẻ nghiêng 3D
    await page.goto(`${FE}/hotels`, { waitUntil: "networkidle" });
    const card = page.locator("main article.rounded-xl:has(img)").first();
    await card.scrollIntoViewIfNeeded();
    await page.waitForTimeout(900);
    const box = await card.boundingBox();
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps: 3 });
    await page.waitForTimeout(120);
    let v = await vars(card);
    check(v.cls.includes("tn-tilt"), "rê vào thẻ: bật nghiêng 3D");
    check(Math.abs(v.rx) < 0.6 && Math.abs(v.ry) < 0.6, `giữa thẻ ≈ phẳng (rx ${v.rx}, ry ${v.ry}) — toạ độ đúng với zoom 0.75`);
    await page.mouse.move(box.x + 8, box.y + 8, { steps: 3 });
    await page.waitForTimeout(120);
    v = await vars(card);
    check(v.rx > 4 && v.ry < -4, `góc trên trái: nghiêng về phía con trỏ (rx ${v.rx}, ry ${v.ry})`);
    check(v.glare === "1", "có vệt sáng theo con trỏ");
    await page.screenshot({ path: shot("effects_tilt.png") });
    await page.mouse.move(box.x + box.width + 300, 5, { steps: 3 });
    await page.waitForTimeout(750);
    v = await vars(card);
    check(!/tn-tilt( |$)/.test(v.cls), `rời thẻ: thẻ trở về phẳng (${v.cls.slice(-60)} rx ${v.rx})`);
    check(await page.evaluate(() => document.querySelectorAll("main img[data-tn-img]").length > 0), "ảnh trong trang được theo dõi để hiện dần");

    // bấm thẻ vẫn chuyển trang bình thường sau khi nghiêng
    const href = await card.locator('a[href^="/hotel/"]').first().getAttribute("href");
    await card.locator("img").first().click();
    await page.waitForURL(`**${href}`);
    check(true, "bấm thẻ đang nghiêng vẫn mở trang chi tiết");

    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, `không 5xx (${w.serverErrors().slice(0, 3).join(" | ")})`);

    // ---------- giảm chuyển động: không nghiêng, không chạy chữ
    const calm = await browser.newContext({ viewport: { width: 1280, height: 800 }, reducedMotion: "reduce" });
    const cp = await calm.newPage();
    await cp.goto(`${FE}/`, { waitUntil: "networkidle" });
    const anim = await cp.evaluate(() => getComputedStyle(document.querySelector(".tn-intro-line")).animationName);
    check(anim === "none", `giảm chuyển động: tắt chữ trượt (${anim})`);
    await cp.goto(`${FE}/hotels`, { waitUntil: "networkidle" });
    const ccard = cp.locator("main article.rounded-xl:has(img)").first();
    const cb = await ccard.boundingBox();
    await cp.mouse.move(cb.x + 10, cb.y + 10, { steps: 3 });
    await cp.waitForTimeout(150);
    check(!(await ccard.evaluate((el) => el.className)).includes("tn-tilt"), "giảm chuyển động: thẻ không nghiêng");

    // ---------- điện thoại: không nghiêng (không có chuột), trang vẫn hiển thị
    const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
    const mp = await mobile.newPage();
    const mw = watch(mp);
    await mp.goto(`${FE}/`, { waitUntil: "networkidle" });
    await mp.waitForTimeout(1300);
    await mp.screenshot({ path: shot("effects_mobile_home.png") });
    check(await mp.locator("main h1").isVisible(), "điện thoại: hero hiển thị");
    check(mw.errors().length === 0, "điện thoại: không lỗi console");

    await browser.close();
});
