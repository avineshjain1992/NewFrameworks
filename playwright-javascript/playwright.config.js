import { defineConfig } from "@playwright/test";
import { config as c } from "./src/config.js";
const selection = process.env.RUN_SELECTION || "all";
const reportRoot = `${c.reportDir}/${selection}`;
const artifactRoot = `${c.artifactDir}/${selection}`;
export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  workers: c.workers,
  retries: c.retries,
  timeout: c.timeout * 3,
  expect: { timeout: c.timeout },
  outputDir: `${artifactRoot}/playwright`,
  reporter: [
    ["./src/reporting/safe-reporter.js"],
    ["list"],
    ["html", { outputFolder: `${reportRoot}/html`, open: "never" }],
    ["junit", { outputFile: `${reportRoot}/junit.xml` }],
    [
      "allure-playwright",
      {
        resultsDir: `${reportRoot}/allure-results`,
        detail: false,
        environmentInfo: { environment: c.environment },
      },
    ],
  ],
  use: {
    baseURL: c.webBase,
    browserName: c.browser,
    headless: c.headless,
    launchOptions: c.executablePath ? { executablePath: c.executablePath } : {},
    actionTimeout: c.timeout,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: c.video ? "retain-on-failure" : "off",
  },
  projects: [
    {
      name: "api",
      testDir: "./tests/api",
      use: { trace: "off", video: "off", screenshot: "off" },
    },
    { name: "web", testDir: "./tests/web" },
  ],
});
