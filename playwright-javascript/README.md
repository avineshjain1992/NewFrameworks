# Playwright Test with JavaScript

Native ES modules, JavaScript only. `tests/api` and `tests/web` are independent Playwright projects. `src/api` owns API lifecycle, `src/pages`/`components` describe UI interactions, `src/data` builds unique payloads, `src/assertions` validates HTTP/schema contracts, `src/config.js` loads environment files, and `tests/fixtures.js` owns cleanup. API fixtures use `playwright.request.newContext()` and never request the browser fixture.

## Setup

Node.js 24 LTS and npm. From this directory:

```sh
npm ci
npx playwright install chromium
npm run lint
npm run format:check
```

API-only use needs `npm ci` but no browser installation. Linux web CI uses `npx playwright install --with-deps chromium`. Firefox and WebKit are supported by `BROWSER` after installing their binaries. `npm run format` applies Prettier.

## Execution

From the **repository root**, with automatic fixture management:

```sh
node scripts/run.mjs js-all
node scripts/run.mjs js-api
node scripts/run.mjs js-web
node scripts/run.mjs js-api --grep @smoke
node scripts/run.mjs js-web --grep @regression
node scripts/run.mjs js-api --grep @negative
node scripts/run.mjs js-api --grep @crud --workers 4
node scripts/run.mjs js-web --grep "login succeeds" --headed
node scripts/run.mjs js-web --workers 2
```

From this directory, against an already running configured target:

```sh
npm test
npm run test:api
npm run test:web
npm run test:smoke
npm run test:regression
npm run test:api -- --grep @positive
npm run test:web -- --grep "login succeeds"
```

Select environment with `TEST_ENV=qa` in bash or `$env:TEST_ENV='qa'` in PowerShell, then run the desired command. Set `BROWSER=firefox` or `webkit` for browser selection. HEADLESS=true is the default; CLI `--headed`, `--workers`, `--retries` override the corresponding config. Retry count defaults to zero; Playwright reporters expose each attempt. Data-driven validation has three independent title values. Eight API scenarios produce ten test invocations; web has five.

## Reporting

Reports use `reports/<api|web|all>/`: HTML under `html/`, CI XML in `junit.xml`, Allure data in `allure-results/`, and structured timestamped Pino logs in `logs/worker-<pid>.jsonl`. Browser artifacts use `artifacts/<selection>/playwright/` and per-test worker-safe names. Screenshot and trace are retained on failure; video is opt-in. Separate suite output roots prevent API/web runs from overwriting each other; combined runs include both projects in the all root. Use a distinct REPORT_DIR/ARTIFACT_DIR for simultaneous independent runs of the same selection.

Render native HTML with `npx playwright show-report reports/all/html`. For Allure see root instructions. The JUnit-format XML reporter does not add JUnit to the Java projects. Configuration and schema files are packaged with this project; it needs no dependency on sibling frameworks to test a configured target.

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
