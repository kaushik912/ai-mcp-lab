// Generating a quiz, answering it, and how questions are displayed.
const { test, expect } = require('@playwright/test');
const { CODE_SNIPPET, fakeQuiz, fakeQuizApi, openApp, generateQuiz } = require('./helpers');

test('grades the answers in the browser', async ({ page }) => {
  await fakeQuizApi(page);
  await openApp(page);
  await generateQuiz(page);

  // The right answer is always option index 1. Answer 3 questions right and 1 wrong.
  for (const n of [0, 1, 2]) await page.locator(`input[name="q${n}"][value="1"]`).check();
  await page.locator('input[name="q3"][value="0"]').check();
  await page.click('#submit');

  await expect(page.locator('#score')).toHaveText('Score: 3 / 10');
  await expect(page.locator('.q').first().locator('.explain')).toContainText('Correct');
  await expect(page.locator('.q').nth(3).locator('.explain')).toContainText('Incorrect');
  await expect(page.locator('.q').nth(3).locator('.opt.wrong')).toHaveCount(1);
});

test('shows the error message when generation fails', async ({ page }) => {
  await page.route('**/api/quiz', (route) =>
    route.fulfill({ status: 401, json: { error: 'OpenRouter rejected the API key' } }));
  await openApp(page);
  await page.fill('#key', 'bad-key');
  await page.click('#generate');

  await expect(page.locator('#status')).toHaveText('OpenRouter rejected the API key');
  await expect(page.locator('#quiz')).toBeHidden();
  await expect(page.locator('#generate')).toBeEnabled(); // can try again
});

test('a code snippet keeps its line breaks and uses a monospace font', async ({ page }) => {
  await fakeQuizApi(page, (topic) => fakeQuiz(topic, { 1: { scenario: CODE_SNIPPET } }));
  await openApp(page);
  await generateQuiz(page);

  const code = page.locator('.scenario.code');
  await expect(code).toHaveCount(1); // only the multi-line scenario is treated as code
  await expect(code).toHaveCSS('white-space', 'pre-wrap');
  expect(await code.evaluate((e) => getComputedStyle(e).fontFamily)).toMatch(/mono/i);
  await expect(page.locator('.scenario:not(.code)')).toHaveCount(4); // the other prose scenarios
});

test('model text is shown as text, never run as HTML', async ({ page }) => {
  await fakeQuizApi(page, (topic) =>
    fakeQuiz(topic, { 0: { question: 'Evil <img src=x onerror="window.__pwned=1"> ?' } }));
  await openApp(page);
  await generateQuiz(page);

  await expect(page.locator('.q').first()).toContainText('<img src=x'); // visible as plain text
  await expect(page.locator('.q img')).toHaveCount(0);
  expect(await page.evaluate(() => window.__pwned)).toBeUndefined();
});

test('the API key is never stored in the browser', async ({ page, context }) => {
  await fakeQuizApi(page);
  await openApp(page);
  await generateQuiz(page, 'sk-or-SUPER-SECRET');
  await page.locator('button.star').first().click(); // also exercise saving something

  const stored = await page.evaluate(() => JSON.stringify({
    local: { ...localStorage }, session: { ...sessionStorage }, cookies: document.cookie,
  }));
  expect(stored).not.toContain('SUPER-SECRET');
  expect(await context.cookies()).toHaveLength(0);
});
