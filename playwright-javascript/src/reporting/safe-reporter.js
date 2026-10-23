import fs from "node:fs";
import { unzipSync, zipSync, strFromU8, strToU8 } from "fflate";
import { config } from "../config.js";
const blocked =
  /^(.*headers|result|value|localStorage|sessionStorage|origins|postData|cookies|storageState|httpCredentials|password|token|authorization|params|snapshot|sha1|_sha1)$/i;
function clean(value) {
  if (typeof value === "string") {
    for (const secret of [
      config.username,
      config.password,
      config.token,
    ].filter(Boolean))
      value = value.replaceAll(secret, "[REDACTED]");
    return value.replace(/Bearer\s+[^\s"\\]+/gi, "Bearer [REDACTED]");
  }
  if (Array.isArray(value)) return value.map(clean);
  if (value && typeof value === "object")
    return Object.fromEntries(
      Object.entries(value).map(([key, val]) => [
        key,
        blocked.test(key)
          ? Array.isArray(val)
            ? []
            : val && typeof val === "object"
              ? {}
              : "[REDACTED]"
          : clean(val),
      ]),
    );
  return value;
}
export function sanitizeTrace(file) {
  const entries = unzipSync(fs.readFileSync(file));
  for (const [name, bytes] of Object.entries(entries)) {
    if (
      name.startsWith("resources/") &&
      !name.endsWith(".jpeg") &&
      !name.endsWith(".png")
    ) {
      delete entries[name];
      continue;
    }
    if (name.endsWith(".network")) {
      entries[name] = new Uint8Array();
      continue;
    }
    if (
      name.endsWith(".trace") ||
      name.endsWith(".network") ||
      name.endsWith(".stacks")
    ) {
      entries[name] = strToU8(
        strFromU8(bytes)
          .split("\n")
          .filter(Boolean)
          .flatMap((line) => {
            const event = JSON.parse(line);
            if (["frame-snapshot", "console", "event"].includes(event.type))
              return [];
            return [JSON.stringify(clean(event))];
          })
          .join("\n") + "\n",
      );
    }
  }
  const temporary = `${file}.sanitized`;
  fs.writeFileSync(temporary, zipSync(entries));
  fs.renameSync(temporary, file);
}
export default class SafeReporter {
  onTestEnd(test, result) {
    for (const error of result.errors) {
      if (error.message) error.message = clean(error.message);
      if (error.stack) error.stack = clean(error.stack);
      if (error.snippet) error.snippet = clean(error.snippet);
    }
    for (const attachment of result.attachments) {
      if (attachment.name === "trace" && attachment.path) {
        try {
          sanitizeTrace(attachment.path);
        } catch {
          // Fail closed: never publish an unsanitized archive.
          fs.rmSync(attachment.path, { force: true });
          attachment.path = undefined;
          attachment.body = Buffer.from("Trace withheld: sanitization failed");
          attachment.contentType = "text/plain";
        }
      }
    }
  }
}
