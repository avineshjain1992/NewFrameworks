# Task automation workshop

Three independent automation projects share a deterministic Node HTTP/UI fixture. Java uses TestNG throughout; JavaScript uses Playwright Test. There are **34 distinct scenario definitions**, producing **40 invocations** with validation data: 10 API + 5 web in each Playwright framework, and 10 Rest Assured API tests.

```text
playwright-java/        Maven shared, api-tests and web-tests modules
playwright-javascript/ JavaScript Playwright Test API/web projects
restassured-java/       Maven API tests
 demo/                 zero-dependency task HTTP API and real browser UI
 schemas/              canonical demo task contract (project copies are independently runnable)
 scripts/              portable owned-process fixture/test runners
 .github/workflows/    five separately identified API/web CI jobs
 docs/                 dependency decisions and verification evidence
```

## Quick start (Linux/macOS and Windows PowerShell)

Install JDK 21, Node.js 24 LTS and npm. Java projects include Maven Wrapper, so global Maven is optional. Use an existing checkout; cloud tasks are already isolated and do not need a Git worktree.

```sh
npm --prefix playwright-javascript ci
```

Linux/macOS, install/compile Java dependencies and browsers:

```sh
./playwright-java/mvnw -f playwright-java/pom.xml install -DskipTests
./restassured-java/mvnw -f restassured-java/pom.xml test-compile
npm --prefix playwright-javascript exec playwright install chromium
./playwright-java/mvnw -f playwright-java/web-tests/pom.xml org.codehaus.mojo:exec-maven-plugin:3.6.4:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="install chromium"
```

PowerShell substitutes `.\playwright-java\mvnw.cmd` and `.\restassured-java\mvnw.cmd` for those wrapper executables; all other arguments and Node commands are identical. Wrapper distribution checksums are pinned. On Linux, append `--with-deps` to Playwright install arguments if system browser libraries are missing (requires package-manager permissions). Install each engine you intend to select.

Run from the repository root on every platform:

```sh
node scripts/run.mjs pw-api
node scripts/run.mjs pw-web
node scripts/run.mjs pw-all
node scripts/run.mjs js-api
node scripts/run.mjs js-web
node scripts/run.mjs js-all
node scripts/run.mjs ra-api
```

These scripts create a fixture, wait for `/health` to return `{status:"ok"}`, propagate the actual test exit code, and stop only their own child processes in `finally`. They refuse to reuse or kill a service already occupying PORT (default 3100). Separate concurrent invocations should use different PORT values. To develop the fixture itself: `node demo/server.js`; then use project-local commands against that already running target. Its in-memory state is discarded on restart. Synthetic demo login is `demo` / `demo-password`.

Once npm/Maven dependencies, wrapper distribution, Playwright drivers and browser binaries are cached, tests use only local HTTP and do not need internet. Java can use `-o` to enforce Maven offline mode. API-only commands need no browser binary. See [verification](docs/VERIFICATION.md) for actual runs and environment-specific limitations.

## Checks, reports, and extension

```sh
npm --prefix playwright-javascript run lint
npm --prefix playwright-javascript run format:check
./playwright-java/mvnw -f playwright-java/pom.xml spotless:check
./restassured-java/mvnw -f restassured-java/pom.xml spotless:check
```

Each framework README covers group/tag filtering, single tests, environments, parallelism, architecture, test additions, artifact paths and troubleshooting:

- [Playwright Java](playwright-java/README.md)
- [Playwright JavaScript](playwright-javascript/README.md)
- [Rest Assured Java](restassured-java/README.md)

Allure adapters generate results, not a rendered report. With Node 24, render using the pinned Allure 3 CLI (`npx --yes allure@3.22.1 generate <results-directory> --output <html-directory>`); CLI version and actual invocation are recorded in verification. Java also provides Surefire XML/HTML, JS provides native Playwright HTML and JUnit-format XML. Report generation does not replace test execution. Generated dependencies, reports, logs, videos and traces are ignored by Git.

## Choosing and adapting a framework

Playwright API testing is convenient when API setup/cleanup and browser workflows share a toolchain. Rest Assured offers a Java-first HTTP testing ecosystem without a browser driver. Java favors TestNG groups, XML suite control, typed models and Maven modules; JavaScript favors Playwright's native worker fixtures, tags, auto-retrying UI assertions and integrated reporting.

To replace the demo, configure nonlocal URLs/authentication explicitly, adapt domain API clients and page objects, replace schemas/payloads and seed-specific assertions, and provision isolated test users/data. URL replacement alone cannot adapt business contracts. Never point destructive CRUD examples at production data. Authentication helpers log only safe status metadata. Use secure environment variables for real secrets and synthetic data in screenshots/videos.

GitHub Actions runs Playwright Java API/web, JavaScript API/web, and Rest Assured API as separate jobs with caches, checks, owned fixture lifecycle, failure-preserving exit codes and always-uploaded diagnostics. See [dependencies](docs/DEPENDENCIES.md) for version sources.

Managed Java runner commands use `clean test` so reports belong to the current run. For direct Maven execution, use `clean test` when you need fresh Allure results; plain `test` retains prior result files. The JavaScript npm runner clears its selected Allure result directory before running. Archive reports before another run of the same suite if needed.

## This cloud instance

The system initially contained only a Java runtime and an unwritable home cache. The tested Linux-specific bootstrap handles a verified JDK, writable caches, Maven proxy/system trust and the available system Chromium:

```sh
bash scripts/setup-cloud.sh
source .local/cloud-env.sh
node scripts/run.mjs pw-all -o
node scripts/run.mjs js-all
node scripts/run.mjs ra-api -o
```

Source `.local/cloud-env.sh` in each new shell. This is cloud-specific initialization; the general Windows/macOS setup above uses your installed JDK. Standard matching browser binaries and FFmpeg still need the saved network allowlist changes applied. See the exact outstanding commands in [verification](docs/VERIFICATION.md). No real secrets are saved in the bootstrap or examples.
