package workshop.api;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;
import workshop.support.*;

public final class TasksApiTest {
  private final ThreadLocal<TasksClient> clients = new ThreadLocal<>();

  @BeforeMethod(alwaysRun = true)
  public void setup() {
    clients.set(new TasksClient(new Config()));
  }

  @AfterMethod(alwaysRun = true)
  public void cleanup(ITestResult result) {
    TasksClient client = clients.get();
    try {
      if (client != null) client.close();
    } catch (RuntimeException e) {
      if (result.isSuccess()) throw e;
      org.slf4j.LoggerFactory.getLogger(getClass()).warn("Cleanup failed after test failure");
    } finally {
      clients.remove();
    }
  }

  private ApiResponse call(String method, String path, Object payload) {
    return clients.get().send(method, path, payload);
  }

  private String create() {
    ApiResponse r = call("POST", "/api/tasks", TaskPayload.unique());
    Contracts.response(r, 201);
    return r.json().path("id").asText();
  }

  @Test(
      groups = {"smoke", "regression", "positive"},
      retryAnalyzer = Retry.class)
  public void authenticatedGet() {
    ApiResponse r = call("GET", "/api/tasks", null);
    Contracts.response(r, 200);
    boolean found = false;
    for (var task : r.json().path("tasks"))
      if (task.path("id").asText().equals("welcome")) {
        Assert.assertEquals(task.path("title").asText(), "Welcome task");
        Assert.assertFalse(task.path("completed").asBoolean());
        found = true;
      }
    Assert.assertTrue(found, "Seed task present");
  }

  @Test(
      groups = {"smoke", "regression", "positive", "crud"},
      retryAnalyzer = Retry.class)
  public void creationPersists() {
    TaskPayload payload = TaskPayload.unique();
    ApiResponse r = call("POST", "/api/tasks", payload);
    Contracts.response(r, 201);
    Assert.assertEquals(r.json().path("title").asText(), payload.title());
    ApiResponse stored = call("GET", "/api/tasks/" + r.json().path("id").asText(), null);
    Contracts.response(stored, 200);
    Assert.assertEquals(stored.json(), r.json());
  }

  @Test(
      groups = {"regression", "positive", "crud"},
      retryAnalyzer = Retry.class)
  public void updatePersists() {
    String id = create();
    TaskPayload payload = new TaskPayload(TaskPayload.unique().title(), true);
    ApiResponse r = call("PATCH", "/api/tasks/" + id, payload);
    Contracts.response(r, 200);
    Assert.assertEquals(r.json().path("title").asText(), payload.title());
    Assert.assertTrue(r.json().path("completed").asBoolean());
    ApiResponse stored = call("GET", "/api/tasks/" + id, null);
    Contracts.response(stored, 200);
    Assert.assertEquals(stored.json(), r.json());
  }

  @Test(
      groups = {"regression", "positive", "crud"},
      retryAnalyzer = Retry.class)
  public void deletionPersists() {
    String id = create();
    ApiResponse r = call("DELETE", "/api/tasks/" + id, null);
    Contracts.response(r, 200);
    Assert.assertTrue(r.json().path("deleted").asBoolean());
    ApiResponse missing = call("GET", "/api/tasks/" + id, null);
    Contracts.response(missing, 404);
    Assert.assertEquals(missing.json().path("error").asText(), "Task not found");
  }

  @DataProvider(name = "invalidTitles", parallel = true)
  public Object[][] invalidTitles() {
    return new Object[][] {{""}, {"   "}, {"x".repeat(121)}};
  }

  @Test(
      dataProvider = "invalidTitles",
      groups = {"regression", "negative"},
      retryAnalyzer = Retry.class)
  public void validation(String title) {
    ApiResponse r = call("POST", "/api/tasks", new TaskPayload(title, false));
    Contracts.response(r, 422);
    Assert.assertEquals(
        r.json().path("error").asText(),
        "Title must contain 1–120 characters and completed must be boolean");
  }

  @Test(
      groups = {"smoke", "regression", "negative"},
      retryAnalyzer = Retry.class)
  public void unauthorized() {
    ApiResponse r = clients.get().send("GET", "/api/tasks", null, false);
    Contracts.response(r, 401);
    Assert.assertEquals(r.json().path("error").asText(), "Unauthorized");
  }

  @Test(
      groups = {"regression", "negative"},
      retryAnalyzer = Retry.class)
  public void missingResource() {
    ApiResponse r = call("GET", "/api/tasks/missing", null);
    Contracts.response(r, 404);
    Assert.assertEquals(r.json().path("error").asText(), "Task not found");
  }

  @Test(
      groups = {"regression", "positive"},
      retryAnalyzer = Retry.class)
  public void schema() {
    String id = create();
    ApiResponse r = call("GET", "/api/tasks/" + id, null);
    Contracts.response(r, 200);
    Contracts.schema(r);
  }
}
