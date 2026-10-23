package workshop.support;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import org.testng.Assert;

public final class Contracts {
  private Contracts() {}

  public static void response(ApiResponse r, int status) {
    Assert.assertEquals(r.status(), status, "HTTP status");
    Assert.assertTrue(
        r.headers().getOrDefault("content-type", "").contains("application/json"),
        "JSON content type");
    Assert.assertTrue(
        r.headers().getOrDefault("x-request-id", "").matches("[a-f0-9-]{36}"),
        "Correlation header");
  }

  public static void schema(ApiResponse r) {
    Assert.assertTrue(
        matchesJsonSchemaInClasspath("schemas/task.schema.json").matches(r.body()),
        "Task schema validation failed");
  }
}
