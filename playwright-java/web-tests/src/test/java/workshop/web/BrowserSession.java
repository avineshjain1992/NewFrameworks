package workshop.web;

import com.microsoft.playwright.*;
import io.qameta.allure.Allure;
import java.nio.file.*;
import java.util.*;
import org.testng.ITestResult;
import workshop.api.TasksClient;
import workshop.support.*;

public final class BrowserSession implements AutoCloseable {
  public final Config config;
  public Playwright playwright;
  public Browser browser;
  public BrowserContext context;
  public Page page;
  public TasksClient client;
  public final List<String> diagnostics = new ArrayList<>();
  public final Path artifacts;

  public BrowserSession() {
    config = new Config();
    artifacts = Path.of(config.artifactDir, UUID.randomUUID().toString());
    try {
      Files.createDirectories(artifacts);
      client = new TasksClient(config);
      playwright = Playwright.create();
      BrowserType type =
          switch (config.browser) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
          };
      browser =
          type.launch(
              new BrowserType.LaunchOptions()
                  .setHeadless(config.headless)
                  .setExecutablePath(
                      config.get("BROWSER_EXECUTABLE_PATH", "").isBlank()
                          ? null
                          : Path.of(config.get("BROWSER_EXECUTABLE_PATH", ""))));
      Browser.NewContextOptions options =
          new Browser.NewContextOptions().setBaseURL(config.webBase);
      if (config.video) options.setRecordVideoDir(artifacts.resolve("video"));
      context = browser.newContext(options);
      context.setDefaultTimeout(config.timeout);
      context
          .tracing()
          .start(
              new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(false));
      page = context.newPage();
      page.onConsoleMessage(
          m -> {
            if (m.type().equals("error")) diagnostics.add("console-error");
          });
      page.onPageError(e -> diagnostics.add("page-error"));
      page.navigate("/");
    } catch (Exception e) {
      try {
        close();
      } catch (RuntimeException cleanup) {
        e.addSuppressed(cleanup);
      }
      throw new IllegalStateException("Browser session startup failed", e);
    }
  }

  public void capture(ITestResult result) {
    boolean failed = !result.isSuccess();
    try {
      if (failed) {
        page.screenshot(new Page.ScreenshotOptions().setPath(artifacts.resolve("failure.png")));
        try (var in = Files.newInputStream(artifacts.resolve("failure.png"))) {
          Allure.attachment(
              "failure screenshot",
              "image/png",
              in,
              new io.qameta.allure.AttachmentOptions().setFileExtension(".png"));
        }
      }
      context
          .tracing()
          .stop(
              failed
                  ? new Tracing.StopOptions().setPath(artifacts.resolve("trace.zip"))
                  : new Tracing.StopOptions());
      if (failed) {
        TraceRedactor.sanitize(artifacts.resolve("trace.zip"), config);
        try (var in = Files.newInputStream(artifacts.resolve("trace.zip"))) {
          Allure.attachment(
              "trace",
              "application/zip",
              in,
              new io.qameta.allure.AttachmentOptions().setFileExtension(".zip"));
        }
      }
      Files.write(artifacts.resolve("diagnostics.txt"), diagnostics);
      Allure.attachment("browser diagnostics", String.join("\n", diagnostics));
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(getClass()).warn("Failure artifact capture failed");
    }
  }

  public void close() {
    RuntimeException failure = null;
    for (Runnable cleanup :
        List.<Runnable>of(
            () -> {
              if (context != null) context.close();
            },
            () -> {
              if (browser != null) browser.close();
            },
            () -> {
              if (playwright != null) playwright.close();
            },
            () -> {
              if (client != null) client.close();
            })) {
      try {
        cleanup.run();
      } catch (RuntimeException e) {
        if (failure == null) failure = e;
      }
    }
    if (config.video && page != null && page.video() != null) {
      try (var in = Files.newInputStream(page.video().path())) {
        Allure.attachment(
            "browser video",
            "video/webm",
            in,
            new io.qameta.allure.AttachmentOptions().setFileExtension(".webm"));
      } catch (Exception error) {
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Video attachment unavailable");
      }
    }
    if (failure != null) throw failure;
  }
}
