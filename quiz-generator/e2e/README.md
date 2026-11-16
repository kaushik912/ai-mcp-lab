# Browser tests (Playwright)

Real Chromium opens the real app and acts like a user. Run from `quiz-generator/`:

```bash
make e2e-install   # once: npm packages + Chromium
make e2e           # run all tests (starts the app on :8099 itself, then stops it)
make e2e-headed    # same, but you watch the browser
make e2e-report    # open the HTML report of the last run
```

## How to read a test
A test is a short story: open the page, do what a user does, check what they see.

```js
test('a starred question is saved and survives a reload', async ({ page }) => {
  await openApp(page);                 // open the page
  await generateQuiz(page);            // fill key, click Generate, wait for 10 questions
  await star(page, 0).click();         // act: tap the star on question 1
  await expect(page.locator('#tab-saved')).toHaveText('Saved (1)');   // check: what the user sees
  ...
```

Only four ideas:
| Idea | Meaning | Example |
|------|---------|---------|
| `page` | one browser tab | `page.goto('/')` |
| locator | how to find something on screen | `page.locator('#generate')` |
| action | what the user does | `.click()`, `.fill('text')`, `.check()` |
| `expect(...)` | what must be true; **waits automatically** until it is | `await expect(x).toHaveText('...')` |

## Things worth knowing
- **The AI model is faked.** Tests intercept `POST /api/quiz` and answer with a canned quiz (`fakeQuiz` in `tests/helpers.js`): no key, no network, no cost. Only `api.spec.js` talks to the real server.
- **Every test starts clean:** a fresh browser, so saved favorites (localStorage) start empty.
- **Port 8099, not 8080.** If 8099 is busy the run fails loudly instead of testing the wrong app. Use another port with `E2E_PORT=8100 make e2e`.
- **Phone-size screen** (390x844), because the app is used on a phone.

## Files
- `tests/topics.spec.js`: topic dropdown, hints, "Other", fallback
- `tests/quiz.spec.js`: generate, grade, errors, code snippets, text-not-HTML, key not stored
- `tests/favorites.spec.js`: star, Saved tab, export/import, 200 cap, blocked/corrupt storage
- `tests/api.spec.js`: server-side validation (401/400)
- `tests/helpers.js`: shared fake quiz and small helpers
- `playwright.config.js`: starts the app, sets port and screen size
