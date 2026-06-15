// Headless check for the console line editor: ←/→ arrows, mid-line insert + backspace.
// Asserts the SUBMITted line (logged as SUBMIT:<line>) reflects cursor-aware edits.
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4319/";
const execPath =
  process.env.CHROME_BIN || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const logs = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({
  executablePath: execPath, headless: true, args: ["--no-sandbox", "--disable-setuid-sandbox"]
});
async function waitEq(s, ms = 30000) {
  const d = Date.now() + ms;
  while (Date.now() < d) { if (logs.some((l) => l === s)) return true; await sleep(120); }
  throw new Error(`timeout: ${s}\n${logs.join("\n")}`);
}
const eq = (s) => logs.some((l) => l === s);

try {
  const page = await browser.newPage();
  page.on("console", (m) => logs.push(m.text()));
  await page.evaluateOnNewDocument(() => localStorage.clear());
  await page.goto(url, { waitUntil: "load", timeout: 60000 });
  await waitEq("LESSON:PHASE Visa").catch(() => {});
  // advance to a prompt that accepts input
  const focusOnce = async () => { await page.click("#console").catch(() => {}); await page.focus(".xterm-helper-textarea").catch(() => {}); };
  const key = async (k) => { await page.keyboard.press(k); await sleep(20); };
  const type = async (s) => { await page.keyboard.type(s, { delay: 8 }); };
  // wait for engine + OvaParts
  await sleep(1000); await focusOnce();
  { const d = Date.now() + 60000; while (Date.now() < d && !logs.includes("LESSON:PHASE OvaParts")) {
      if (logs.includes("LESSON:PHASE Visa")) await key("Enter");
      if (logs.includes("LESSON:PHASE Instruera")) await key("Enter");
      await sleep(300);
    } }
  if (!logs.includes("LESSON:PHASE OvaParts")) throw new Error("did not reach OvaParts\n" + logs.join("\n"));
  await focusOnce();
  await sleep(300);

  // 1) insert with ←: type "AC", ←, "B"  → "ABC"
  await type("AC"); await key("ArrowLeft"); await type("B"); await key("Enter");
  await waitEq("SUBMIT:ABC");

  // 2) backspace mid-line: "AXB", ← (between X and B), Backspace → "AB"
  await type("AXB"); await key("ArrowLeft"); await key("Backspace"); await key("Enter");
  await waitEq("SUBMIT:AB");

  // 3) → arrow: "AB", ←←, → (cursor=1), "X" → "AXB"
  await type("AB"); await key("ArrowLeft"); await key("ArrowLeft"); await key("ArrowRight"); await type("X"); await key("Enter");
  await waitEq("SUBMIT:AXB");

  const checks = [
    ["← then insert mid-line", eq("SUBMIT:ABC"), "AC ← B → ABC"],
    ["backspace mid-line", eq("SUBMIT:AB"), "AXB ← ⌫ → AB"],
    ["→ arrow then insert", eq("SUBMIT:AXB"), "AB ←← → X → AXB"]
  ];
  let ok = true;
  for (const [n, p, d] of checks) { console.log(`${p ? "PASS" : "FAIL"}  ${n}  — ${d}`); ok = ok && p; }
  if (!ok) logs.forEach((l) => console.log(JSON.stringify(l)));
  await browser.close();
  process.exit(ok ? 0 : 1);
} catch (e) { console.log("ERROR " + e.message); await browser.close(); process.exit(1); }
