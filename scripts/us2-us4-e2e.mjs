// Headless acceptance for US4 (replay VISA mid-drill) and US2 (fail PRÖVA → reroute) (T049/T040).
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4319/";
const execPath =
  process.env.CHROME_BIN || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

// Lesson01 reference SQL (must match the lesson).
const PART1 = "SELECT t.trade_id, tr.name FROM trades t JOIN traders tr ON tr.trader_id = t.trader_id";
const PART2 = "SELECT name FROM traders WHERE trader_id NOT IN (SELECT trader_id FROM trades)";
const WHOLE =
  "SELECT tr.name, COALESCE(SUM(t.qty), 0) AS total FROM traders tr LEFT JOIN trades t ON t.trader_id = tr.trader_id GROUP BY tr.name";
const WRONG_EXAM = "SELECT 1 AS x"; // not the orphan-instruments answer → fails PRÖVA

const logs = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({
  executablePath: execPath,
  headless: true,
  args: ["--no-sandbox", "--disable-setuid-sandbox"]
});

async function waitFor(substr, timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (logs.some((l) => l.includes(substr))) return true;
    await sleep(150);
  }
  throw new Error(`timeout waiting for: ${substr}\n${logs.join("\n")}`);
}
const count = (substr) => logs.filter((l) => l.includes(substr)).length;
async function waitForCount(substr, n, timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (count(substr) >= n) return true;
    await sleep(150);
  }
  throw new Error(`timeout waiting for ${n}× ${substr} (saw ${count(substr)})\n${logs.join("\n")}`);
}

try {
  const page = await browser.newPage();
  page.on("console", (m) => logs.push(m.text()));
  page.on("pageerror", (e) => logs.push("PAGEERROR " + e.message));
  await page.goto(url, { waitUntil: "load", timeout: 60000 });

  await waitFor("SPIKE:READY", 60000);
  await waitFor("LESSON:PHASE Visa");

  const term = async () => {
    await page.click("#console").catch(() => {});
    await page.focus(".xterm-helper-textarea").catch(() => {});
  };
  const enter = async () => { await term(); await page.keyboard.press("Enter"); };
  const typeLine = async (s) => { await term(); await page.keyboard.type(s, { delay: 1 }); await page.keyboard.press("Enter"); };

  await enter(); await waitFor("LESSON:PHASE Instruera");
  await enter(); await waitFor("LESSON:PHASE OvaParts");

  // US4: replay the demo mid-drill, then keep going from the same drill.
  await typeLine("repeat-demo");
  await waitFor("LESSON:REPLAY");

  await typeLine(PART1); await waitFor("LESSON:DRILLPASS inner-join");
  await typeLine(PART2); await waitFor("LESSON:DRILLPASS orphan-traders");
  await waitFor("LESSON:PHASE OvaWhole");
  await typeLine(WHOLE); await waitFor("LESSON:WHOLEPASS");
  await enter(); await waitFor("LESSON:PHASE Prova");

  // US4: repeat-demo is refused during PRÖVA.
  await typeLine("repeat-demo");
  await waitFor("LESSON:REPLAY-REFUSED");

  // US2: a wrong PRÖVA answer fails and reroutes to the drills.
  await typeLine(WRONG_EXAM);
  await waitFor("LESSON:GRADE");
  await waitFor("LESSON:REFLECT");
  await waitForCount("LESSON:PHASE OvaParts", 2); // re-entered ÖVA(parts)

  const gradeLog = logs.find((l) => l.startsWith("LESSON:GRADE")) || "";
  const failed = /LESSON:GRADE \d+ false/.test(gradeLog);
  const reflect = logs.find((l) => l.startsWith("LESSON:REFLECT")) || "";

  const checks = [
    ["US4: repeat-demo replays VISA mid-drill", logs.includes("LESSON:REPLAY") || count("LESSON:REPLAY") >= 1, "LESSON:REPLAY"],
    ["US4: repeat-demo refused in PRÖVA", count("LESSON:REPLAY-REFUSED") >= 1, "LESSON:REPLAY-REFUSED"],
    ["US2: wrong PRÖVA → failing grade", failed, gradeLog],
    ["US2: reflection lists drill-again parts", /inner-join|orphan-traders/.test(reflect), reflect],
    ["US2: rerouted back to ÖVA(parts)", count("LESSON:PHASE OvaParts") >= 2, `OvaParts×${count("LESSON:PHASE OvaParts")}`]
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
