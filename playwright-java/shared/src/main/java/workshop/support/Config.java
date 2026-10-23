package workshop.support;

import java.net.URI;
import java.nio.file.*;
import java.util.*;

public final class Config {
  private final Map<String, String> file = new HashMap<>();
  public final String environment,
      apiBase,
      webBase,
      authMode,
      username,
      password,
      token,
      browser,
      reportDir,
      artifactDir,
      logLevel;
  public final int timeout, workers, retries;
  public final boolean headless, video;

  public Config() {
    environment = System.getProperty("TEST_ENV", System.getenv().getOrDefault("TEST_ENV", "local"));
    if (!List.of("local", "dev", "qa", "staging").contains(environment))
      throw new IllegalArgumentException("TEST_ENV must be local, dev, qa or staging");
    Path root = Path.of(System.getProperty("user.dir"));
    if (!Files.exists(root.resolve(".env.example"))
        && root.getParent() != null
        && Files.exists(root.getParent().resolve(".env.example"))) root = root.getParent();
    load(root.resolve(".env"));
    load(root.resolve(".env." + environment));
    boolean local = environment.equals("local");
    apiBase = url("API_BASE_URL", local);
    webBase = url("WEB_BASE_URL", local);
    authMode = get("AUTH_MODE", "password");
    username = get("AUTH_USERNAME", local ? "demo" : "");
    password = get("AUTH_PASSWORD", local ? "demo-password" : "");
    token = get("AUTH_TOKEN", "");
    if (!List.of("password", "bearer").contains(authMode)
        || (authMode.equals("password") && (username.isBlank() || password.isBlank()))
        || (authMode.equals("bearer") && token.isBlank()))
      throw new IllegalArgumentException(
          "Set AUTH_MODE=password with AUTH_USERNAME/AUTH_PASSWORD or AUTH_MODE=bearer with AUTH_TOKEN");
    browser = get("BROWSER", "chromium");
    if (!List.of("chromium", "firefox", "webkit").contains(browser))
      throw new IllegalArgumentException("BROWSER must be chromium, firefox or webkit");
    headless = bool("HEADLESS", "true");
    video = bool("VIDEO", "false");
    timeout = number("TIMEOUT_MS", "10000", 1);
    workers = number("WORKERS", "2", 1);
    retries = number("RETRIES", "0", 0);
    reportDir = get("REPORT_DIR", "target/reports");
    artifactDir = get("ARTIFACT_DIR", "target/artifacts");
    logLevel = get("LOG_LEVEL", "info");
    if (!List.of("trace", "debug", "info", "warn", "error", "off").contains(logLevel.toLowerCase()))
      throw new IllegalArgumentException("Invalid LOG_LEVEL");
    System.setProperty("LOG_LEVEL", logLevel);
    System.setProperty("REPORT_DIR", reportDir);
  }

  private void load(Path path) {
    if (!Files.exists(path)) return;
    try {
      for (String line : Files.readAllLines(path)) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        int pos = line.indexOf('=');
        if (pos < 1) throw new IllegalArgumentException("Invalid env entry in " + path);
        String value = line.substring(pos + 1).trim();
        if (value.length() >= 2
            && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))))
          value = value.substring(1, value.length() - 1);
        file.put(line.substring(0, pos).trim(), value);
      }
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot read " + path, e);
    }
  }

  public String get(String key, String fallback) {
    return System.getProperty(
        key, System.getenv().getOrDefault(key, file.getOrDefault(key, fallback)));
  }

  private int number(String key, String fallback, int min) {
    try {
      int n = Integer.parseInt(get(key, fallback));
      if (n < min) throw new NumberFormatException();
      return n;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " must be an integer >= " + min);
    }
  }

  private boolean bool(String key, String fallback) {
    String v = get(key, fallback);
    if (!List.of("true", "false").contains(v))
      throw new IllegalArgumentException(key + " must be true or false");
    return Boolean.parseBoolean(v);
  }

  private String url(String key, boolean local) {
    String v = get(key, local ? "http://127.0.0.1:3100" : "");
    try {
      URI u = URI.create(v);
      if (!List.of("http", "https").contains(u.getScheme())
          || u.getHost() == null
          || u.getUserInfo() != null
          || u.getHost().endsWith(".invalid")
          || (!local && List.of("localhost", "127.0.0.1", "[::1]").contains(u.getHost())))
        throw new IllegalArgumentException();
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException(
          key
              + " must be explicitly set to a valid target URL without credentials for "
              + environment);
    }
    return v;
  }
}
