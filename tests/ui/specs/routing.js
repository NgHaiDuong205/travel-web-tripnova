// UI: nút "Lên lịch trình bằng AI" ở trang điểm đến + đường đi thật trên bản đồ AI Planner / lịch trình đã lưu.
// Cần BE + dịch vụ AI (hoặc AI tắt -> lịch trình tự động) + OSRM truy cập được. Tạo 2 lịch trình mới cho user test (không xoá gì).
const { FE, USER, check, api, launch, shot, run, login } = require("../lib");

run(async () => {
    const dests = await api("/api/destinations/");
    const hoian = dests.find((d) => d.name === "Hoi An");
    const danang = dests.find((d) => d.name === "Da Nang");
    const auth = await login(USER, true);

    const browser = await launch();
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const page = await context.newPage();
    const errors = [];
    const serverErrors = [];
    let routeCalls = 0;
    page.on("console", (m) => { if (m.type() === "error") errors.push(m.text()); });
    page.on("pageerror", (e) => errors.push(String(e)));
    page.on("response", (r) => {
        if (r.status() >= 500) serverErrors.push(`${r.status()} ${r.url()}`);
        if (r.url().includes("/api/routing/route/")) routeCalls += 1;
    });
    await page.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));

    // 1. Khách chưa đăng nhập: trang điểm đến có nút → AI Planner điền sẵn điểm đến
    await page.goto(`${FE}/destinations/${hoian.id}`);
    const btn = page.getByRole("link", { name: "Lên lịch trình bằng AI" });
    await btn.waitFor({ timeout: 20000 });
    check(await btn.isVisible(), "live destination page shows 'Lên lịch trình bằng AI'");
    await btn.click();
    await page.waitForURL(/\/ai-planner\?/);
    const url = new URL(page.url());
    check(url.searchParams.get("destinationId") === hoian.id && url.searchParams.get("name") === "Hoi An", `planner URL carries destination (${url.search})`);
    const destInput = page.getByPlaceholder(/./).first();
    await page.waitForTimeout(800);
    check((await page.locator("input").first().inputValue()).includes("Hoi An"), "planner destination field prefilled with Hoi An");

    // 2. Trang biên tập có bản ghi DB (Amalfi Coast) cũng có nút
    await page.goto(`${FE}/destinations/amalfi`);
    const curatedBtn = page.getByRole("link", { name: "Lên lịch trình bằng AI" });
    await curatedBtn.waitFor({ timeout: 20000 }).catch(() => {});
    check(await curatedBtn.isVisible(), "curated page with DB match (amalfi) shows the button");
    if (await curatedBtn.isVisible()) {
        check((await curatedBtn.getAttribute("href")).includes("name=Amalfi+Coast"), "curated button links to Amalfi Coast");
    }

    // 3. Đăng nhập (token vào localStorage) → tạo lịch trình Hội An từ nút
    await page.evaluate(({ a }) => {
        localStorage.setItem("tripnova.accessToken", a.accessToken);
        localStorage.setItem("tripnova.refreshToken", a.refreshToken);
        localStorage.setItem("tripnova.user", JSON.stringify(a.user));
        sessionStorage.removeItem("tripnova.planner");
    }, { a: auth });
    await page.goto(`${FE}/destinations/${hoian.id}`);
    await page.getByRole("link", { name: "Lên lịch trình bằng AI" }).click();
    await page.waitForURL(/\/ai-planner\?/);
    await page.getByRole("button", { name: "Lên lịch trình" }).click();
    await page.locator("text=Ngày 1").first().waitFor({ timeout: 90000 });
    await page.waitForTimeout(2500); // đường đi tải sau
    const walkLegs = await page.locator("span.material-symbols-outlined", { hasText: /^directions_walk$/ }).count();
    const driveLegs = await page.locator("span.material-symbols-outlined", { hasText: /^directions_car$/ }).count();
    check(walkLegs + driveLegs > 0, `timeline shows travel modes (walk ${walkLegs}, drive ${driveLegs})`);
    check(await page.locator("text=/Đi (bộ|xe) ~\\d+ phút · [\\d.]+ km/").count() > 0, "timeline text 'Đi bộ/Đi xe ~N phút · X km'");
    check(await page.locator("text=Chưa lấy được dữ liệu đường đi").count() === 0, "no routing_estimated warning shown");
    const solid = () => page.locator("path.leaflet-interactive:not([stroke-dasharray])").count();
    const dashed = () => page.locator("path.leaflet-interactive[stroke-dasharray='8 8']").count();
    let s = await solid();
    check(s >= 3, `map draws road routes as solid lines (${s}), dashed ${await dashed()}`);
    const attribution = await page.locator(".leaflet-control-attribution").innerText();
    check(attribution.includes("OSRM"), "map attribution credits OSRM");
    const pts = await page.locator("path.leaflet-interactive:not([stroke-dasharray])").first().getAttribute("d");
    check((pts.match(/L/g) || []).length > 5, `road polyline has many vertices (${(pts.match(/L/g) || []).length})`);
    await page.screenshot({ path: shot("routing_planner.png") });

    // 4. Chỉ xem ngày 2 → 1 đường; bỏ 1 điểm → tải lại đường (request mới), vẫn nét liền
    await page.getByRole("button", { name: "Ngày 2" }).click();
    await page.waitForTimeout(1500);
    check(await solid() === 1, `day 2 only: one solid route (${await solid()})`);
    const before = routeCalls;
    const removeBtn = page.locator("button[aria-label^='Bỏ ']").first();
    await removeBtn.click();
    await page.waitForTimeout(2500);
    check(routeCalls > before, `removing a stop refetches the route (${routeCalls - before} call)`);
    check(await solid() === 1 && await dashed() === 0, "after removal the route is still a road route");
    await page.getByRole("button", { name: "Tất cả các ngày" }).click();

    // 5. Lưu → trang lịch trình có đường theo phố
    await page.getByRole("button", { name: "Lưu vào lịch trình của tôi" }).click();
    await page.waitForURL(/\/itineraries\/[0-9a-f-]{36}/, { timeout: 30000 });
    await page.locator(".leaflet-container").waitFor({ timeout: 20000 });
    await page.waitForTimeout(3000);
    s = await solid();
    check(s >= 3, `saved itinerary map draws road routes (${s})`);
    await page.screenshot({ path: shot("routing_itinerary.png") });
    const itineraryId = page.url().split("/itineraries/")[1];

    // 6. Có bản nháp Hội An trong phiên, bấm nút ở Đà Nẵng → bỏ bản nháp cũ, form là Đà Nẵng
    await page.goto(`${FE}/ai-planner`);
    await page.locator("text=Ngày 1").first().waitFor({ timeout: 10000 });
    await page.goto(`${FE}/destinations/${danang.id}`);
    await page.getByRole("link", { name: "Lên lịch trình bằng AI" }).click();
    await page.waitForURL(/\/ai-planner\?/);
    await page.waitForTimeout(800);
    check(await page.locator("text=Ngày 1").count() === 0, "stale Hoi An draft dropped when opening planner for Da Nang");
    check((await page.locator("input").first().inputValue()).includes("Da Nang"), "form shows Da Nang");
    // Mở lại planner cho chính điểm đến của bản nháp thì giữ bản nháp
    await page.getByRole("button", { name: "Lên lịch trình" }).click();
    await page.locator("text=Ngày 1").first().waitFor({ timeout: 90000 });
    await page.goto(`${FE}/destinations/${danang.id}`);
    await page.getByRole("link", { name: "Lên lịch trình bằng AI" }).click();
    await page.waitForURL(/\/ai-planner\?/);
    await page.waitForTimeout(800);
    check(await page.locator("text=Ngày 1").count() > 0, "same-destination draft kept");

    // 7. Điện thoại, tiếng Anh: nút ở hero + nhãn EN
    const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
    const m = await mobile.newPage();
    await m.addInitScript(() => localStorage.setItem("tripnova.lang", "en"));
    await m.goto(`${FE}/destinations/${hoian.id}`);
    const enBtn = m.getByRole("link", { name: "Plan a trip with AI" });
    await enBtn.waitFor({ timeout: 20000 });
    const box = await enBtn.boundingBox();
    check(box && box.x >= 0 && box.x + box.width <= 390, `EN mobile button visible within viewport (${JSON.stringify(box)})`);
    const overflow = await m.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
    check(!overflow, "no horizontal scroll on mobile");
    await m.screenshot({ path: shot("routing_mobile.png") });

    check(serverErrors.length === 0, `no 5xx (${serverErrors.join(", ")})`);
    const realErrors = errors.filter((e) => !/favicon|Download the React DevTools|ERR_BLOCKED|tile/i.test(e));
    check(realErrors.length === 0, `no console errors ${realErrors.slice(0, 3).join(" | ")}`);
    console.log("itinerary", itineraryId);
    await browser.close();
});
