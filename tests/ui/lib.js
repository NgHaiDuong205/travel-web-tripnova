// Tiện ích chung cho test UI (Playwright + Edge cài sẵn, không tải trình duyệt).
// Biến môi trường: UI_FE_URL (http://localhost:3000), UI_BE_URL (http://localhost:8080),
// UI_ADMIN_EMAIL/UI_ADMIN_PASSWORD, UI_USER_EMAIL/UI_USER_PASSWORD, UI_HEADED=1 để xem trình duyệt.
const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright-core");

const FE = (process.env.UI_FE_URL || "http://localhost:3000").replace(/\/$/, "");
const BE = (process.env.UI_BE_URL || "http://localhost:8080").replace(/\/$/, "");
const ADMIN = { email: process.env.UI_ADMIN_EMAIL || "claude.test+a2@tripnova.local", password: process.env.UI_ADMIN_PASSWORD || "secret123" };
const USER = { email: process.env.UI_USER_EMAIL || "e2e.user@tripnova.local", password: process.env.UI_USER_PASSWORD || "secret123" };
const SHOTS = path.join(__dirname, "shots");

let failed = 0;
function check(cond, msg) {
    console.log(`${cond ? "PASS" : "FAIL"} ${msg}`);
    if (!cond) failed += 1;
}

async function api(p, opts = {}) {
    const res = await fetch(BE + p, { headers: { "Content-Type": "application/json", ...(opts.headers || {}) }, ...opts });
    const text = await res.text();
    return text ? JSON.parse(text) : null;
}

/** Đăng nhập qua API; tài khoản khách chưa có thì đăng ký. */
async function login({ email, password }, register = false) {
    let res = await fetch(BE + "/api/auth/login/", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
    if (!res.ok && register) {
        await fetch(BE + "/api/auth/register/", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password, fullName: "E2E User" }) });
        res = await fetch(BE + "/api/auth/login/", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
    }
    if (!res.ok) throw new Error(`Không đăng nhập được ${email}: ${res.status}`);
    return res.json();
}

/** Ghi token vào localStorage trước khi trang chạy (giống FE sau khi đăng nhập). */
async function useAuth(context, auth) {
    await context.addInitScript((a) => {
        localStorage.setItem("tripnova.accessToken", a.accessToken);
        localStorage.setItem("tripnova.refreshToken", a.refreshToken);
        localStorage.setItem("tripnova.user", JSON.stringify(a.user));
    }, auth);
}

async function launch() {
    return chromium.launch({ channel: "msedge", headless: !process.env.UI_HEADED });
}

/** Theo dõi lỗi console + phản hồi 5xx của một trang. */
function watch(page) {
    const errors = [];
    const serverErrors = [];
    page.on("console", (m) => { if (m.type() === "error") errors.push(m.text()); });
    page.on("pageerror", (e) => errors.push(String(e)));
    page.on("response", (r) => { if (r.status() >= 500) serverErrors.push(`${r.status()} ${r.url()}`); });
    return {
        errors: () => errors.filter((e) => !/favicon|React DevTools|ERR_BLOCKED|tile|Failed to load resource: the server responded with a status of 4\d\d/i.test(e)),
        serverErrors: () => serverErrors,
        reset: () => { errors.length = 0; serverErrors.length = 0; },
    };
}

function shot(name) {
    fs.mkdirSync(SHOTS, { recursive: true });
    return path.join(SHOTS, name);
}

/** Kết thúc spec: in tổng kết, exit code 1 nếu có FAIL. */
function finish() {
    console.log(failed ? `FAILED ${failed}` : "ALL PASS");
    process.exit(failed ? 1 : 0);
}

function run(main) {
    main().then(finish).catch((e) => { console.error(e); process.exit(2); });
}

module.exports = { FE, BE, ADMIN, USER, check, api, login, useAuth, launch, watch, shot, run };
