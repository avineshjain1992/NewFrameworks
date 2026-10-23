package workshop.support;

import org.slf4j.LoggerFactory;
import org.testng.*;

public final class RunListener implements ITestListener, IAlterSuiteListener {
  public void alter(java.util.List<org.testng.xml.XmlSuite> suites) {
    for (var suite : suites) {
      suite.setThreadCount(new Config().workers);
      suite.setDataProviderThreadCount(new Config().workers);
    }
  }

  public void onTestStart(ITestResult result) {
    var analyzer = result.getMethod().getRetryAnalyzer(result);
    result.setAttribute(
        "retryAttempt", analyzer instanceof Retry retry ? retry.attempt(result) : 0);
    io.qameta.allure.Allure.label("environment", new Config().environment);
  }

  public void onTestSuccess(ITestResult r) {
    log(r, "passed");
  }

  public void onTestFailure(ITestResult r) {
    log(r, "failed");
  }

  public void onTestSkipped(ITestResult r) {
    log(r, "skipped-or-retried");
  }

  private void log(ITestResult r, String status) {
    LoggerFactory.getLogger(RunListener.class)
        .info(
            "test={} status={} durationMs={} retry={}",
            r.getMethod().getMethodName(),
            status,
            r.getEndMillis() - r.getStartMillis(),
            r.getAttribute("retryAttempt"));
  }
}
