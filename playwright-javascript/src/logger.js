import pino from "pino";
import fs from "node:fs";
import { config } from "./config.js";
fs.mkdirSync(`${config.reportDir}/${config.selection}/logs`, {
  recursive: true,
});
// Log only allowlisted metadata. No URLs, bodies, headers or credentials.
export const logger = pino(
  {
    level: config.logLevel,
    timestamp: pino.stdTimeFunctions.isoTime,
    base: { environment: config.environment, pid: process.pid },
    redact: ["password", "token", "authorization", "cookie"],
  },
  pino.multistream([
    { stream: process.stdout },
    {
      stream: pino.destination({
        dest: `${config.reportDir}/${config.selection}/logs/worker-${process.pid}.jsonl`,
        sync: true,
      }),
    },
  ]),
);
