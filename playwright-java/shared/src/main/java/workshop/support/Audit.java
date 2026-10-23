package workshop.support;

import io.qameta.allure.Allure;
import org.slf4j.LoggerFactory;

public final class Audit {
  private Audit() {}

  public static void http(String method, int status, String correlation) {
    // Allowlist metadata; never log request/response bodies, URLs, cookies or headers.
    String metadata =
        "method="
            + method
            + " route=/api/tasks/:id status="
            + status
            + " correlation="
            + correlation;
    LoggerFactory.getLogger(Audit.class).info(metadata);
    try {
      Allure.attachment("sanitized-http", metadata);
    } catch (RuntimeException e) {
      LoggerFactory.getLogger(Audit.class).warn("Report attachment unavailable");
    }
  }
}
