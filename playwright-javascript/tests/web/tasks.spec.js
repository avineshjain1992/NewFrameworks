import { test, expect } from "../fixtures.js";
import { config } from "../../src/config.js";
import { LoginPage } from "../../src/pages/login-page.js";
import { TasksPage } from "../../src/pages/tasks-page.js";
import { taskPayload } from "../../src/data/task.js";
test.beforeEach(async ({ page }, info) => {
  const diagnostics = [];
  // Deliberately omit console text and exception messages: applications can log secrets.
  page.on("console", (message) => {
    if (message.type() === "error")
      diagnostics.push({ event: "console-error" });
  });
  page.on("pageerror", () => diagnostics.push({ event: "page-error" }));
  info._diagnostics = diagnostics;
  await new LoginPage(page).open();
});
test.afterEach(async ({ page }, info) => {
  await page.title();
  await info.attach("browser-diagnostics", {
    body: JSON.stringify(info._diagnostics),
    contentType: "application/json",
  });
});
async function login(page) {
  await new LoginPage(page).login(config.username, config.password);
}
test("login succeeds @smoke @regression @positive", async ({ page }) => {
  await login(page);
  await expect(page.getByRole("heading", { name: "Your tasks" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Log in", exact: true }),
  ).toBeHidden();
});
test("invalid login @regression @negative", async ({ page }) => {
  await new LoginPage(page).login("invalid", "invalid");
  await expect(page.getByRole("alert")).toHaveText("Invalid credentials");
  await expect(page.getByRole("heading", { name: "Your tasks" })).toBeHidden();
});
test("list contains seed @smoke @regression @positive", async ({ page }) => {
  await login(page);
  const tasks = new TasksPage(page);
  await expect(tasks.tasks.row("Welcome task")).toHaveText("Welcome task");
});
test("create through UI @regression @positive @crud", async ({
  page,
  client,
}) => {
  await login(page);
  const title = taskPayload().title;
  const tasks = new TasksPage(page);
  // Register ownership from the real POST response before the UI assertion can fail.
  const created = page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      new URL(r.url()).pathname === "/api/tasks",
  );
  try {
    await tasks.create(title);
    const response = await created;
    expect(response.status()).toBe(201);
    client.owned.add((await response.json()).id);
    await expect(tasks.tasks.row(title)).toContainText(title);
  } finally {
    const response = await client.send("GET", "/api/tasks");
    for (const task of (await response.json()).tasks)
      if (task.title === title) client.owned.add(task.id);
  }
});
test("required title validation @regression @negative", async ({ page }) => {
  await login(page);
  await expect(new TasksPage(page).tasks.row("Welcome task")).toBeVisible();
  const submissions = [];
  page.on("request", (request) => {
    if (
      request.method() === "POST" &&
      new URL(request.url()).pathname === "/api/tasks"
    )
      submissions.push(request);
  });
  const field = page.getByLabel("Task title", { exact: true });
  await page.getByRole("button", { name: "Add task", exact: true }).click();
  expect(await field.evaluate((input) => input.validity.valueMissing)).toBe(
    true,
  );
  await expect(page.getByRole("heading", { name: "Your tasks" })).toBeVisible();
});
