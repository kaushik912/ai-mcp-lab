// Favorites: star a question, see it in the Saved tab, export/import, and the edge cases.
const fs = require('fs');
const { test, expect } = require('@playwright/test');
const { fakeQuizApi, openApp, generateQuiz, star, favorite, FAV_KEY } = require('./helpers');

test.beforeEach(async ({ page }) => {
  await fakeQuizApi(page);
});

test('a starred question is saved and survives a reload', async ({ page }) => {
  await openApp(page);
  await generateQuiz(page);
  await star(page, 0).click();
  await star(page, 2).click();
  await expect(page.locator('#tab-saved')).toHaveText('Saved (2)');

  await page.reload();
  await expect(page.locator('#tab-saved')).toHaveText('Saved (2)');
});

test('the same question shows as starred when it comes back in a new quiz', async ({ page }) => {
  await openApp(page);
  await generateQuiz(page);
  await star(page, 0).click();

  await page.reload();
  await generateQuiz(page); // the fake API returns the same questions again
  await expect(star(page, 0)).toHaveAttribute('aria-pressed', 'true');
  await expect(star(page, 1)).toHaveAttribute('aria-pressed', 'false');
});

test('starring twice does not save a duplicate', async ({ page }) => {
  await openApp(page);
  await generateQuiz(page);
  await star(page, 0).click();
  await star(page, 0).click(); // un-star
  await star(page, 0).click(); // star again
  await expect(page.locator('#tab-saved')).toHaveText('Saved (1)');
});

test('the Saved tab hides the answer until you tap Reveal', async ({ page }) => {
  await openApp(page);
  await generateQuiz(page);
  await star(page, 0).click();
  await page.click('#tab-saved');

  const card = page.locator('#saved-list .q').first();
  await expect(card.locator('.explain')).toHaveCount(0); // nothing given away yet
  await card.locator('input[type=radio]').first().check(); // pick a wrong option (right one is index 1)
  await card.getByRole('button', { name: 'Reveal answer' }).click();

  await expect(card.locator('.explain')).toContainText('Incorrect');
  await expect(card.locator('.opt.correct')).toHaveCount(1);
});

test('removing a favorite in the Saved tab un-stars it in the quiz tab', async ({ page }) => {
  await openApp(page);
  await generateQuiz(page);
  await star(page, 0).click();
  await page.click('#tab-saved');
  await page.getByRole('button', { name: 'Remove' }).click();
  await expect(page.locator('#tab-saved')).toHaveText('Saved (0)');

  await page.click('#tab-new');
  await expect(star(page, 0)).toHaveAttribute('aria-pressed', 'false');
});

test('export downloads the favorites and never includes the API key', async ({ page }) => {
  await openApp(page);
  await generateQuiz(page, 'sk-or-SUPER-SECRET');
  await star(page, 0).click();
  await page.click('#tab-saved');

  const [download] = await Promise.all([page.waitForEvent('download'), page.click('#export')]);
  expect(download.suggestedFilename()).toBe('favorites.json');
  const text = fs.readFileSync(await download.path(), 'utf8');
  const file = JSON.parse(text);
  expect(file.version).toBe(1);
  expect(file.items).toHaveLength(1);
  expect(text).not.toContain('SUPER-SECRET');
});

test('import adds valid questions and skips invalid or duplicate ones', async ({ page }) => {
  await openApp(page);
  await page.click('#tab-saved');

  const items = [
    favorite(),                                                    // valid
    favorite(),                                                    // duplicate of the one above
    favorite({ question: 'Bad index?', correctIndex: 9 }),         // invalid: index out of range
    favorite({ question: 'Three options?', options: ['a', 'b', 'c'] }), // invalid: needs 4 options
  ];
  await page.setInputFiles('#import-file', {
    name: 'favorites.json', mimeType: 'application/json',
    buffer: Buffer.from(JSON.stringify({ version: 1, items })),
  });

  await expect(page.locator('#fav-msg')).toContainText('Imported 1, skipped 3');
  await expect(page.locator('#tab-saved')).toHaveText('Saved (1)');
});

test('import rejects a file that is not JSON', async ({ page }) => {
  await openApp(page);
  await page.click('#tab-saved');
  await page.setInputFiles('#import-file', {
    name: 'oops.json', mimeType: 'application/json', buffer: Buffer.from('this is not json {'),
  });
  await expect(page.locator('#fav-msg')).toContainText('Not a valid JSON file');
  await expect(page.locator('#tab-saved')).toHaveText('Saved (0)');
});

test('refuses to save more than 200 favorites', async ({ page }) => {
  const filler = Array.from({ length: 200 }, (_, i) => favorite({ question: 'Filler ' + i + '?' }));
  await page.addInitScript(([key, value]) => localStorage.setItem(key, value), [FAV_KEY, JSON.stringify(filler)]);
  await openApp(page);
  await expect(page.locator('#tab-saved')).toHaveText('Saved (200)');

  await generateQuiz(page);
  await star(page, 0).click();
  await expect(page.locator('#fav-msg')).toContainText('full');
  await expect(star(page, 0)).toHaveAttribute('aria-pressed', 'false');
  await expect(page.locator('#tab-saved')).toHaveText('Saved (200)');
});

test('still works when the browser blocks storage', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(window, 'localStorage', { get() { throw new Error('blocked'); } });
  });
  await openApp(page);
  await generateQuiz(page);
  await star(page, 0).click();

  await expect(page.locator('#fav-notice')).toContainText('storage unavailable'); // told, not crashed
  await expect(page.locator('#tab-saved')).toHaveText('Saved (1)'); // kept in memory for this visit
});

test('corrupt saved data is treated as empty and replaced on the next save', async ({ page }) => {
  await page.addInitScript((key) => localStorage.setItem(key, '{not json'), FAV_KEY);
  await openApp(page);
  await expect(page.locator('#tab-saved')).toHaveText('Saved (0)');

  await generateQuiz(page);
  await star(page, 0).click();
  const stored = await page.evaluate((key) => JSON.parse(localStorage.getItem(key)), FAV_KEY);
  expect(stored).toHaveLength(1);
});
