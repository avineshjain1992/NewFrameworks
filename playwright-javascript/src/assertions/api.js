import { expect } from "@playwright/test";
import Ajv from "ajv";
import { readFileSync } from "node:fs";
const schema = JSON.parse(
  readFileSync(new URL("../../schemas/task.schema.json", import.meta.url)),
);
const validate = new Ajv({ allErrors: true }).compile(schema);
export function contract(response, status) {
  expect(response.status()).toBe(status);
  expect(response.headers()["content-type"]).toContain("application/json");
  expect(response.headers()["x-request-id"]).toMatch(/^[a-f0-9-]{36}$/);
}
export function taskSchema(body) {
  expect(validate(body), JSON.stringify(validate.errors)).toBe(true);
}
