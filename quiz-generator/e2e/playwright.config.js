// Playwright settings. The important part is `webServer`: Playwright starts the Spring app
// itself (on a spare port) before the tests and stops it afterwards.
const { defineConfig } = require('@playwright/test');

const PORT = process.env.E2E_PORT || 8099; // not 8080, so it never clashes with the app you run by hand

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: true,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${PORT}`,
    viewport: { width: 390, height: 844 }, // phone size: the app is used on a phone
    trace: 'off',
  },
  webServer: {
    command: `PORT=${PORT} ./mvnw -q spring-boot:run`,
    cwd: '..', // the quiz-generator folder (where mvnw lives)
    url: `http://localhost:${PORT}`,
    reuseExistingServer: false, // if the port is busy, fail loudly instead of testing the wrong app
    timeout: 120_000,
  },
});
