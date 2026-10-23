package workshop.api;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import java.util.*;
import workshop.support.*;

public final class TasksClient implements AutoCloseable {
  private final Config config;
  private final Playwright playwright;
  private final APIRequestContext context;
  private String token;
  private final Set<String> owned = new LinkedHashSet<>();

  public TasksClient(Config config) {
    this.config = config;
    playwright =
        Playwright.create(
            new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
    context =
        playwright
            .request()
            .newContext(
                new APIRequest.NewContextOptions()
                    .setBaseURL(config.apiBase)
                    .setTimeout(config.timeout));
    try {
      if (config.authMode.equals("bearer")) token = config.token;
      else {
        ApiResponse r =
            raw(
                "POST",
                "/api/login",
                Map.of("username", config.username, "password", config.password),
                false,
                UUID.randomUUID().toString());
        if (r.status() != 200)
          throw new IllegalStateException("Authentication failed: HTTP " + r.status());
        token = r.json().path("token").asText();
      }
    } catch (RuntimeException e) {
      context.dispose();
      playwright.close();
      throw e;
    }
  }

  private ApiResponse raw(
      String method, String path, Object payload, boolean authenticated, String correlation) {
    RequestOptions options =
        RequestOptions.create().setMaxRetries(0).setHeader("x-request-id", correlation);
    if (authenticated) options.setHeader("Authorization", "Bearer " + token);
    if (payload != null)
      options.setHeader("Content-Type", "application/json").setData(Json.write(payload));
    APIResponse r = context.fetch(path, options.setMethod(method));
    try {
      return new ApiResponse(r.status(), r.headers(), r.text());
    } finally {
      r.dispose();
    }
  }

  public ApiResponse send(String method, String path, Object payload, boolean authenticated) {
    String correlation = UUID.randomUUID().toString();
    ApiResponse r = raw(method, path, payload, authenticated, correlation);
    if (method.equals("POST") && r.status() == 201) owned.add(r.json().path("id").asText());
    Audit.http(method, r.status(), correlation);
    return r;
  }

  public ApiResponse send(String method, String path, Object payload) {
    return send(method, path, payload, true);
  }

  public void own(String id) {
    owned.add(id);
  }

  public void close() {
    RuntimeException failure = null;
    try {
      for (String id : owned) {
        try {
          ApiResponse r = send("DELETE", "/api/tasks/" + id, null);
          if (r.status() != 200 && r.status() != 404)
            throw new IllegalStateException("Cleanup HTTP " + r.status());
        } catch (RuntimeException e) {
          if (failure == null) failure = e;
        }
      }
    } finally {
      context.dispose();
      playwright.close();
    }
    if (failure != null) throw failure;
  }
}
