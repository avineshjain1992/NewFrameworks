import { randomUUID } from "node:crypto";
import { config } from "../config.js";
import { logger } from "../logger.js";
export class TasksClient {
  constructor(context, token, record) {
    this.context = context;
    this.token = token;
    this.record = record;
    this.owned = new Set();
  }
  static async connect(playwright, record) {
    const context = await playwright.request.newContext({
      baseURL: config.apiBase,
      timeout: config.timeout,
      failOnStatusCode: false,
    });
    try {
      let token = config.token;
      if (config.authMode === "password") {
        const response = await context.post("/api/login", {
          data: { username: config.username, password: config.password },
        });
        if (response.status() !== 200)
          throw new Error(`Authentication failed: HTTP ${response.status()}`);
        token = (await response.json()).token;
      }
      return new TasksClient(context, token, record);
    } catch (error) {
      await context.dispose();
      throw error;
    }
  }
  async send(method, path, data, authenticated = true) {
    const correlation = randomUUID();
    const response = await this.context.fetch(path, {
      method,
      maxRetries: 0,
      data,
      headers: {
        "x-request-id": correlation,
        ...(authenticated ? { authorization: `Bearer ${this.token}` } : {}),
      },
    });
    const metadata = {
      method,
      route: "/api/tasks/:id",
      status: response.status(),
      correlation,
    };
    logger.info(metadata, "HTTP completed");
    this.record.push(metadata);
    if (method === "POST" && response.status() === 201)
      this.owned.add((await response.json()).id);
    return response;
  }
  async dispose() {
    let failure;
    try {
      for (const id of this.owned) {
        try {
          const r = await this.send("DELETE", `/api/tasks/${id}`);
          if (![200, 404].includes(r.status()))
            throw new Error(`Cleanup HTTP ${r.status()}`);
        } catch (e) {
          failure ??= e;
        }
      }
    } finally {
      await this.context.dispose();
    }
    if (failure) throw failure;
  }
}
