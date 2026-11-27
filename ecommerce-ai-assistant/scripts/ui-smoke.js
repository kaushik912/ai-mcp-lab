// Browser smoke test for the Phase 5 UI. Drives login, a demo chip, the X-Ray drawer, a memory follow-up,
// a local+MCP return, the user switcher and reset; prints what it sees and saves screenshots.
//
// Setup (once):   cd scripts && npm i playwright-core@1.49.1
// Run:            CHROME_PATH=/path/to/chrome node scripts/ui-smoke.js /tmp/shots
// Needs both apps running and OPENROUTER_API_KEY set for the storefront. Makes ~3 LLM calls.
const path = require('path');
const fs = require('fs');
const { chromium } = require(path.join(__dirname, 'node_modules', 'playwright-core'));

const out = process.argv[2] || 'ui-shots';
fs.mkdirSync(out, { recursive: true });
const exe = process.env.CHROME_PATH;
if (!exe) { console.error('Set CHROME_PATH to a Chrome/Chromium binary'); process.exit(2); }
const BASE = process.env.BASE_URL || 'http://localhost:8080';
const shot = (page, name) => page.screenshot({ path: path.join(out, name) });

async function waitForReply(page, before) {
  await page.waitForFunction((n) => document.querySelectorAll('details.xray').length > n, before, { timeout: 120000 });
  await page.waitForTimeout(800); // orders/products refresh after the reply
  return (await page.locator('details.xray').last().locator('summary').innerText()).replace(/\s+/g, ' ');
}

async function ask(page, text) {
  const before = await page.locator('details.xray').count();
  await page.fill('#chat-input', text);
  await page.click('#chat-send');
  return waitForReply(page, before);
}

(async () => {
  const browser = await chromium.launch({ executablePath: exe, args: ['--no-sandbox'] });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });

  await page.goto(BASE + '/');
  console.log('1. unauthenticated -> ', new URL(page.url()).pathname);
  await shot(page, '01-login.png');

  await page.click('#login-alex');
  await page.waitForSelector('#orders-body tr');
  await Promise.all([page.waitForNavigation(), page.click('#reset-btn')]);  // reset reloads the page
  await page.waitForSelector('#orders-body tr');
  console.log('2. badge:', (await page.innerText('#user-badge')).replace(/\s+/g, ' ').trim(),
    '| orders:', await page.locator('#orders-body tr').count(), '| products:', await page.locator('#product-grid > div').count());
  await shot(page, '02-alex-home.png');

  await page.locator('#demo-chips button', { hasText: '36-day jacket' }).click();
  console.log('3. jacket X-Ray:', await waitForReply(page, 0));
  await page.locator('details.xray').last().locator('summary').click();
  await shot(page, '03-jacket-xray.png');

  console.log('4. follow-up X-Ray:', await ask(page, 'Okay, give me the exchange voucher then'));
  console.log('   ORD-1003 row:', (await page.innerText('#orders-body tr[data-order="ORD-1003"]')).replace(/\s+/g, ' '));

  console.log('5. return+MCP X-Ray:', await ask(page, 'Return my running shoes from ORD-1001 and find a drop-off point'));
  await page.locator('details.xray').last().locator('summary').click();
  console.log('   ORD-1001 row:', (await page.innerText('#orders-body tr[data-order="ORD-1001"]')).replace(/\s+/g, ' '));
  console.log('   SummitGrip stock:', (await page.innerText('[data-sku="SUMMIT-SHOE-01"] .stock')).trim());
  await shot(page, '04-return-mcp-xray.png');

  await page.click('#switch-priya');
  await page.waitForSelector('#orders-body tr');
  console.log('6. after switch badge:', (await page.innerText('#user-badge')).replace(/\s+/g, ' ').trim(),
    '| orders:', (await page.locator('#orders-body tr').allInnerTexts()).map((t) => t.split(/\s/)[0]),
    '| chat empty:', (await page.locator('details.xray').count()) === 0);
  await shot(page, '05-priya.png');

  await Promise.all([page.waitForNavigation(), page.click('#reset-btn')]);
  await page.waitForSelector('#orders-body tr');
  await page.click('#switch-alex');
  await page.waitForSelector('#orders-body tr');
  console.log('7. after reset any RMA/VCH badge:', /RMA-|VCH-/.test(await page.innerText('#orders-body')));

  await page.setViewportSize({ width: 390, height: 844 });
  console.log('8. phone width horizontal overflow:', await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth));
  console.log('9. JS errors:', errors.length ? errors : 'none');
  console.log('Screenshots in', path.resolve(out));
  await browser.close();
})().catch((e) => { console.error('FAILED:', e); process.exit(1); });
