import { test, expect } from "../fixtures.js";
import { taskPayload } from "../../src/data/task.js";
import { contract, taskSchema } from "../../src/assertions/api.js";
test("authenticated list @smoke @regression @positive", async ({ client }) => {
  const r = await client.send("GET", "/api/tasks");
  contract(r, 200);
  expect((await r.json()).tasks).toContainEqual({
    id: "welcome",
    title: "Welcome task",
    completed: false,
  });
});
test("create persists @smoke @regression @positive @crud", async ({
  client,
}) => {
  const data = taskPayload();
  const r = await client.send("POST", "/api/tasks", data);
  contract(r, 201);
  const body = await r.json();
  expect(body).toMatchObject(data);
  const stored = await client.send("GET", `/api/tasks/${body.id}`);
  contract(stored, 200);
  expect(await stored.json()).toEqual(body);
});
test("update persists @regression @positive @crud", async ({ client }) => {
  const created = await client.send("POST", "/api/tasks", taskPayload());
  contract(created, 201);
  const { id } = await created.json();
  const data = taskPayload({ completed: true });
  const r = await client.send("PATCH", `/api/tasks/${id}`, data);
  contract(r, 200);
  expect(await r.json()).toEqual({ id, ...data });
  expect(await (await client.send("GET", `/api/tasks/${id}`)).json()).toEqual({
    id,
    ...data,
  });
});
test("delete removes resource @regression @positive @crud", async ({
  client,
}) => {
  const r = await client.send("POST", "/api/tasks", taskPayload());
  contract(r, 201);
  const { id } = await r.json();
  const removed = await client.send("DELETE", `/api/tasks/${id}`);
  contract(removed, 200);
  expect(await removed.json()).toEqual({ deleted: true });
  const missing = await client.send("GET", `/api/tasks/${id}`);
  contract(missing, 404);
  expect(await missing.json()).toEqual({ error: "Task not found" });
});
for (const title of ["", " ".repeat(3), "x".repeat(121)])
  test(`invalid title length ${title.length} @regression @negative`, async ({
    client,
  }) => {
    const r = await client.send("POST", "/api/tasks", taskPayload({ title }));
    contract(r, 422);
    expect((await r.json()).error).toBe(
      "Title must contain 1–120 characters and completed must be boolean",
    );
  });
test("authentication required @smoke @regression @negative", async ({
  client,
}) => {
  const r = await client.send("GET", "/api/tasks", undefined, false);
  contract(r, 401);
  expect(await r.json()).toEqual({ error: "Unauthorized" });
});
test("unknown resource @regression @negative", async ({ client }) => {
  const r = await client.send("GET", "/api/tasks/missing");
  contract(r, 404);
  expect(await r.json()).toEqual({ error: "Task not found" });
});
test("created task satisfies schema @regression @positive", async ({
  client,
}) => {
  const r = await client.send("POST", "/api/tasks", taskPayload());
  contract(r, 201);
  taskSchema(await r.json());
});
