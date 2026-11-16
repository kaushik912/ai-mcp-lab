// Talks to the real server (no browser): request validation that happens before any model call.
const { test, expect } = require('@playwright/test');

test('a request without an API key is rejected with 401', async ({ request }) => {
  const res = await request.post('/api/quiz', { data: { topic: 'Java' } });
  expect(res.status()).toBe(401);
  expect((await res.json()).error).toContain('X-OpenRouter-Key');
});

test('a blank topic is rejected with 400', async ({ request }) => {
  const res = await request.post('/api/quiz', {
    headers: { 'X-OpenRouter-Key': 'test-key' }, data: { topic: '   ', scope: 'anything' },
  });
  expect(res.status()).toBe(400);
});

test('a scope longer than 400 characters is rejected with 400', async ({ request }) => {
  const res = await request.post('/api/quiz', {
    headers: { 'X-OpenRouter-Key': 'test-key' }, data: { topic: 'Java', scope: 'x'.repeat(401) },
  });
  expect(res.status()).toBe(400);
  expect((await res.json()).error).toContain('scope');
});
