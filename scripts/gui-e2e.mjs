// Headless acceptance for the guided web GUI (constitution v2.0.0). Drives the lesson via
// the view's test hooks (set SQL + click-equivalents), covering the full loop, US2 reroute,
// US3 resume-after-reload, and the language toggle. Verifies the GUI + CodeMirror render.
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4319/";
const execPath =
  process.env.CHROME_BIN || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const PART1 = "SELECT t.trade_id, tr.name FROM trades t JOIN traders tr ON tr.trader_id = t.trader_id";
const PART2 = "SELECT name FROM traders WHERE trader_id NOT IN (SELECT trader_id FROM trades)";
const WHOLE = "SELECT tr.name, COALESCE(SUM(t.qty), 0) AS total FROM traders tr LEFT JOIN trades t ON t.trader_id = tr.trader_id GROUP BY tr.name";
const EXAM = "SELECT symbol FROM instruments WHERE instrument_id NOT IN (SELECT instrument_id FROM trades)";

let logs = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({ executablePath: execPath, headless: true, args: ["--no-sandbox", "--disable-setuid-sandbox"] });
async function waitFor(s, ms = 60000) {
  const d = Date.now() + ms;
  while (Date.now() < d) { if (logs.some((l) => l.includes(s))) return true; await sleep(120); }
  throw new Error(`timeout: ${s}\n${logs.join("\n")}`);
}
const has = (s) => logs.some((l) => l.includes(s));
const results = [];
const check = (n, p, d) => results.push([n, p, d]);

let page;
const call = (fn, ...a) => page.evaluate((f, args) => window[f](...args), fn, a);
const setRun = async (sql, wait) => { await call("drillSetSql", sql); await sleep(60); await call("drillRun"); await waitFor(wait); };

try {
  page = await browser.newPage();
  page.on("console", (m) => logs.push(m.text()));
  page.on("pageerror", (e) => logs.push("PAGEERROR " + e.message));

  // --- boot + GUI renders ---
  await page.goto(url, { waitUntil: "load", timeout: 60000 });
  await waitFor("SPIKE:READY");
  await waitFor("LESSON:PHASE Visa");
  await sleep(400);
  const dom = await page.evaluate(() => ({
    cm: !!document.querySelector(".cm-editor"),
    run: [...document.querySelectorAll("button")].some((b) => b.textContent.includes("Run")),
    schema: !!document.querySelector(".schema")
  }));
  check("GUI renders CodeMirror editor", dom.cm, "");
  check("GUI renders a Run button", dom.run, "");
  check("schema panel present below", dom.schema, "");

  // --- demo phases → practice ---
  await call("drillContinue"); await waitFor("LESSON:PHASE Instruera");
  await call("drillContinue"); await waitFor("LESSON:PHASE OvaParts");

  // language toggle (default En → Sv), progress preserved
  await call("drillLang"); await waitFor("LESSON:LANG Sv");
  check("language toggle works", has("LESSON:LANG Sv"), "En→Sv");

  // --- pass part 1, then test resume across reload (US3) ---
  await setRun(PART1, "LESSON:DRILLPASS inner-join");
  logs = [];
  await page.reload({ waitUntil: "load", timeout: 60000 });
  await waitFor("SPIKE:READY");
  await waitFor("LESSON:RESUME OvaParts");
  check("US3: resumes at OvaParts after reload", has("LESSON:RESUME OvaParts"), "");

  // --- finish parts → whole → PRÖVA, force a fail → reroute (US2) ---
  await setRun(PART2, "LESSON:DRILLPASS orphan-traders");
  await waitFor("LESSON:PHASE OvaWhole");
  await setRun(WHOLE, "LESSON:WHOLEPASS");
  await call("drillToProva"); await waitFor("LESSON:PHASE Prova");
  await setRun("SELECT 1 AS x", "LESSON:GRADE");
  check("US2: wrong PRÖVA fails", has("LESSON:GRADE") && /LESSON:GRADE \d+ false/.test(logs.find((l)=>l.startsWith("LESSON:GRADE"))||""), "fail grade");
  await waitFor("LESSON:REFLECT");
  await call("drillAgain"); await waitFor("LESSON:PHASE OvaParts");
  check("US2: reroute back to ÖVA(parts)", has("LESSON:REFLECT"), "drill again");

  // --- re-pass everything → pass PRÖVA → complete ---
  await setRun(PART1, "LESSON:DRILLPASS inner-join");
  await setRun(PART2, "LESSON:DRILLPASS orphan-traders");
  await waitFor("LESSON:PHASE OvaWhole");
  await setRun(WHOLE, "LESSON:WHOLEPASS");
  await call("drillToProva"); await waitFor("LESSON:PHASE Prova");
  await setRun(EXAM, "LESSON:COMPLETE");
  // 2nd PRÖVA attempt after the reroute → passes with an attempt penalty (90, not 100).
  const grade = logs.find((l) => l.startsWith("LESSON:GRADE") && l.includes("true")) || "";
  check("full loop completes with a pass", has("LESSON:COMPLETE") && grade !== "", grade);

  let ok = true;
  for (const [n, p, d] of results) { console.log(`${p ? "PASS" : "FAIL"}  ${n}${d ? "  — " + d : ""}`); ok = ok && p; }
  if (!ok) logs.forEach((l) => console.log(l));
  await browser.close();
  process.exit(ok ? 0 : 1);
} catch (e) { console.log("ERROR " + e.message); logs.forEach((l)=>console.log(l)); await browser.close(); process.exit(1); }
