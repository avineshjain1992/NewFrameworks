import http from "node:http";
import { randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
const tasks = new Map();
const tokens = new Set();
const html = readFileSync(new URL("./index.html", import.meta.url));
function reply(res, status, value, correlation) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "x-request-id": correlation,
    "cache-control": "no-store",
  });
  res.end(JSON.stringify(value));
}
const server = http.createServer(async (req, res) => {
  const path = new URL(req.url, "http://localhost").pathname;
  const id = req.headers["x-request-id"] || randomUUID();
  const send = (status, value) => reply(res, status, value, id);
  if (path === "/health") return send(200, { status: "ok" });
  if (path === "/" && req.method === "GET") {
    res.writeHead(200, { "content-type": "text/html; charset=utf-8" });
    return res.end(html);
  }
  if (!path.startsWith("/api/")) return send(404, { error: "Not found" });
  let body = {};
  try {
    let raw = "";
    for await (const chunk of req) {
      raw += chunk;
      if (raw.length > 16384) return send(413, { error: "Payload too large" });
    }
    if (raw) body = JSON.parse(raw);
  } catch {
    return send(400, { error: "Invalid JSON" });
  }
  if (path === "/api/login" && req.method === "POST") {
    if (body.username !== "demo" || body.password !== "demo-password")
      return send(401, { error: "Invalid credentials" });
    const token = randomUUID();
    tokens.add(token);
    if (tokens.size > 10000) tokens.delete(tokens.values().next().value);
    return send(200, { token });
  }
  if (!tokens.has((req.headers.authorization || "").replace(/^Bearer /, "")))
    return send(401, { error: "Unauthorized" });
  if (path === "/api/tasks" && req.method === "GET")
    return send(200, {
      tasks: [
        { id: "welcome", title: "Welcome task", completed: false },
        ...tasks.values(),
      ],
    });
  const match = path.match(/^\/api\/tasks\/([^/]+)$/);
  if (
    (path === "/api/tasks" && req.method === "POST") ||
    (match && req.method === "PATCH")
  ) {
    if (match && !tasks.has(match[1]))
      return send(404, { error: "Task not found" });
    if (
      typeof body.title !== "string" ||
      !body.title.trim() ||
      body.title.length > 120 ||
      (body.completed !== undefined && typeof body.completed !== "boolean")
    )
      return send(422, {
        error:
          "Title must contain 1–120 characters and completed must be boolean",
      });
    const task = {
      id: match ? match[1] : randomUUID(),
      title: body.title.trim(),
      completed: body.completed ?? false,
    };
    tasks.set(task.id, task);
    return send(match ? 200 : 201, task);
  }
  if (match) {
    const task = tasks.get(match[1]);
    if (!task) return send(404, { error: "Task not found" });
    if (req.method === "GET") return send(200, task);
    if (req.method === "DELETE") {
      tasks.delete(match[1]);
      return send(200, { deleted: true });
    }
  }
  return send(404, { error: "Not found" });
});
server.listen(Number(process.env.PORT || 3100), "127.0.0.1", () =>
  console.log(JSON.stringify({ event: "ready", port: server.address().port })),
);
for (const signal of ["SIGINT", "SIGTERM"])
  process.on(signal, () => server.close(() => process.exit(0)));
