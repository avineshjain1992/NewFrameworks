import { randomUUID } from "node:crypto";
export const taskPayload = (overrides = {}) => ({
  title: `task-${randomUUID()}`,
  completed: false,
  ...overrides,
});
