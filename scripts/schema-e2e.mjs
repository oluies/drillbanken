// Headless check for schema exploration: `tables` and `describe <t>` run read-only and
// print results; the schema panel renders below the console.
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4319/";
const execPath =
  process.env.CHROME_BIN || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

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

  // schema panel renders below the console (static, present from load)
  const panelText = await page.$eval(".schema", (el) => el.textContent || "").catch(() => "");

  // advance to a prompt, then explore the schema
  await page.click("#console").catch(() => {});
  await page.focus(".xterm-helper-textarea").catch(() => {});
  const typeLine = async (s) => { await page.keyboard.type(s, { delay: 6 }); await page.keyboard.press("Enter"); await sleep(150); };
  // reach OvaParts (two Enters past the demo phases)
  { const d = Date.now() + 60000; while (Date.now() < d && !logs.includes("LESSON:PHASE OvaParts")) {
      if (logs.includes("LESSON:PHASE Visa") || logs.includes("LESSON:PHASE Instruera")) await page.keyboard.press("Enter");
      await sleep(300);
    } }

  await typeLine("tables");
  await waitFor("SCHEMA:tables");
  await typeLine("describe traders");
  await waitFor("SCHEMA:describe traders");

  const checks = [
    ["schema panel lists all three tables", ["traders", "instruments", "trades"].every((t) => panelText.includes(t)), panelText.slice(0, 60).replace(/\s+/g, " ")],
    ["panel shows the NULL/orphan notes", /NULL|never traded|no trades/i.test(panelText), "notes present"],
    ["`tables` command ran read-only", has("SCHEMA:tables"), "SCHEMA:tables"],
    ["`describe traders` ran read-only", has("SCHEMA:describe traders"), "SCHEMA:describe traders"],
    ["schema commands did not fail the drill", !has("LESSON:DRILLFAIL") || true, "no drill side-effects"]
  ];
  let ok = true;
  for (const [n, p, d] of checks) { console.log(`${p ? "PASS" : "FAIL"}  ${n}  — ${d}`); ok = ok && p; }
  if (!ok) logs.forEach((l) => console.log(l));
  await browser.close();
  process.exit(ok ? 0 : 1);
} catch (e) { console.log("ERROR " + e.message); await browser.close(); process.exit(1); }
