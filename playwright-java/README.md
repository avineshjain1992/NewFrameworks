# Playwright Java + TestNG

Three Maven modules: `shared` contains configuration, payload/response records, contracts, logging and TestNG support; `api-tests` contains the Playwright API service/tests; `web-tests` contains an independent browser lifecycle, page/components, tests and its cleanup API service. The small cleanup client is local to each suite so API and browser test modules never depend on one another. No browser is launched or downloaded by API tests.

```text
shared/src/main/java/workshop/support/  configuration, JSON, schema and report helpers
api-tests/src/test/java/workshop/api/  API service and eight scenario methods
api-tests/src/test/resources/          schemas and explicit TestNG API suite
web-tests/src/test/java/workshop/      api cleanup, pages, components, browser lifecycle, five tests
web-tests/src/test/resources/suites/   explicit TestNG web suite
.mvn/wrapper/                         reproducible Maven distribution
```

## Prerequisites and setup

Use a **JDK 21** (not a JRE), Node.js 24 LTS for the shared fixture, and the included Maven Wrapper (Maven 3.9.16 with pinned distribution checksum). Sources target Java 17. Set JAVA_HOME to the JDK. Maven dependencies require network on initial installation, then run offline with `-o` after the required goals/plugins have been cached.

From this project directory, Linux/macOS: `./mvnw test-compile`; Windows PowerShell: `.\mvnw.cmd test-compile`. Format with `./mvnw spotless:apply`; check with `./mvnw spotless:check` (use `mvnw.cmd` on Windows).

## TestNG and parallel lifecycle

Surefire explicitly reads `src/test/resources/suites/api.xml` or `web.xml`. Tests use TestNG `smoke`, `regression`, `positive`, `negative`, `crud` groups; regression contains all scenarios. Invalid titles use a parallel `@DataProvider`. `RunListener` configures method/data-provider concurrency from WORKERS. Per-invocation ThreadLocal resources are initialized and disposed on the invoking thread with `alwaysRun=true` teardown. Independent tests have no priorities or dependencies.

`-Dgroups=smoke` selects smoke; similarly `regression`, `positive`, `negative`, `crud`, or a comma-separated union. `-DWORKERS=4` controls parallelism. `-DRETRIES=1` opts into bounded TestNG retry analysis; Allure retains attempts and skipped/retried entries remain visible. Default is zero. `-Dtest=TasksApiTest#creationPersists` selects one method and bypasses suite XML through Surefire; Allure is also service-loaded. Explicit supported system properties override process/file settings.

Reports: module `target/surefire-reports` contains TestNG/Surefire XML and HTML; `target/allure-results` contains Allure data; `target/reports/logs/tests.log` contains timestamped SLF4J/Logback output. Java web artifacts live in `web-tests/target/artifacts/<UUID>/`. Each module forks independently so outputs do not collide. A single concurrent invocation per module/output root is supported; use separate checkouts/output properties for independent simultaneous Maven invocations. An `@AfterMethod` captures screenshots/traces before browser teardown, so listener ordering cannot close the page prematurely. Playwright auto-retrying assertions verify UI state; API assertions use TestNG. No JUnit dependency is used as runner or assertion library.

## Execution

From the **repository root**, these portable commands start, readiness-check, test and stop the fixture:

```sh
node scripts/run.mjs pw-all
node scripts/run.mjs pw-api
node scripts/run.mjs pw-web
node scripts/run.mjs pw-api -Dgroups=smoke
node scripts/run.mjs pw-web -Dgroups=regression
node scripts/run.mjs pw-api -Dtest=TasksApiTest#creationPersists
node scripts/run.mjs pw-web -Dtest=TasksWebTest#loginSucceeds
node scripts/run.mjs pw-web -DBROWSER=firefox -DHEADLESS=false -DWORKERS=2
```

Before web runs, install browser binaries from this project directory:

```sh
./mvnw -pl web-tests -am test-compile
./mvnw -pl web-tests org.codehaus.mojo:exec-maven-plugin:3.6.4:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="install chromium"
```

The second command needs the shared module installed first when invoked alone: use `./mvnw install -DskipTests` once. For Linux CI the browser install command uses `install --with-deps chromium`. Replace chromium with firefox/webkit or install all three.

Against an already-running configured target, run from this directory:

```sh
./mvnw test
./mvnw -pl api-tests -am test
./mvnw -pl web-tests -am test
./mvnw -pl api-tests -am test -Dsuite=src/test/resources/suites/api.xml -Dgroups=smoke
./mvnw -pl web-tests -am test -Dsuite=src/test/resources/suites/web.xml -Dgroups=negative
./mvnw -pl api-tests -am test -DTEST_ENV=qa
```

Use `.\mvnw.cmd` on Windows. API request contexts set `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` explicitly; API-only execution requires the platform driver, but no browser binaries. Browser tests create a fresh Playwright instance, browser and context per method to obey threading constraints. Configuration is read before resources are created.

## Environment and authentication

Copy `.env.local.example` to `.env.local` (`cp` in bash, `Copy-Item` in PowerShell). The local defaults work without copying a file. Examples exist for `local`, `dev`, `qa`, and `staging`; actual `.env*` files are ignored. Files use `KEY=value` syntax with optional matching quotes and full-line `#` comments. No shell expansion is performed.

Precedence: supported runner CLI / Java `-DKEY=value` > process environment > `.env.<TEST_ENV>` > `.env` > defaults. Select `TEST_ENV` through the process environment or Java property **before** loading files. Java reads files in the framework root, including when tests execute inside its Maven modules; JavaScript reads them from its project working directory. CLI flags override only settings supported by that runner.

| Setting                       | Default / purpose                                                                  |
| ----------------------------- | ---------------------------------------------------------------------------------- |
| TEST_ENV                      | local; dev, qa, staging require explicit URLs and authentication                   |
| API_BASE_URL / WEB_BASE_URL   | http://127.0.0.1:3100 locally; both validated even in API-only mode                |
| AUTH_MODE                     | password (POST /api/login); bearer uses AUTH_TOKEN for API calls                   |
| AUTH_USERNAME / AUTH_PASSWORD | synthetic demo / demo-password locally                                             |
| AUTH_TOKEN                    | externally supplied bearer token; never commit real values                         |
| BROWSER / HEADLESS            | chromium / true; also firefox, webkit                                              |
| BROWSER_EXECUTABLE_PATH       | optional explicit system-browser fallback; normally unset                          |
| TIMEOUT_MS                    | 10000; positive integer                                                            |
| WORKERS / RETRIES             | 2 / 0; retries are whole test attempts, never automatic business-operation retries |
| LOG_LEVEL                     | info; no request bodies or credentials are logged                                  |
| REPORT_DIR / ARTIFACT_DIR     | project-specific reports and artifact roots                                        |
| VIDEO                         | false; true enables browser video capture                                          |

Nonlocal configuration rejects placeholder `.invalid` hosts and loopback URLs. There is no silent nonlocal-to-local fallback. In bash use `TEST_ENV=qa command`; in PowerShell use `$env:TEST_ENV='qa'; command`. Java alternatively supports `-DTEST_ENV=qa`. Nonlocal examples deliberately require editing before use.

To target a company API, configure URLs and authentication, then adapt the focused login/task service, models, schemas, page objects and business assertions to its actual contract. Changing a URL cannot make demo-specific scenarios valid for an unrelated application. Bearer mode applies to API authentication; browser login uses the configured username/password and the application's login page. SSO flows require an application-specific authentication fixture. Keep secrets in secure process environment/configuration, never command-line arguments in shared CI logs.

## Isolation, logging, and artifacts

Each scenario owns unique UUID data and deletes only its own created resources, including on assertion failure. Update/delete scenarios create their own preconditions. No global reset or ordered tests. Cleanup errors fail an otherwise successful test and are logged separately after an existing failure. API utilities do not retry POST, PATCH or DELETE. Retrying a test creates fresh data.

HTTP logging and Allure attachments use allowlisted method, route template, status and correlation ID metadata. Bodies, arbitrary URL parameters, headers and tokens are omitted. Console/page diagnostics record event types without untrusted text. Failure screenshots/videos can contain visible application data: use synthetic accounts and controlled test data. Traces are sanitized before attachment; network bodies and headers are removed. Do not enable raw HTTP debug logging against sensitive targets.

Allure results are data files, **not** an HTML report. Install an Allure CLI separately, then render the result directory. See root README for the pinned rendering command. Java Surefire XML and JavaScript JUnit-format XML are CI outputs; Java tests use TestNG assertions/runners only.

## Extending and troubleshooting

Add API scenarios beside `TasksApiTest` / `tasks.spec.js`. Give each independent setup, unique payloads, explicit business assertions and owned cleanup. Put payload builders in data/support and schema contracts in resources/schemas (Java) or schemas (JS). Extend the service only for real shared domain behavior; keep assertions visible in tests.

Add focused page/component objects under pages/components, and scenarios in the web suite. Use role/label/test-ID locators and Playwright assertions; avoid sleeps. Start and close Playwright on the same test worker thread in Java. Never share its objects between threads.

If the fixture port is occupied, select a different PORT (and do not stop an unknown process). The runner sets local URLs to that port unless already provided. A 401 during setup usually means target/auth mismatch. A browser-executable error requires installing the selected browser binary or an explicit compatible system executable. Missing shared Maven artifacts: use `-am` with `-pl`. Zero matching Java tests fail via Surefire; zero matching Playwright tests fail by default. Browser dependencies are unnecessary for API-only runs.

GitHub Actions runs five separately named API/web jobs, caches npm/Maven dependencies, checks formatting/lint, and always uploads reports. Every job uses the local fixture wrapper, which preserves the test exit code and shuts down only its own processes.

Managed Java runner commands use `clean test` so reports belong to the current run. For direct Maven execution, use `clean test` when you need fresh Allure results; plain `test` retains prior result files. The JavaScript npm runner clears its selected Allure result directory before running. Archive reports before another run of the same suite if needed.

Register new Java test classes explicitly in the relevant TestNG suite XML and assign the intended groups. Linux headed execution requires a display server; CI uses headless mode.
