// Đăng ký trên giao diện → màn "Kiểm tra hộp thư" → email chào mừng (Mailpit) → đăng nhập khi chưa xác thực bị chặn,
// có nút gửi lại → bấm link trong email → xác thực → đăng nhập được. Quên mật khẩu: bấm link trong email → đổi mật khẩu.
// Cần Mailpit (UI_MAILPIT_URL, mặc định http://127.0.0.1:8025) và BE gửi mail vào Mailpit. Tạo 1 tài khoản test mới.
const { FE, check, launch, watch, shot, run } = require("../lib");

const MAILPIT = (process.env.UI_MAILPIT_URL || "http://127.0.0.1:8025").replace(/\/$/, "");
const PASSWORD = "secret123";

async function waitMail(to, subject, since, timeoutMs = 20000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const res = await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`);
        const { messages = [] } = await res.json();
        const hit = messages.find((m) => m.Subject.includes(subject) && Date.parse(m.Created) >= since - 2000);
        if (hit) return (await fetch(`${MAILPIT}/api/v1/message/${hit.ID}`)).json();
        await new Promise((r) => setTimeout(r, 500));
    }
    throw new Error(`Không thấy email "${subject}" tới ${to}`);
}

function linkFrom(mail, path) {
    const m = mail.Text.match(new RegExp(`https?://[^\\s"<>]*${path}\\?token=[^\\s"<>&]+`));
    return m && m[0];
}

run(async () => {
    try {
        await fetch(`${MAILPIT}/api/v1/info`);
    } catch {
        console.log(`SKIP Mailpit không chạy ở ${MAILPIT}`);
        return;
    }
    const email = `e2e.verify.ui${Date.now()}@tripnova.local`;
    const browser = await launch();
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await ctx.addInitScript(() => localStorage.setItem("tripnova.lang", "vi"));
    const page = await ctx.newPage();
    const w = watch(page);

    // ---------- đăng ký
    const since = Date.now();
    await page.goto(`${FE}/register`, { waitUntil: "networkidle" });
    await page.fill('input[name="fullName"]', "Khách Thử Nghiệm");
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', PASSWORD);
    await page.fill('input[name="confirmPassword"]', PASSWORD);
    await page.check('input[name="agree"]');
    await page.getByRole("button", { name: "Tạo tài khoản" }).click();
    const panel = page.getByTestId("check-email");
    await panel.waitFor({ timeout: 15000 });
    const panelText = await panel.innerText();
    check(panelText.includes("Kiểm tra hộp thư") && panelText.includes(email), "đăng ký xong: màn Kiểm tra hộp thư có email");
    check(await panel.getByRole("button", { name: /Gửi lại sau \d+ giây/ }).isDisabled(), "nút gửi lại đang đếm ngược 60 giây");
    const stored = await page.evaluate(() => localStorage.getItem("tripnova.accessToken"));
    check(!stored, "chưa xác thực: không lưu token (chưa đăng nhập)");
    await panel.screenshot({ path: shot("verify_check_inbox.png") });

    const welcome = await waitMail(email, "Welcome to TripNova", since);
    check(welcome.Subject.includes("Chào mừng"), `email chào mừng: "${welcome.Subject}"`);
    const verifyLink = linkFrom(welcome, "/verify-email");
    check(!!verifyLink, "email có link xác thực");

    // ảnh chụp email (HTML) để xem bố cục
    const mailPage = await ctx.newPage();
    await mailPage.setContent(welcome.HTML);
    await mailPage.screenshot({ path: shot("verify_welcome_email.png"), fullPage: true });
    await mailPage.close();

    // ---------- đăng nhập khi chưa xác thực
    await page.goto(`${FE}/login`, { waitUntil: "networkidle" });
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', PASSWORD);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    const notVerified = page.getByText("chưa được xác thực");
    await notVerified.waitFor({ timeout: 10000 });
    check(true, "đăng nhập khi chưa xác thực: báo chưa xác thực");
    check(page.url().includes("/login"), "vẫn ở trang đăng nhập");
    const resend = page.getByRole("button", { name: "Gửi lại email xác thực" });
    check(await resend.isVisible(), "có nút Gửi lại email xác thực");
    await resend.click();
    await page.getByText(/vừa gửi lại link/).waitFor({ timeout: 10000 });
    check(await page.getByRole("button", { name: /Gửi lại sau \d+ giây/ }).isVisible(), "bấm gửi lại → báo đã gửi + đếm ngược");
    await page.screenshot({ path: shot("verify_login_blocked.png") });

    // ---------- bấm link trong email
    await page.goto(verifyLink, { waitUntil: "networkidle" });
    await page.getByText("đã được xác minh").waitFor({ timeout: 10000 });
    check((await page.locator("main, section").first().innerText()).includes(email), "trang xác thực: báo email đã xác minh");
    await page.getByRole("link", { name: "Đăng nhập" }).last().click();
    await page.waitForURL(/\/login/);
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', PASSWORD);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await page.waitForURL((url) => !url.pathname.startsWith("/login"), { timeout: 15000 });
    const token = await page.evaluate(() => localStorage.getItem("tripnova.accessToken"));
    check(!!token, "xác thực xong: đăng nhập thành công");

    // ---------- quên mật khẩu qua link email
    await ctx.clearCookies();
    await page.evaluate(() => { localStorage.removeItem("tripnova.accessToken"); localStorage.removeItem("tripnova.refreshToken"); localStorage.removeItem("tripnova.user"); });
    const sinceReset = Date.now();
    await page.goto(`${FE}/forgot-password`, { waitUntil: "networkidle" });
    await page.fill('input[type="email"]', email);
    await page.getByRole("button", { name: /Gửi liên kết/ }).click();
    await page.getByText(/liên kết đặt lại mật khẩu đã được gửi/).waitFor({ timeout: 10000 });
    const resetMail = await waitMail(email, "Reset your TripNova password", sinceReset);
    const resetLink = linkFrom(resetMail, "/reset-password");
    check(!!resetLink, "email quên mật khẩu có link đặt lại");
    await page.goto(resetLink, { waitUntil: "networkidle" });
    const pwInputs = page.locator('input[type="password"]');
    await pwInputs.nth(0).fill("newsecret123");
    if (await pwInputs.count() > 1) await pwInputs.nth(1).fill("newsecret123");
    await page.locator('form button[type="submit"]').click();
    await page.getByText(/Đặt lại mật khẩu thành công/).waitFor({ timeout: 10000 });
    check(true, "bấm link → đặt mật khẩu mới thành công");
    await page.goto(`${FE}/login`, { waitUntil: "networkidle" });
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', "newsecret123");
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await page.waitForURL((url) => !url.pathname.startsWith("/login"), { timeout: 15000 });
    check(true, "đăng nhập bằng mật khẩu mới");

    // ---------- điện thoại: màn kiểm tra hộp thư không cuộn ngang
    const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true });
    await mobile.addInitScript(() => localStorage.setItem("tripnova.lang", "en"));
    const mp = await mobile.newPage();
    await mp.goto(`${FE}/register`, { waitUntil: "networkidle" });
    const mEmail = `e2e.verify.uim${Date.now()}@tripnova.local`;
    await mp.fill('input[name="fullName"]', "Mobile Tester");
    await mp.fill('input[name="email"]', mEmail);
    await mp.fill('input[name="password"]', PASSWORD);
    await mp.fill('input[name="confirmPassword"]', PASSWORD);
    await mp.check('input[name="agree"]');
    await mp.locator('form button[type="submit"]').click();
    await mp.getByTestId("check-email").waitFor({ timeout: 15000 });
    check((await mp.getByTestId("check-email").innerText()).includes("Check your inbox"), "EN điện thoại: Check your inbox");
    const overflow = await mp.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    check(overflow <= 1, `điện thoại: không cuộn ngang (${overflow}px)`);
    await mp.screenshot({ path: shot("verify_check_inbox_mobile.png") });

    check(w.errors().length === 0, `không lỗi console (${w.errors().slice(0, 3).join(" | ")})`);
    check(w.serverErrors().length === 0, "không 5xx");
    await browser.close();
});
