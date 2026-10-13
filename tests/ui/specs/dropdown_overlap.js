// Dropdown trong lưới không bị ô lưới phía sau đè (lỗi stacking context của hiệu ứng hiện dần).
const { FE, check, launch, shot, run } = require("../lib");

run(async () => {
    const browser = await launch();
    for (const [theme, width] of [["light", 1440], ["dark", 1440], ["light", 390]]) {
        const ctx = await browser.newContext({ viewport: { width, height: 900 }, colorScheme: theme });
        const page = await ctx.newPage();
        await page.addInitScript((th) => { localStorage.setItem("tripnova.lang", "vi"); localStorage.setItem("tripnova.theme", th); }, theme);
        await page.goto(`${FE}/ai-planner`);
        await page.locator("#planner-destination").fill("ha");
        const items = page.locator("ul.absolute button");
        await items.first().waitFor({ timeout: 10000 });
        await page.waitForTimeout(1200); // chờ hiệu ứng hiện dần xong
        const n = await items.count();
        let covered = 0;
        const list = await page.locator("ul.absolute").boundingBox();
        for (let i = 0; i < n; i++) {
            const box = await items.nth(i).boundingBox();
            // chỉ mục đang hiện trong khung cuộn của danh sách
            if (!box || box.y + box.height / 2 > Math.min(900, list.y + list.height)) continue;
            // Phần tử trên cùng tại tâm mục phải nằm trong chính mục đó
            const ok = await page.evaluate(({ x, y }) => !!document.elementFromPoint(x, y)?.closest("ul.absolute"), { x: box.x + 40, y: box.y + box.height / 2 });
            if (!ok) covered++;
        }
        check(n > 0 && covered === 0, `${theme} ${width}px: ${n} suggestions, ${covered} covered by content below`);
        // Chọn được mục ở dưới cùng (trước đây bấm trúng nút Ngân sách / phạm vi)
        const last = items.nth(Math.min(n, 5) - 1);
        const name = (await last.locator("span.block").first().innerText()).trim();
        await last.click();
        check((await page.locator("#planner-destination").inputValue()) === name, `${theme} ${width}px: clicking lower suggestion picks it (${name})`);
        await page.locator("#planner-destination").fill("ha");
        await items.first().waitFor();
        await page.waitForTimeout(500);
        await page.screenshot({ path: shot(`picker_fixed_${theme}_${width}.png`), clip: { x: 0, y: 150, width, height: 650 } });
        await ctx.close();
    }
    await browser.close();
});
