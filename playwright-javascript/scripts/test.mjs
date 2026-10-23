import fs from "node:fs";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
const [scope = "all", ...extra] = process.argv.slice(2);
const selected =
  extra.find((arg) => arg.startsWith("--project="))?.split("=")[1] ||
  (extra.includes("--project") ? extra[extra.indexOf("--project") + 1] : scope);
if (!["all", "api", "web"].includes(selected))
  throw new Error("Select api, web or all");
const args = [
  "test",
  ...(scope === "all" ? [] : [`--project=${scope}`]),
  ...extra,
];
process.env.RUN_SELECTION = selected;
const { config } = await import("../src/config.js");
fs.rmSync(`${config.reportDir}/${selected}/allure-results`, {
  recursive: true,
  force: true,
});
const child = spawn(
  process.execPath,
  [fileURLToPath(import.meta.resolve("@playwright/test/cli")), ...args],
  { env: { ...process.env, RUN_SELECTION: selected }, stdio: "inherit" },
);
for (const signal of ["SIGINT", "SIGTERM"])
  process.once(signal, () => child.kill(signal));
child.once("error", (error) => {
  console.error(error.message);
  process.exitCode = 1;
});
child.once("exit", (code) => {
  process.exitCode = code ?? 1;
});
