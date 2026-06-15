// Headless acceptance for the Milestone 0 interop spike (T015).
// Loads the built app in headless Chromium, waits for the engine to boot, and asserts the
// three acceptance signals the app logs: SPIKE:READY <version>, SPIKE:QUERY ..=42, SPIKE:ERROR ..
//
// Usage: node scripts/spike-e2e.mjs <url>   (chromium via CHROME_BIN or /opt/homebrew/bin/chromium)
import puppeteer from "puppeteer-core";

const url = process.argv[2] || "http://localhost:4173/";
const execPath = process.env.CHROME_BIN || "/opt/homebrew/bin/chromium";

const logs = [];
const browser = await puppeteer.launch({
  executablePath: execPath,
  headless: true,
  args: ["--no-sandbox", "--disable-setuid-sandbox"]
});
try {
  const page = await browser.newPage();
  page.on("console", (m) => logs.push(m.text()));
  page.on("pageerror", (e) => logs.push("PAGEERROR " + e.message));
  await page.goto(url, { waitUntil: "load", timeout: 60000 });

  // Wait until all three spike signals (or a failure) have been logged.
  const deadline = Date.now() + 60000;
  const seen = () => ({
    ready: logs.find((l) => l.startsWith("SPIKE:READY")),
    query: logs.find((l) => l.startsWith("SPIKE:QUERY ")),
    error: logs.find((l) => l.startsWith("SPIKE:ERROR ")),
    failed: logs.find((l) => l.startsWith("SPIKE:FAILED"))
  });
  while (Date.now() < deadline) {
    const s = seen();
    if (s.failed) throw new Error("engine boot failed: " + s.failed);
    if (s.ready && s.query && s.error) break;
    await new Promise((r) => setTimeout(r, 250));
  }

  const s = seen();
  const headerText = await page.$eval("#app", (el) => el.textContent || "").catch(() => "");

  const checks = [
    ["boot → Ready(version)", !!s.ready, s.ready],
    ["SELECT 42 renders (=42)", !!s.query && s.query.includes("42"), s.query],
    ["bad query → typed error", !!s.error, s.error],
    ["status header shows Ready", /Ready — DuckDB/.test(headerText), headerText.slice(0, 80)]
  ];

  let ok = true;
  for (const [name, pass, detail] of checks) {
    console.log(`${pass ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
    ok = ok && pass;
  }
  if (!ok) {
    console.log("--- captured logs ---");
    logs.forEach((l) => console.log(l));
  }
  await browser.close();
  process.exit(ok ? 0 : 1);
} catch (e) {
  console.log("ERROR " + e.message);
  logs.forEach((l) => console.log(l));
  await browser.close();
  process.exit(1);
}
