// Smoke: mở các trang chính (khách / tài khoản / admin, VI + EN, desktop + điện thoại).
// Mỗi trang: có nội dung, không lỗi JS, không 5xx, không cuộn ngang trên điện thoại. Chỉ đọc, không ghi dữ liệu.
const { FE, ADMIN, USER, check, api, login, useAuth, launch, watch, shot, run } = require("../lib");

async function visit(page, monitor, url, label, { mobile = false } = {}) {
    monitor.reset();
    await page.goto(FE + url, { waitUntil: "domcontentloaded" });
    await page.waitForLoadState("networkidle", { timeout: 20000 }).catch(() => {});
    await page.waitForTimeout(600);
    const text = (await page.locator("body").innerText()).trim();
    const crashed = /Something went wrong|Unexpected Application Error/i.test(text);
    check(text.length > 200 && !crashed, `${label}: trang có nội dung (${text.length} ký tự)`);
    check(monitor.serverErrors().length === 0, `${label}: không 5xx ${monitor.serverErrors().slice(0, 2).join(", ")}`);
    check(monitor.errors().length === 0, `${label}: không lỗi console ${monitor.errors().slice(0, 2).join(" | ")}`);
    if (mobile) {
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
        check(!overflow, `${label}: không cuộn ngang`);
    }
}

run(async () => {
    const hotels = await api("/api/hotels/?limit=1");
    const cars = await api("/api/cars/?limit=1");
    const tours = await api("/api/tours/?limit=1");
    const dests = await api("/api/destinations/?q=Hoi%20An&limit=1");
    const browser = await launch();

    // 1. Khách chưa đăng nhập, tiếng Việt, desktop
    const guest = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await guest.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    let page = await guest.newPage();
    let monitor = watch(page);
    const publicPages = [
        ["/", "Trang chủ"], ["/hotels", "Khách sạn"], ["/destinations", "Điểm đến"], ["/cars", "Thuê xe"],
        ["/flights", "Chuyến bay"], ["/tours", "Tour"], ["/contacts", "Liên hệ"], ["/abouts", "Giới thiệu"],
        ["/ai-planner", "AI Planner"], ["/search?q=ha", "Tìm kiếm"], ["/login", "Đăng nhập"],
    ];
    if (hotels?.length) publicPages.push([`/hotel/${hotels[0].id}`, "Chi tiết khách sạn"]);
    if (cars?.length) publicPages.push([`/cars/${cars[0].id}`, "Chi tiết xe"]);
    if (tours?.length) publicPages.push([`/tours/${tours[0].id}`, "Chi tiết tour"]);
    if (dests?.length) publicPages.push([`/destinations/${dests[0].id}`, "Chi tiết điểm đến"]);
    for (const [url, label] of publicPages) await visit(page, monitor, url, label);
    await page.goto(FE + "/");
    await page.screenshot({ path: shot("smoke_home_vi.png") });

    // Trang cần đăng nhập -> về /login kèm redirect
    await page.goto(FE + "/my-trips");
    await page.waitForURL(/\/login/, { timeout: 10000 }).catch(() => {});
    check(page.url().includes("/login"), "Khách mở /my-trips bị chuyển tới /login");
    await guest.close();

    // 2. Khách đã đăng nhập: trang tài khoản
    const userAuth = await login(USER, true);
    const userCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await userCtx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    await useAuth(userCtx, userAuth);
    page = await userCtx.newPage();
    monitor = watch(page);
    for (const [url, label] of [["/profile", "Hồ sơ"], ["/my-trips", "Chuyến đi"], ["/cart", "Giỏ hàng"],
        ["/invoices", "Hoá đơn"], ["/notifications", "Thông báo"], ["/itineraries", "Lịch trình"],
        ["/saved-trips", "Đã lưu"], ["/account/security", "Bảo mật"]]) {
        await visit(page, monitor, url, label);
    }
    await page.goto(FE + "/admin");
    await page.waitForTimeout(1500);
    check(!/\/admin$/.test(new URL(page.url()).pathname) || /403|forbidden|không có quyền/i.test(await page.locator("body").innerText()),
        "Khách thường không vào được /admin");
    await userCtx.close();

    // 3. Admin
    const adminAuth = await login(ADMIN);
    const adminCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await adminCtx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    await useAuth(adminCtx, adminAuth);
    page = await adminCtx.newPage();
    monitor = watch(page);
    for (const [url, label] of [["/admin", "Admin dashboard"], ["/admin/hotels", "Admin khách sạn"],
        ["/admin/bookings", "Admin booking"], ["/admin/users", "Admin user"], ["/admin/payments", "Admin thanh toán"],
        ["/admin/invoices", "Admin hoá đơn"], ["/admin/messages", "Admin liên hệ"], ["/admin/audit-logs", "Admin audit log"]]) {
        await visit(page, monitor, url, label);
    }
    await page.goto(FE + "/admin/audit-logs");
    await page.waitForTimeout(1500);
    await page.screenshot({ path: shot("smoke_admin_audit.png") });
    await adminCtx.close();

    // 4. Điện thoại, tiếng Anh
    const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
    await mobile.addInitScript(() => localStorage.setItem("tripnova.lang", "en"));
    page = await mobile.newPage();
    monitor = watch(page);
    for (const [url, label] of [["/", "EN mobile home"], ["/hotels", "EN mobile hotels"], ["/ai-planner", "EN mobile planner"]]) {
        await visit(page, monitor, url, label, { mobile: true });
    }
    await page.goto(FE + "/");
    await page.screenshot({ path: shot("smoke_home_en_mobile.png") });
    await mobile.close();
    await browser.close();
});
