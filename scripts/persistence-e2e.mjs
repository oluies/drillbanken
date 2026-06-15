// Headless acceptance for US3 (resume after reload) and US6 (export/import) — T045/T057.
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4319/";
const execPath =
  process.env.CHROME_BIN || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const PART1 = "SELECT t.trade_id, tr.name FROM trades t JOIN traders tr ON tr.trader_id = t.trader_id";
const PART2 = "SELECT name FROM traders WHERE trader_id NOT IN (SELECT trader_id FROM trades)";

let logs = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({
  executablePath: execPath,
  headless: true,
  args: ["--no-sandbox", "--disable-setuid-sandbox"]
});

async function waitFor(substr, timeoutMs = 60000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (logs.some((l) => l.includes(substr))) return true;
    await sleep(150);
  }
  throw new Error(`timeout waiting for: ${substr}\n${logs.join("\n")}`);
}
const has = (s) => logs.some((l) => l.includes(s));

const results = [];
const check = (name, pass, detail) => { results.push([name, pass, detail]); };

try {
  const page = await browser.newPage();
  page.on("console", (m) => logs.push(m.text()));
  page.on("pageerror", (e) => logs.push("PAGEERROR " + e.message));

  const term = async () => {
    await page.click("#console").catch(() => {});
    await page.focus(".xterm-helper-textarea").catch(() => {});
  };
  const enter = async () => { await term(); await page.keyboard.press("Enter"); };
  const typeLine = async (s) => { await term(); await page.keyboard.type(s, { delay: 1 }); await page.keyboard.press("Enter"); };

  // --- play partway: pass part 1 ---
  await page.goto(url, { waitUntil: "load", timeout: 60000 });
  await waitFor("SPIKE:READY");
  await waitFor("LESSON:PHASE Visa");
  await enter(); await waitFor("LESSON:PHASE Instruera");
  await enter(); await waitFor("LESSON:PHASE OvaParts");
  await typeLine(PART1); await waitFor("LESSON:DRILLPASS inner-join");
  const exportA = await page.evaluate(() => window.drillExport());

  // --- US3: reload resumes at the same phase, part 1 still passed ---
  logs = [];
  await page.reload({ waitUntil: "load", timeout: 60000 });
  await waitFor("SPIKE:READY");
  await waitFor("LESSON:RESUME OvaParts");
  check("US3: reload resumes at OvaParts", has("LESSON:RESUME OvaParts"), "LESSON:RESUME OvaParts");
  await typeLine(PART2);
  await waitFor("LESSON:DRILLPASS orphan-traders");
  check("US3: part 1 stayed passed (advanced on part 2)", has("LESSON:DRILLPASS orphan-traders"), "continued without redoing part 1");

  // --- US6: clear → fresh start (no resume) ---
  await page.evaluate(() => localStorage.clear());
  logs = [];
  await page.reload({ waitUntil: "load", timeout: 60000 });
  await waitFor("SPIKE:READY");
  await waitFor("LESSON:PHASE Visa");
  await sleep(500);
  check("US6: after clear, fresh start (no resume)", has("LESSON:PHASE Visa") && !has("LESSON:RESUME"), "Visa, no RESUME");

  // --- US6: import the earlier export → reload resumes again ---
  const imported = await page.evaluate((j) => window.drillImport(j), exportA);
  check("US6: importFrom accepted the JSON", imported === true, `drillImport=${imported}`);
  logs = [];
  await page.reload({ waitUntil: "load", timeout: 60000 });
  await waitFor("SPIKE:READY");
  await waitFor("LESSON:RESUME OvaParts");
  check("US6: imported progress restored on reload", has("LESSON:RESUME OvaParts"), "LESSON:RESUME OvaParts");

  let ok = true;
  for (const [name, pass, detail] of results) {
    console.log(`${pass ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
    ok = ok && pass;
  }
  if (!ok) { console.log("--- logs ---"); logs.forEach((l) => console.log(l)); }
  await browser.close();
  process.exit(ok ? 0 : 1);
} catch (e) {
  console.log("ERROR " + e.message);
  logs.forEach((l) => console.log(l));
  await browser.close();
  process.exit(1);
}
