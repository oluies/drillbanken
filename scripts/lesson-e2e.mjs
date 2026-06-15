// Headless acceptance for User Story 1 (T036): play the full lesson loop end to end and
// assert a passing PRÖVA grade + completion. Drives the xterm console by typing reference
// SQL for each phase (which matches the reference result → Pass).
//
// Usage: node scripts/lesson-e2e.mjs <url>
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4319/";
const execPath =
  process.env.CHROME_BIN || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

// Reference SQL — must match modules/content/.../lessons/Lesson01.scala.
const PART1 = "SELECT t.trade_id, tr.name FROM trades t JOIN traders tr ON tr.trader_id = t.trader_id";
const PART2 = "SELECT name FROM traders WHERE trader_id NOT IN (SELECT trader_id FROM trades)";
const WHOLE =
  "SELECT tr.name, COALESCE(SUM(t.qty), 0) AS total FROM traders tr LEFT JOIN trades t ON t.trader_id = tr.trader_id GROUP BY tr.name";
const EXAM = "SELECT symbol FROM instruments WHERE instrument_id NOT IN (SELECT instrument_id FROM trades)";

const logs = [];
const browser = await puppeteer.launch({
  executablePath: execPath,
  headless: true,
  args: ["--no-sandbox", "--disable-setuid-sandbox"]
});

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function waitForLog(substr, timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (logs.some((l) => l.includes(substr))) return true;
    await sleep(150);
  }
  throw new Error(`timeout waiting for log containing: ${substr}\n--- logs ---\n${logs.join("\n")}`);
}

try {
  const page = await browser.newPage();
  page.on("console", (m) => logs.push(m.text()));
  page.on("pageerror", (e) => logs.push("PAGEERROR " + e.message));
  await page.goto(url, { waitUntil: "load", timeout: 60000 });

  // engine boots and the lesson starts in VISA
  await waitForLog("SPIKE:READY", 60000);
  await waitForLog("LESSON:PHASE Visa");

  const term = async () => {
    await page.click("#console").catch(() => {});
    await page.focus(".xterm-helper-textarea").catch(() => {});
  };
  const enter = async () => { await term(); await page.keyboard.press("Enter"); };
  const typeLine = async (sql) => { await term(); await page.keyboard.type(sql, { delay: 1 }); await page.keyboard.press("Enter"); };

  await enter();                         // Visa -> Instruera
  await waitForLog("LESSON:PHASE Instruera");
  await enter();                         // Instruera -> OvaParts
  await waitForLog("LESSON:PHASE OvaParts");

  await typeLine(PART1);
  await waitForLog("LESSON:DRILLPASS inner-join");
  await typeLine(PART2);
  await waitForLog("LESSON:DRILLPASS orphan-traders");
  await waitForLog("LESSON:PHASE OvaWhole");

  await typeLine(WHOLE);
  await waitForLog("LESSON:WHOLEPASS");
  await enter();                         // OvaWhole -> Prova
  await waitForLog("LESSON:PHASE Prova");

  await typeLine(EXAM);
  await waitForLog("LESSON:GRADE");
  await waitForLog("LESSON:COMPLETE");

  const gradeLog = logs.find((l) => l.startsWith("LESSON:GRADE")) || "";
  const m = gradeLog.match(/LESSON:GRADE (\d+) (true|false)/);
  const points = m ? parseInt(m[1], 10) : -1;
  const passed = m ? m[2] === "true" : false;

  const checks = [
    ["lesson reached PRÖVA and graded", !!m, gradeLog],
    ["PRÖVA passed", passed, gradeLog],
    ["full marks (100) on clean run", points === 100, `points=${points}`],
    ["lesson completed", logs.includes("LESSON:COMPLETE") || logs.some((l) => l === "LESSON:COMPLETE"), "LESSON:COMPLETE"]
  ];
  let ok = true;
  for (const [name, pass, detail] of checks) {
    console.log(`${pass ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
    ok = ok && pass;
  }
  if (!ok) { console.log("--- logs ---"); logs.forEach((l) => console.log(l)); }
  await browser.close();
  process.exit(ok ? 0 : 1);
} catch (e) {
  console.log("ERROR " + e.message);
  await browser.close();
  process.exit(1);
}
