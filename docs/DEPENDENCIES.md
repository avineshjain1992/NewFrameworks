# Dependency selection

Versions were checked against package registries and official source documentation during implementation. Public documentation websites for Playwright, TestNG and Maven were blocked in this cloud, so the corresponding official GitHub source docs were used:

- https://raw.githubusercontent.com/microsoft/playwright/main/docs/src/intro-java.md
- https://raw.githubusercontent.com/microsoft/playwright/main/docs/src/intro-js.md
- https://raw.githubusercontent.com/testng-team/testng/master/README.md
- https://raw.githubusercontent.com/rest-assured/rest-assured/master/README.md
- https://raw.githubusercontent.com/apache/maven-wrapper/master/README.md
- https://raw.githubusercontent.com/allure-framework/allure-java/main/README.md

Java: Playwright 1.64.0, TestNG 7.12.0, Rest Assured and its JSON schema module 6.3.0, Allure TestNG 3.0.0 (Java 17+ and `Allure.attachment` API), Jackson 2.22.4, SLF4J 2.0.20 (stable 2.0 line; avoid 2.1 alpha), Logback 1.6.5. Build: Maven 3.9.16, Wrapper 3.3.4, Compiler 3.15.0, Surefire 3.5.6, Spotless 3.3.0 and Google Java Format 1.35.0. Dependencies are pinned in project POMs, with reproducible wrapper distribution SHA-256.

JS: Playwright Test 1.64.1, Allure Playwright 3.14.0, Pino 10.4.0, dotenv 18.0.9, Ajv 8.20.0, ESLint 10.13.0, @eslint/js 10.0.1 and Prettier 3.9.10. Node 24 LTS satisfies their engine ranges. npm package-lock.json pins the entire resolved npm graph. Java/JS Playwright versions are independently pinned to their published stable releases and can have different browser revisions.

Registry sources: `https://registry.npmjs.org/<package>/latest` and `https://repo.maven.apache.org/maven2/<group>/<artifact>/maven-metadata.xml`. Browser installations must retain Playwright's TLS/artifact verification. Maven and JDK bootstraps in this cloud were verified against official distribution checksums. No insecure TLS flags or modified checksums are used.

Surefire is intentionally pinned to the stable 3.5.6 line: 3.6.0 removed native TestNG suite XML support and routes TestNG through JUnit Platform. Version 3.5.6 preserves the required native TestNG provider and XML suite behavior.

Additional pinned tools: Allure CLI 3.22.1 (offline rendering verified), fflate 0.8.3 for trace sanitization, Maven Clean 3.5.0 (stable; 4.x releases are beta), Exec 3.6.4 for Playwright Java CLI, and Apache HttpClient 4.5.14 for Rest Assured's required legacy client binding. Its automatic retry handler is explicitly disabled.
