// The topic picker: 6 umbrella topics (with hints) plus "Other" for free text.
const { test, expect } = require('@playwright/test');
const { fakeQuizApi, openApp, generateQuiz } = require('./helpers');

test('offers 6 topics plus Other', async ({ page }) => {
  await fakeQuizApi(page);
  await openApp(page);
  await expect(page.locator('#topic-select option')).toHaveText([
    'Java', 'Spring and JPA', 'Databases', 'System design', 'Web and security', 'DevOps and cloud',
    'Other (type your own)',
  ]);
  await expect(page.locator('#topic')).toBeHidden(); // custom box only appears for "Other"
});

test('picking a topic sends its label and its hint', async ({ page }) => {
  const sent = await fakeQuizApi(page);
  await openApp(page);
  await page.selectOption('#topic-select', { label: 'Databases' });
  await generateQuiz(page);

  expect(sent[0].topic).toBe('Databases');
  expect(sent[0].scope).toContain('Indexing'); // the hint from topics.json
  expect(sent[0].difficulty).toBe('MEDIUM');
});

test('Other sends the typed topic and no hint', async ({ page }) => {
  const sent = await fakeQuizApi(page);
  await openApp(page);
  await page.selectOption('#topic-select', { label: 'Other (type your own)' });
  await page.fill('#topic', '  Kafka consumer groups ');
  await generateQuiz(page);

  expect(sent[0].topic).toBe('Kafka consumer groups'); // trimmed
  expect(sent[0].scope).toBeUndefined();
});

test('an empty custom topic is blocked before any request', async ({ page }) => {
  const sent = await fakeQuizApi(page);
  await openApp(page);
  await page.selectOption('#topic-select', { label: 'Other (type your own)' });
  await page.fill('#key', 'test-key');
  await page.click('#generate');

  await expect(page.locator('.q')).toHaveCount(0);
  expect(sent).toHaveLength(0);
});

test('falls back to free text if topics.json cannot be loaded', async ({ page }) => {
  const sent = await fakeQuizApi(page);
  await page.route('**/topics.json', (route) => route.fulfill({ status: 500, body: 'nope' }));
  await page.goto('/');

  await expect(page.locator('#topic-select option')).toHaveText(['Other (type your own)']);
  await expect(page.locator('#topic')).toBeVisible();
  await page.fill('#topic', 'Redis');
  await generateQuiz(page);
  expect(sent[0].topic).toBe('Redis');
});
