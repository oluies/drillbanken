// Headless acceptance for the language toggle (T065, FR-027/SC-011): toggle mid-drill,
// confirm the switch is recorded and progress is preserved (the drill still passes).
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4319/";
const execPath =
  process.env.CHROME_BIN || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const PART1 = "SELECT t.trade_id, tr.name FROM trades t JOIN traders tr ON tr.trader_id = t.trader_id";

const logs = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({
  executablePath: execPath, headless: true, args: ["--no-sandbox", "--disable-setuid-sandbox"]
});
async function waitFor(s, ms = 60000) {
  const d = Date.now() + ms;
  while (Date.now() < d) { if (logs.some((l) => l.includes(s))) return true; await sleep(150); }
  throw new Error(`timeout: ${s}\n${logs.join("\n")}`);
}
const has = (s) => logs.some((l) => l.includes(s));
try {
  const page = await browser.newPage();
  page.on("console", (m) => logs.push(m.text()));
  await page.evaluateOnNewDocument(() => localStorage.clear());
  await page.goto(url, { waitUntil: "load", timeout: 60000 });
  await waitFor("SPIKE:READY");
  await waitFor("LESSON:PHASE Visa");
  const term = async () => { await page.click("#console").catch(() => {}); await page.focus(".xterm-helper-textarea").catch(() => {}); };
  const enter = async () => { await term(); await page.keyboard.press("Enter"); };
  const typeLine = async (s) => { await term(); await page.keyboard.type(s, { delay: 1 }); await page.keyboard.press("Enter"); };

  await enter(); await waitFor("LESSON:PHASE Instruera");
  await enter(); await waitFor("LESSON:PHASE OvaParts");

  await typeLine("lang");                 // default En → Sv
  await waitFor("LESSON:LANG Sv");
  await typeLine(PART1);                  // progress must be intact after the toggle
  await waitFor("LESSON:DRILLPASS inner-join");
  // language persisted to ProgressState
  const lang = await page.evaluate(() => JSON.parse(window.drillExport()).language);

  const checks = [
    ["toggle switches language mid-drill", has("LESSON:LANG Sv"), "LESSON:LANG Sv"],
    ["progress preserved across toggle", has("LESSON:DRILLPASS inner-join"), "drill still passed"],
    ["language persisted to progress", lang === "Sv", `language=${lang}`]
  ];
  let ok = true;
  for (const [n, p, d] of checks) { console.log(`${p ? "PASS" : "FAIL"}  ${n}${d ? "  — " + d : ""}`); ok = ok && p; }
  if (!ok) logs.forEach((l) => console.log(l));
  await browser.close();
  process.exit(ok ? 0 : 1);
} catch (e) { console.log("ERROR " + e.message); await browser.close(); process.exit(1); }
