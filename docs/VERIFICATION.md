# Executed verification

These are current-instance results, not a claim that GitHub Actions, another operating system, or a published cloud snapshot has been validated.

## Full suites

All **34 distinct scenarios / 40 data-expanded invocations** passed against the real local fixture, with **zero failures and zero skips**. Tests exercised real HTTP and browser interactions. The final runs used four workers and retries disabled:

| Command from repository root after cloud initialization |         Passed |
| ------------------------------------------------------- | -------------: |
| `node scripts/run.mjs pw-all -o -DWORKERS=4`            | API 10 + web 5 |
| `node scripts/run.mjs js-all --workers 4`               | API 10 + web 5 |
| `node scripts/run.mjs ra-api -o -DWORKERS=4`            |         API 10 |

Maven `-o` enforced offline artifact resolution. JavaScript ran with dependencies already installed; an additional run used npm offline mode. No external application or real credentials were used. Full Allure result directories were checked: 10, 5, 15 and 10 passed-result files respectively. Managed Java runs clean build outputs; the JavaScript runner clears its selected Allure data so prior failures do not contaminate a new run.

## Additional executed checks

- Both Java projects compiled under checksum-verified Temurin JDK 21. The original system Java was only a runtime, so Maven compilation required installing a JDK. Downloaded JDK trust uses the system Java CA store, preserving TLS verification.
- Maven Wrapper distribution downloaded successfully with its pinned SHA-256, and managed commands were run through that wrapper.
- `bash scripts/setup-cloud.sh` installed/refreshed dependencies, compiled both projects, warmed offline goals, checked formatting, and wrote reusable shell initialization. The explicit system Chromium fallback was used.
- Java Spotless checks passed in both projects; JavaScript ESLint, repository demo/script ESLint, project Prettier and repository Prettier checks passed.
- Separate Playwright API-only suites passed with an empty browser-cache path and no executable override: Java 10, JavaScript 10. API code creates request contexts, never browser resources, and Java disables browser download during API initialization.
- Separate web suites passed: Java 5, JavaScript 5. The combined full runs passed again with four workers and isolated UUID data.
- Smoke filtering: Java Playwright API 3, web 2; Rest Assured 3; JavaScript combined 5. Single API-test selection passed once in all three frameworks.
- Nonmatching filters returned exit code 1 in all three frameworks; Java explicitly reported zero tests before failing. Nonlocal JavaScript configuration without required URLs failed at startup.
- Cancellation during fixture startup was tested and the startup signal-handling race was fixed; the fixture stopped with a nonzero exit. Occupied-port rejection was tested and left the existing service healthy.
- Controlled assertion failures returned exit code 1 in Java web, JavaScript web, and Rest Assured API. Both web suites emitted screenshots and sanitized trace archives. Rest Assured with RETRIES=1 recorded two attempts (one retried/skipped, one failed), remained nonzero, and preserved the failure. Induced source changes were removed before the final successful full runs.
- Final report/log text was scanned for the synthetic password; no occurrences were found. Failure trace JSON was also checked for the synthetic password; it was absent. Trace exports remove request/response headers, payload resources, DOM snapshots, returned values and configured credential strings. These checks cover the synthetic demo; arbitrary company applications still require controlled data in screenshots/video.
- Allure CLI 3.22.1 rendered HTML offline for Playwright Java API, Rest Assured, and JavaScript results. Raw results are separate from the rendered HTML.

## Report locations

| Project                         | Results and reports                                                                                                                                   |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| Playwright Java                 | `api-tests/target/surefire-reports`, `web-tests/target/surefire-reports`, each module's `target/allure-results`, `target/reports/logs`                |
| Java browser failures           | `web-tests/target/artifacts/<UUID>/failure.png`, `trace.zip`, `diagnostics.txt`                                                                       |
| Rest Assured                    | `target/surefire-reports`, `target/allure-results`, `target/reports/logs`                                                                             |
| JavaScript                      | `reports/<api                                                                                                                                         | web | all>/{html,junit.xml,allure-results,logs}`, `artifacts/<selection>/playwright` |
| Rendered Allure examples        | Java API/Rest Assured `target/allure-html`, JavaScript `reports/all/allure-html`                                                                      |
| Preserved verification evidence | repository `.local/verification/checks.json`, full final command logs, and controlled-failure screenshots/traces under `.local/verification/failures` |

Generated outputs are ignored by Git and exist only in the current prepared filesystem unless exported or snapshotted.

## Remaining limits and precise next steps

Chromium download from `cdn.playwright.dev` was denied with HTTP 403 `Domain forbidden`. Real browser tests passed using installed system Chromium 154 via BROWSER_EXECUTABLE_PATH; the pinned Playwright browser revision has not been installed/validated. Video-enabled attempts failed during browser setup because Playwright FFmpeg revision 1013 was absent. Those were prerequisite failures, not successful video checks. Firefox and WebKit have not been run.

The environment draft now saves custom browser-download domains `cdn.playwright.dev`, `cdn.download.playwright.dev`, `playwright.download.prss.microsoft.com`, and `storage.googleapis.com`, plus install_script and start_skill. Saving that draft does not apply network access or publish a snapshot. Review/save it in environment settings, then publish the environment when appropriate. After network access is applied, initialize the shell, unset BROWSER_EXECUTABLE_PATH, install the matching binaries, then rerun:

```sh
cd /workspace/NewFrameworks
source .local/cloud-env.sh
unset BROWSER_EXECUTABLE_PATH
(cd playwright-javascript && npx playwright install chromium firefox webkit)
./playwright-java/mvnw -f playwright-java/pom.xml install -DskipTests
./playwright-java/mvnw -f playwright-java/web-tests/pom.xml org.codehaus.mojo:exec-maven-plugin:3.6.4:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test '-Dexec.args=install chromium firefox webkit'
VIDEO=true node scripts/run.mjs js-web
node scripts/run.mjs pw-web -DVIDEO=true
BROWSER=firefox node scripts/run.mjs js-web
node scripts/run.mjs pw-web -DBROWSER=firefox
BROWSER=webkit node scripts/run.mjs js-web
node scripts/run.mjs pw-web -DBROWSER=webkit
```

The current cloud bootstrap selects system Chromium when present. Unset its executable override in the shell before testing downloaded engines. These pending commands are documented, **not claimed as passed**. Windows/macOS instructions and GitHub Actions workflows were written but were not executed in this Linux cloud. Fresh-task snapshot restoration and publication are not verified.
