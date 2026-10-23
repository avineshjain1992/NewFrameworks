import fs from "node:fs";
import path from "node:path";
import dotenv from "dotenv";
const selected = process.env.TEST_ENV || "local";
if (!["local", "dev", "qa", "staging"].includes(selected))
  throw new Error("TEST_ENV must be local, dev, qa or staging");
const read = (file) =>
  fs.existsSync(file) ? dotenv.parse(fs.readFileSync(file)) : {};
const file = { ...read(".env"), ...read(`.env.${selected}`) };
const local = selected === "local";
const get = (key, fallback) => process.env[key] ?? file[key] ?? fallback;
const number = (key, fallback, minimum) => {
  const n = Number(get(key, fallback));
  if (!Number.isInteger(n) || n < minimum)
    throw new Error(`${key} must be an integer >= ${minimum}`);
  return n;
};
const bool = (key, fallback) => {
  const v = get(key, fallback);
  if (!["true", "false"].includes(v))
    throw new Error(`${key} must be true or false`);
  return v === "true";
};
const url = (key) => {
  const value = get(key, local ? "http://127.0.0.1:3100" : "");
  let parsed;
  try {
    parsed = new URL(value);
  } catch {
    throw new Error(`${key} must be explicitly configured for ${selected}`);
  }
  if (
    !["http:", "https:"].includes(parsed.protocol) ||
    parsed.username ||
    parsed.password ||
    parsed.hostname.endsWith(".invalid") ||
    (!local && ["127.0.0.1", "localhost", "[::1]"].includes(parsed.hostname))
  )
    throw new Error(
      `${key} requires a valid target URL without credentials for ${selected}`,
    );
  return value;
};
const authMode = get("AUTH_MODE", "password");
const username = get("AUTH_USERNAME", local ? "demo" : "");
const password = get("AUTH_PASSWORD", local ? "demo-password" : "");
const token = get("AUTH_TOKEN", "");
if (
  !["password", "bearer"].includes(authMode) ||
  (authMode === "password" && (!username || !password)) ||
  (authMode === "bearer" && !token)
)
  throw new Error(
    "Configure AUTH_MODE=password with AUTH_USERNAME/AUTH_PASSWORD, or AUTH_MODE=bearer with AUTH_TOKEN",
  );
const browser = get("BROWSER", "chromium");
if (!["chromium", "firefox", "webkit"].includes(browser))
  throw new Error("BROWSER must be chromium, firefox or webkit");
const logLevel = get("LOG_LEVEL", "info");
if (
  !["trace", "debug", "info", "warn", "error", "fatal", "silent"].includes(
    logLevel,
  )
)
  throw new Error("Invalid LOG_LEVEL");
const selection = process.env.RUN_SELECTION || "all";
export const config = Object.freeze({
  selection,
  environment: selected,
  apiBase: url("API_BASE_URL"),
  webBase: url("WEB_BASE_URL"),
  authMode,
  username,
  password,
  token,
  browser,
  executablePath: get("BROWSER_EXECUTABLE_PATH", ""),
  headless: bool("HEADLESS", "true"),
  timeout: number("TIMEOUT_MS", 10000, 1),
  workers: number("WORKERS", 2, 1),
  retries: number("RETRIES", 0, 0),
  logLevel,
  reportDir: path.resolve(get("REPORT_DIR", "reports")),
  artifactDir: path.resolve(get("ARTIFACT_DIR", "artifacts")),
  video: bool("VIDEO", "false"),
});
