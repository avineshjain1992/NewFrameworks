import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
const root = fileURLToPath(new URL("../", import.meta.url));
const [target, ...extra] = process.argv.slice(2);
const choices = [
  "pw-api",
  "pw-web",
  "pw-all",
  "ra-api",
  "js-api",
  "js-web",
  "js-all",
];
if (!choices.includes(target))
  throw new Error(`Select one of ${choices.join(", ")}`);
const windows = process.platform === "win32";
let executable;
let args;
let directory;
if (target.startsWith("js-")) {
  directory = `${root}/playwright-javascript`;
  executable = process.execPath;
  args = ["scripts/test.mjs", target.slice(3), ...extra];
} else {
  directory = `${root}/${target.startsWith("ra-") ? "restassured-java" : "playwright-java"}`;
  executable = windows ? "mvnw.cmd" : "./mvnw";
  args = ["-B", "-ntp", "clean", "test"];
  if (target === "pw-api") args.push("-pl", "api-tests", "-am");
  if (target === "pw-web") args.push("-pl", "web-tests", "-am");
  args.push(...extra);
}
const child = spawn(executable, args, {
  cwd: directory,
  env: process.env,
  stdio: "inherit",
  shell: windows && !target.startsWith("js-"),
});
for (const signal of ["SIGTERM", "SIGINT"])
  process.once(signal, () => child.kill(signal));
child.once("error", (error) => {
  console.error(error.message);
  process.exitCode = 1;
});
child.once("exit", (code) => {
  process.exitCode = code ?? 1;
});
