// Drives a real Chrome over the DevTools protocol against the screenshots.sh setup:
// 1) the page loads the seeded day, 2) a KPI stored via REST reaches the cards through
// the /livedata WebSocket (the frontend polls the data accessor every 60 s),
// 3) the previous/next-day arrows switch the date and reload the history.
// Usage: node live-check.mjs <chrome-profile-dir>   (CHROME env var = Chrome binary)
import { spawn } from "node:child_process";
import { setTimeout as sleep } from "node:timers/promises";

const chrome = spawn(process.env.CHROME, ["--headless=new", "--remote-debugging-port=9333",
  "--user-data-dir=" + process.argv[2], "about:blank"], { stdio: "ignore" });
let id = 0;
try {
  let target;
  for (let i = 0; i < 50 && !target; i++) {
    try { target = (await (await fetch("http://127.0.0.1:9333/json")).json()).find(t => t.type === "page"); }
    catch { await sleep(200); }
  }
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.onopen = r);
  const pending = new Map();
  ws.onmessage = m => { const d = JSON.parse(m.data); if (pending.has(d.id)) { pending.get(d.id)(d.result); pending.delete(d.id); } };
  const send = (method, params = {}) => new Promise(r => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params })); });
  const evalJs = async expr => (await send("Runtime.evaluate", { expression: expr, returnByValue: true })).result.value;
  const cards = () => evalJs(`[...document.querySelectorAll('app-mini-card')].map(c => c.innerText.replace(/\\s+/g,' ').trim())`);
  const date = () => evalJs(`document.querySelector('app-datepicker input')?.value`);
  // Per chart: number of marker points / number of segments in the first line path.
  const points = () => evalJs(`[...document.querySelectorAll('highcharts-chart')].map(c => c.querySelectorAll('.highcharts-point').length + '/' + (c.querySelector('path.highcharts-graph')?.getAttribute('d')||'').split('L').length)`);

  await send("Page.navigate", { url: "http://localhost:4300/" });
  await sleep(8000);
  const before = await cards();
  console.log("load: date", await date(), "| chart points", JSON.stringify(await points()));
  console.log("cards before:", JSON.stringify(before));

  // Later than the seeded 20:00 point: the latest KPI is the one with the newest timestamp,
  // and the frontend ignores older ones.
  const at = new Date(); at.setHours(20, 10, 0, 0);
  await fetch("http://localhost:8083/v1/store-kpi-values", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({
    jobId: "live", dateCreated: at.getTime(), producing: { w: 4321 }, exporting: { w: 1111 }, importing: { w: 222 },
    consuming: { w: 3333 }, selfConsuming: { w: 3210 }, produced: { wh: 99000 }, exported: { wh: 55000 },
    imported: { wh: 2000 }, consumed: { wh: 20000 }, selfConsumed: { wh: 18000 } }) });
  let after = before;
  for (let i = 0; i < 80 && JSON.stringify(after) === JSON.stringify(before); i++) { await sleep(1000); after = await cards(); }
  console.log("cards after live push:", JSON.stringify(after));
  console.log(JSON.stringify(after) !== JSON.stringify(before) ? "LIVE UPDATE OK" : "LIVE UPDATE FAILED (no change within 80 s)");

  await evalJs(`document.querySelector('.datepicker-left-arrow').click()`);
  await sleep(4000);
  console.log("after previous-day click: date", await date(), "| chart points", JSON.stringify(await points()));
  await evalJs(`document.querySelector('.datepicker-right-arrow').click()`);
  await sleep(4000);
  console.log("after next-day click: date", await date(), "| chart points", JSON.stringify(await points()));
  ws.close();
} finally { chrome.kill(); }
