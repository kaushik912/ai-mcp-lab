// Shared helpers so each test reads like a short user story.
const { expect } = require('@playwright/test');

const CODE_SNIPPET = 'String a = "hi";\nString b = new String("hi");\nSystem.out.println(a == b);';

// A canned quiz of 10 questions. Question i has options alpha<i>, beta<i>, gamma<i>, delta<i>
// and the right answer is always the 2nd one (index 1).
function fakeQuiz(topic = 'Java', overrides = {}) {
  return {
    topic,
    questions: Array.from({ length: 10 }, (_, i) => ({
      subtopic: 'sub' + i,
      kind: i % 2 ? 'SCENARIO' : 'CONCEPT',
      scenario: i % 2 ? 'Scenario ' + i : null,
      question: 'Question ' + i + '?',
      options: ['alpha' + i, 'beta' + i, 'gamma' + i, 'delta' + i],
      correctIndex: 1,
      explanation: 'because ' + i,
      ...(overrides[i] || {}),
    })),
  };
}

// Make POST /api/quiz answer with a canned quiz: no model, no key, no cost.
// Returns the list of request bodies the page sent, so a test can inspect them.
async function fakeQuizApi(page, makeQuiz = fakeQuiz) {
  const sent = [];
  await page.route('**/api/quiz', async (route) => {
    const body = route.request().postDataJSON();
    sent.push(body);
    await route.fulfill({ json: makeQuiz(body.topic) });
  });
  return sent;
}

// Open the app and wait until the topic dropdown has loaded (6 topics + "Other").
async function openApp(page) {
  await page.goto('/');
  await expect(page.locator('#topic-select option')).toHaveCount(7);
}

// Fill the key, click Generate, wait for the 10 question cards.
async function generateQuiz(page, key = 'test-key') {
  await page.fill('#key', key);
  await page.click('#generate');
  await expect(page.locator('.q')).toHaveCount(10);
}

// Tap the star on question number `n` (0-based) of the current quiz.
function star(page, n) {
  return page.locator('button.star').nth(n);
}

// A valid favorite, as stored in localStorage / exported files.
function favorite(overrides = {}) {
  return {
    topic: 'Databases', difficulty: 'HARD', kind: 'CONCEPT', scenario: null,
    question: 'What is an index?', options: ['a', 'b', 'c', 'd'], correctIndex: 2,
    explanation: 'because', savedAt: 1700000000000,
    ...overrides,
  };
}

const FAV_KEY = 'quiz.favorites.v1';

module.exports = { CODE_SNIPPET, fakeQuiz, fakeQuizApi, openApp, generateQuiz, star, favorite, FAV_KEY };
