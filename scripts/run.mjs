// The local fixture wrapper owns and stops only the fixture and test processes it creates.
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
const root = fileURLToPath(new URL("../", import.meta.url));
const child = spawn(
  process.execPath,
  [
    "scripts/run-local.mjs",
    process.execPath,
    "scripts/framework.mjs",
    ...process.argv.slice(2),
  ],
  { cwd: root, env: process.env, stdio: "inherit" },
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
