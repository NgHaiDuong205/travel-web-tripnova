// Chatbot tra tình trạng trống theo ngày: đang xem trang xe → hỏi "xe này còn trống ..." → câu trả lời có nhãn [LIVE]
// (tra trực tiếp qua API công khai), có ngày và giá. Cần dịch vụ AI + Gemini; hết hạn mức thì bỏ qua (không tính lỗi).
// Ghi dữ liệu: 1 phiên chat của tài khoản test.
const { FE, USER, check, api, login, useAuth, launch, watch, shot, run } = require("../lib");

run(async () => {
    const auth = await login(USER, true);
    const cars = await api("/api/cars/?limit=1");
    const car = (cars.content || cars)[0];
    check(Boolean(car?.id), "có xe để hỏi");
    const day = new Date(Date.now() + 50 * 86400000);
    const ret = new Date(day.getTime() + 2 * 86400000);
    const dm = (d) => `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`;

    const browser = await launch();
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await useAuth(ctx, auth);
    await ctx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    const page = await ctx.newPage();
    const w = watch(page);
    await page.goto(`${FE}/cars/${car.id}`, { waitUntil: "networkidle" });
    await page.getByTestId("chat-launcher").click({ force: true }); // nút nổi có hiệu ứng lơ lửng
    const widget = page.getByTestId("chat-widget");
    await widget.waitFor({ timeout: 10000 });
    await widget.locator("textarea").fill(`Xe này còn trống từ ${dm(day)} đến ${dm(ret)} không?`);
    await widget.locator('button[type="submit"]').click();
    const answer = widget.getByTestId("assistant-message").last();
    await page.waitForFunction(
        () => {
            const items = document.querySelectorAll('[data-testid="chat-widget"] [data-testid="assistant-message"]');
            const last = items[items.length - 1];
            return last && /LIVE|hạn mức|thử lại|chưa phản hồi/.test(last.innerText);
        },
        null,
        { timeout: 90000 }
    );
    const text = await answer.innerText();
    if (/hạn mức|chưa phản hồi/.test(text)) {
        console.log("  ~ bỏ qua: dịch vụ AI hết hạn mức / chưa phản hồi");
        await browser.close();
        return;
    }
    const badge = answer.locator("button", { hasText: /^LIVE$/ }).first();
    check(await badge.isVisible(), "nhãn LIVE hiển thị");
    check((await badge.getAttribute("class")).includes("bg-green-50"), "nhãn LIVE màu xanh");
    check(/₫/.test(text), "có giá VND");
    check(/còn trống|đã có người|không còn|đã được thuê|bận/i.test(text), "trả lời tình trạng trống");
    await widget.screenshot({ path: shot("chatbot_availability.png") });
    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, "không 5xx");
    await browser.close();
});
