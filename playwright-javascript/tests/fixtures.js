import { test as base, expect } from "@playwright/test";
import { TasksClient } from "../src/api/tasks-client.js";
import { logger } from "../src/logger.js";
export const test = base.extend({
  observability: [
    async ({ playwright }, use, info) => {
      if (!playwright.request)
        throw new Error("Playwright request runtime unavailable");
      await use();
      logger.info(
        {
          test: info.title,
          suite: info.project.name,
          retry: info.retry,
          status: info.status,
          duration: info.duration,
        },
        "Test completed",
      );
    },
    { auto: true },
  ],
  client: async ({ playwright }, use, info) => {
    const calls = [];
    const client = await TasksClient.connect(playwright, calls);
    let original;
    let cleanup;
    try {
      await use(client);
    } catch (error) {
      original = error;
    }
    try {
      await client.dispose();
    } catch (error) {
      cleanup = error;
      logger.error({ event: "cleanup-failure" });
      await info.attach("cleanup-failure", {
        body: "Resource cleanup failed; inspect fixture availability.",
        contentType: "text/plain",
      });
    }
    await info.attach("sanitized-http", {
      body: JSON.stringify(calls),
      contentType: "application/json",
    });
    logger.info(
      {
        test: info.title,
        status: info.status,
        retry: info.retry,
        duration: info.duration,
      },
      "Test finished",
    );
    if (original) throw original;
    if (cleanup && info.status === info.expectedStatus) throw cleanup;
  },
});
export { expect };
