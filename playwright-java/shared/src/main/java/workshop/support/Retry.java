package workshop.support;

import java.util.concurrent.ConcurrentHashMap;
import org.testng.*;

public final class Retry implements IRetryAnalyzer {
  private final ConcurrentHashMap<String, Integer> attempts = new ConcurrentHashMap<>();

  public boolean retry(ITestResult result) {
    int max = new Config().retries;
    String key =
        result.getMethod().getQualifiedName()
            + java.util.Arrays.deepToString(result.getParameters());
    int attempt = attempts.merge(key, 1, Integer::sum);
    return attempt <= max;
  }

  public int attempt(ITestResult result) {
    return attempts.getOrDefault(
        result.getMethod().getQualifiedName()
            + java.util.Arrays.deepToString(result.getParameters()),
        0);
  }
}
