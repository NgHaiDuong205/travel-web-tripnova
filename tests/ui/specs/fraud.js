// Trang admin "Rủi ro gian lận": số liệu theo mức, mặc định lọc "Chờ xem xét", lý do tiếng Việt, lọc mức Cao,
// đánh dấu Hợp lệ rồi trả lại Xem lại (giữ nguyên trạng thái ban đầu), tiếng Anh.
const { FE, ADMIN, check, login, useAuth, launch, watch, shot, run } = require("../lib");

run(async () => {
    const auth = await login(ADMIN);
    const browser = await launch();
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await useAuth(ctx, auth);
    await ctx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    const page = await ctx.newPage();
    const w = watch(page);
    await page.goto(`${FE}/admin/fraud`, { waitUntil: "networkidle" });
    check(await page.getByRole("link", { name: /Rủi ro gian lận/ }).first().isVisible(), "menu có Rủi ro gian lận");
    const stats = page.getByTestId("fraud-stats");
    await stats.waitFor({ timeout: 15000 });
    check(/Cao: \d+/.test(await stats.innerText()) && /Chờ xem xét: \d+/.test(await stats.innerText()), "số giao dịch theo mức + chờ xem xét");
    const table = page.getByTestId("fraud-table");
    const rows = table.locator("tbody tr");
    const count = await rows.count();
    check(count > 0, `bảng có ${count} dòng (mặc định: chờ xem xét)`);
    const statuses = await rows.locator("td:nth-child(7)").allInnerTexts();
    check(statuses.every((s) => s.includes("Chờ xem xét")), "mặc định chỉ hiện giao dịch chờ xem xét");
    const tableText = await table.innerText();
    check(/Tài khoản mới|Đặt nhiều đơn|Thanh toán lỗi|Huỷ nhiều đơn|Email chưa xác thực|Số tiền lớn/.test(tableText), "lý do hiển thị tiếng Việt");
    await page.screenshot({ path: shot("fraud_admin.png") });

    // lọc mức Cao bằng chip
    await stats.getByRole("button", { name: /^Cao:/ }).click();
    await page.waitForLoadState("networkidle");
    const levels = await rows.locator("td:nth-child(5) span.rounded-full").allInnerTexts();
    check(levels.length > 0 && levels.every((l) => l === "Cao"), "chip Cao → chỉ mức Cao");

    // đánh dấu Hợp lệ → Xem lại (trả về trạng thái cũ)
    await stats.getByRole("button", { name: /^Chờ xem xét:/ }).click();
    await page.waitForLoadState("networkidle");
    const first = rows.first();
    const orderCode = (await first.locator("td:nth-child(2) p").first().innerText()).trim();
    await page.fill('input:not([type])', orderCode);
    await page.getByRole("button", { name: "Lọc" }).click();
    await page.waitForLoadState("networkidle");
    await page.locator("select").nth(1).selectOption("");
    await page.getByRole("button", { name: "Lọc" }).click();
    await page.waitForLoadState("networkidle");
    const row = rows.filter({ hasText: orderCode }).first();
    await row.getByRole("button", { name: "Hợp lệ" }).click();
    await page.waitForLoadState("networkidle");
    await row.locator("td:nth-child(7)").filter({ hasText: "Hợp lệ" }).waitFor({ timeout: 10000 });
    check(true, `đánh dấu ${orderCode} Hợp lệ`);
    await row.getByRole("button", { name: "Xem lại" }).click();
    await page.waitForLoadState("networkidle");
    await row.locator("td:nth-child(7)").filter({ hasText: "Chờ xem xét" }).waitFor({ timeout: 10000 });
    check(true, "trả lại trạng thái Chờ xem xét");
    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, "không 5xx");

    // tiếng Anh
    const en = await browser.newContext({ viewport: { width: 1280, height: 800 } });
    await useAuth(en, auth);
    await en.addInitScript(() => localStorage.setItem("tripnova.lang", "en"));
    const ep = await en.newPage();
    await ep.goto(`${FE}/admin/fraud`, { waitUntil: "networkidle" });
    await ep.getByTestId("fraud-stats").waitFor({ timeout: 15000 });
    check((await ep.locator("h1").innerText()).includes("Fraud risk"), "EN: tiêu đề Fraud risk");
    check(/New account|Many orders|Failed payments|Many cancellations|Email not verified|Large amount/.test(await ep.getByTestId("fraud-table").innerText()), "EN: lý do tiếng Anh");

    await browser.close();
});
