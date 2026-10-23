# Rest Assured + TestNG

An independently runnable Maven API project. `src/test/java/workshop/api` holds the focused HTTP service and eight scenario methods; `support` holds configuration, payload/response records, TestNG contracts, retries, listener and allowlisted logging. `src/test/resources` contains schema, Logback configuration and explicit suite XML. No browser runtime is used.

## Prerequisites and setup

Use a **JDK 21** (not a JRE), Node.js 24 LTS for the shared fixture, and the included Maven Wrapper (Maven 3.9.16 with pinned distribution checksum). Sources target Java 17. Set JAVA_HOME to the JDK. Maven dependencies require network on initial installation, then run offline with `-o` after the required goals/plugins have been cached.

From this project directory, Linux/macOS: `./mvnw test-compile`; Windows PowerShell: `.\mvnw.cmd test-compile`. Format with `./mvnw spotless:apply`; check with `./mvnw spotless:check` (use `mvnw.cmd` on Windows).

## TestNG and parallel lifecycle

Surefire explicitly reads `src/test/resources/suites/api.xml` or `web.xml`. Tests use TestNG `smoke`, `regression`, `positive`, `negative`, `crud` groups; regression contains all scenarios. Invalid titles use a parallel `@DataProvider`. `RunListener` configures method/data-provider concurrency from WORKERS. Per-invocation ThreadLocal resources are initialized and disposed on the invoking thread with `alwaysRun=true` teardown. Independent tests have no priorities or dependencies.

`-Dgroups=smoke` selects smoke; similarly `regression`, `positive`, `negative`, `crud`, or a comma-separated union. `-DWORKERS=4` controls parallelism. `-DRETRIES=1` opts into bounded TestNG retry analysis; Allure retains attempts and skipped/retried entries remain visible. Default is zero. `-Dtest=TasksApiTest#creationPersists` selects one method and bypasses suite XML through Surefire; Allure is also service-loaded. Explicit supported system properties override process/file settings.

Reports: module `target/surefire-reports` contains TestNG/Surefire XML and HTML; `target/allure-results` contains Allure data; `target/reports/logs/tests.log` contains timestamped SLF4J/Logback output. Java web artifacts live in `web-tests/target/artifacts/<UUID>/`. Each module forks independently so outputs do not collide. A single concurrent invocation per module/output root is supported; use separate checkouts/output properties for independent simultaneous Maven invocations. An `@AfterMethod` captures screenshots/traces before browser teardown, so listener ordering cannot close the page prematurely. Playwright auto-retrying assertions verify UI state; API assertions use TestNG. No JUnit dependency is used as runner or assertion library.

## Execution

From the **repository root** (fixture is managed automatically):

```sh
node scripts/run.mjs ra-api
node scripts/run.mjs ra-api -Dgroups=smoke
node scripts/run.mjs ra-api -Dgroups=regression
node scripts/run.mjs ra-api -Dgroups=negative
node scripts/run.mjs ra-api -Dgroups=crud -DWORKERS=4
node scripts/run.mjs ra-api -Dtest=TasksApiTest#creationPersists
```

From this directory against a configured running target:

```sh
./mvnw test
./mvnw test -Dsuite=src/test/resources/suites/api.xml -Dgroups=smoke
./mvnw test -DTEST_ENV=qa
```

Windows: `.\mvnw.cmd` replaces `./mvnw`. There are no web, headed, or browser commands because this project is API-only. Each request builds a local Rest Assured specification with explicit timeouts and bearer headers; it never mutates RestAssured static defaults. JSON is serialized explicitly rather than relying on classpath-dependent mapper discovery. Negative responses remain available for TestNG contract assertions.

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

Rest Assured 6 requires Apache HttpClient 4's legacy AbstractHttpClient implementation. A per-test client disables transport retries explicitly and closes its connection manager after owned-resource cleanup. Typed Duration timeouts avoid deprecated parameter-based timeout configuration.

Register new Java test classes explicitly in the relevant TestNG suite XML and assign the intended groups. Linux headed execution requires a display server; CI uses headless mode.
