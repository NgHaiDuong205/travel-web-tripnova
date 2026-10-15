// Chatbot trả lời dạng stream: bong bóng "đang viết" (assistant-streaming) hiện chữ dần rồi thay bằng câu trả lời đã lưu;
// tải lại trang vẫn thấy đủ câu hỏi + câu trả lời; lỗi kiểm tra (câu hỏi rỗng) vẫn là JSON 400.
// Cần dịch vụ AI + Gemini; hết hạn mức thì bỏ qua (không tính lỗi). Ghi dữ liệu: 1 phiên chat của tài khoản test.
const { FE, BE, USER, check, login, useAuth, launch, watch, shot, run } = require("../lib");

run(async () => {
    const auth = await login(USER, true);
    const browser = await launch();
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await useAuth(ctx, auth);
    await ctx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    const page = await ctx.newPage();
    const w = watch(page);
    await page.goto(`${FE}/`, { waitUntil: "networkidle" });
    await page.getByTestId("chat-launcher").click({ force: true }); // nút nổi có hiệu ứng lơ lửng
    const widget = page.getByTestId("chat-widget");
    await widget.waitFor({ timeout: 10000 });

    // Ghi lại mọi lần bong bóng stream xuất hiện và độ dài chữ của nó.
    await page.evaluate(() => {
        window.__streamLengths = [];
        new MutationObserver(() => {
            const el = document.querySelector('[data-testid="assistant-streaming"]');
            if (el) window.__streamLengths.push(el.innerText.length);
        }).observe(document.body, { subtree: true, childList: true, characterData: true });
    });
    const streamRequest = page.waitForRequest((r) => r.url().includes("/messages/stream/"), { timeout: 15000 });
    await widget.locator("textarea").fill("Giới thiệu ngắn 5 khách sạn được đánh giá cao nhất ở Đà Nẵng");
    await widget.locator('button[type="submit"]').click();
    check(Boolean(await streamRequest), "gọi endpoint stream");

    await page.waitForFunction(
        () => {
            const items = document.querySelectorAll('[data-testid="chat-widget"] [data-testid="assistant-message"], [data-testid="chat-widget"] .bg-red-50');
            return items.length > 0 && !document.querySelector('[data-testid="assistant-streaming"]');
        },
        null,
        { timeout: 90000 }
    );
    const errorBubble = await widget.locator(".bg-red-50").count();
    if (errorBubble) {
        console.log("  ~ bỏ qua: dịch vụ AI hết hạn mức / chưa phản hồi");
        await browser.close();
        return;
    }
    const lengths = await page.evaluate(() => window.__streamLengths);
    check(lengths.length > 0, `bong bóng stream đã hiện (${lengths.length} lần cập nhật)`);
    check(new Set(lengths).size >= 2, `chữ chạy dần (độ dài: ${[...new Set(lengths)].slice(0, 6).join(", ")}…)`);
    const answer = widget.getByTestId("assistant-message").last();
    const text = await answer.innerText();
    check(/Đà Nẵng|Da Nang/i.test(text) && text.length > 80, "câu trả lời cuối đầy đủ");
    check((await answer.locator('a[href^="/hotel/"]').count()) > 0, "có link khách sạn");
    check((await answer.locator('button[aria-label]').count()) >= 2, "có nút đánh giá 👍/👎 (tin đã lưu)");
    await widget.screenshot({ path: shot("chatbot_stream.png") });

    // Tải lại trang: lịch sử có đủ câu hỏi + câu trả lời đã lưu.
    await page.reload({ waitUntil: "networkidle" });
    const reopened = page.getByTestId("chat-widget");
    if (!(await reopened.isVisible())) await page.getByTestId("chat-launcher").click({ force: true });
    await reopened.getByTestId("assistant-message").last().waitFor({ timeout: 15000 });
    check((await reopened.getByTestId("assistant-message").last().innerText()).trim().startsWith(text.trim().slice(0, 30)),
        "tải lại: câu trả lời đã được lưu");

    // Lỗi kiểm tra vẫn trả JSON (FE hiển thị thông báo, không treo).
    const session = await page.evaluate(() => JSON.parse(sessionStorage.getItem("tripnova.chatWidget") || "{}").activeId);
    const status = await page.evaluate(async ({ BE, session, token }) => {
        const r = await fetch(`${BE}/api/ai/chat/sessions/${session}/messages/stream/`, {
            method: "POST",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
            body: JSON.stringify({ content: " " }),
        });
        return { status: r.status, type: r.headers.get("content-type") };
    }, { BE, session, token: auth.accessToken });
    check(status.status === 400 && status.type.includes("json"), `câu hỏi rỗng → 400 JSON (${status.status})`);
    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, "không 5xx");
    await browser.close();
});
