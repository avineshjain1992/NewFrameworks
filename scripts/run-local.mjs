import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
const root = fileURLToPath(new URL("../", import.meta.url));
const args = process.argv.slice(2);
if (!args.length)
  throw new Error("Usage: node scripts/run-local.mjs <command> [args...]");
const port = process.env.PORT || "3100";
const origin = `http://127.0.0.1:${port}`;
let fixture;
let command;
let interrupted = false;
async function stop(child) {
  if (!child || child.exitCode !== null || child.signalCode) return;
  const exited = new Promise((resolve) => child.once("exit", resolve));
  const signal = (value) => {
    if (process.platform === "win32") {
      spawn(
        "taskkill",
        [
          "/PID",
          String(child.pid),
          "/T",
          ...(value === "SIGKILL" ? ["/F"] : []),
        ],
        { stdio: "ignore" },
      );
    } else {
      try {
        process.kill(-child.pid, value);
      } catch (error) {
        if (error.code !== "ESRCH") throw error;
      }
    }
  };
  signal("SIGTERM");
  const timer = setTimeout(() => signal("SIGKILL"), 4000);
  await exited;
  clearTimeout(timer);
}
for (const signal of ["SIGINT", "SIGTERM"])
  process.once(signal, () => {
    interrupted = true;
    void stop(command);
    if (!command) void stop(fixture);
  });

try {
  try {
    const response = await fetch(`${origin}/health`);
    if (response)
      throw new Error(
        `Port ${port} is occupied. Select another PORT; existing processes are never stopped.`,
      );
  } catch (e) {
    if (!e.cause) throw e;
  }
  if (interrupted) throw new Error("Interrupted before fixture startup");
  fixture = spawn(process.execPath, ["demo/server.js"], {
    cwd: root,
    env: process.env,
    detached: process.platform !== "win32",
    stdio: "inherit",
  });
  let ready = false;
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    if (interrupted) throw new Error("Interrupted during fixture startup");
    if (fixture.exitCode !== null)
      throw new Error("Fixture exited before readiness");
    try {
      const response = await fetch(`${origin}/health`);
      ready = response.ok && (await response.json()).status === "ok";
    } catch {
      /* polling during startup */
    }
    if (ready) break;
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  if (interrupted) throw new Error("Interrupted before test startup");
  if (!ready) throw new Error("Fixture readiness timed out");
  const executable =
    process.platform === "win32" && ["npm", "npx"].includes(args[0])
      ? `${args[0]}.cmd`
      : args[0];
  command = spawn(executable, args.slice(1), {
    cwd: root,
    detached: process.platform !== "win32",
    stdio: "inherit",
    shell: process.platform === "win32" && executable.endsWith(".cmd"),
    env: {
      ...process.env,
      API_BASE_URL: process.env.API_BASE_URL || origin,
      WEB_BASE_URL: process.env.WEB_BASE_URL || origin,
    },
  });
  process.exitCode = await new Promise((resolve, reject) => {
    command.once("error", reject);
    command.once("exit", (code) => resolve(code ?? 1));
  });
} finally {
  await stop(command);
  await stop(fixture);
}
