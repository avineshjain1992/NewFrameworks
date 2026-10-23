package workshop.api;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import java.util.*;
import workshop.support.*;

// Rest Assured 6 requires the legacy AbstractHttpClient API. Keep it test-scoped and close it.
@SuppressWarnings("deprecation")
public final class TasksClient implements AutoCloseable {
  private final Config config;
  private final org.apache.http.impl.client.DefaultHttpClient httpClient =
      new org.apache.http.impl.client.DefaultHttpClient();

  private String token;
  private final Set<String> owned = new LinkedHashSet<>();

  public TasksClient(Config config) {
    this.config = config;
    httpClient.setHttpRequestRetryHandler(
        new org.apache.http.impl.client.DefaultHttpRequestRetryHandler(0, false));
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
      httpClient.getConnectionManager().shutdown();
      throw e;
    }
  }

  private ApiResponse raw(
      String method, String path, Object payload, boolean authenticated, String correlation) {
    RequestSpecification request =
        RestAssured.given()
            .config(
                RestAssuredConfig.config()
                    .httpClient(
                        HttpClientConfig.httpClientConfig()
                            .httpClientFactory(() -> httpClient)
                            .reuseHttpClientInstance()
                            .connectTimeout(java.time.Duration.ofMillis(config.timeout))
                            .readTimeout(java.time.Duration.ofMillis(config.timeout))
                            .connectionRequestTimeout(java.time.Duration.ofMillis(config.timeout))))
            .baseUri(config.apiBase)
            .header("x-request-id", correlation);
    if (authenticated) request.header("Authorization", "Bearer " + token);
    if (payload != null) request.contentType("application/json").body(Json.write(payload));
    var r = request.request(method, path);
    java.util.Map<String, String> headers = new java.util.HashMap<>();
    for (var h : r.headers())
      headers.put(h.getName().toLowerCase(java.util.Locale.ROOT), h.getValue());
    return new ApiResponse(r.statusCode(), headers, r.asString());
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
      httpClient.getConnectionManager().shutdown();
    }
    if (failure != null) throw failure;
  }
}
