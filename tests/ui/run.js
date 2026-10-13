// Chạy lần lượt các spec trong ./specs (hoặc các tên truyền vào: node run.js smoke routing).
const { spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const dir = path.join(__dirname, "specs");
const wanted = process.argv.slice(2);
const specs = fs.readdirSync(dir)
    .filter((f) => f.endsWith(".js"))
    .filter((f) => !wanted.length || wanted.includes(f.replace(/\.js$/, "")))
    .sort((a, b) => (a === "smoke.js" ? -1 : b === "smoke.js" ? 1 : a.localeCompare(b)));

const results = [];
for (const spec of specs) {
    console.log(`\n===== ${spec}`);
    const started = Date.now();
    const res = spawnSync(process.execPath, [path.join(dir, spec)], { stdio: "inherit", cwd: __dirname });
    results.push({ spec, code: res.status, seconds: Math.round((Date.now() - started) / 1000) });
}
console.log("\n===== Tổng kết");
for (const r of results) console.log(`${r.code === 0 ? "PASS" : "FAIL"}  ${r.spec}  (${r.seconds}s)`);
process.exit(results.every((r) => r.code === 0) ? 0 : 1);
