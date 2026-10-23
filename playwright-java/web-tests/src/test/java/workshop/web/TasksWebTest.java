package workshop.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.testng.*;
import org.testng.annotations.*;
import workshop.pages.*;
import workshop.support.*;

public final class TasksWebTest {
  private final ThreadLocal<BrowserSession> sessions = new ThreadLocal<>();

  @BeforeMethod(alwaysRun = true)
  public void setup() {
    sessions.set(new BrowserSession());
  }

  @AfterMethod(alwaysRun = true)
  public void teardown(ITestResult result) {
    BrowserSession session = sessions.get();
    try {
      if (session != null) {
        session.capture(result);
        try {
          session.close();
        } catch (RuntimeException e) {
          if (result.isSuccess()) throw e;
          org.slf4j.LoggerFactory.getLogger(getClass()).warn("Cleanup failed after test failure");
        }
      }
    } finally {
      sessions.remove();
    }
  }

  private BrowserSession s() {
    return sessions.get();
  }

  private void login() {
    new LoginPage(s().page).login(s().config.username, s().config.password);
  }

  @Test(
      groups = {"smoke", "regression", "positive"},
      retryAnalyzer = Retry.class)
  public void loginSucceeds() {
    login();
    assertThat(
            s().page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Your tasks")))
        .isVisible();
    assertThat(
            s().page
                .getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in").setExact(true)))
        .isHidden();
  }

  @Test(
      groups = {"regression", "negative"},
      retryAnalyzer = Retry.class)
  public void invalidLogin() {
    new LoginPage(s().page).login("invalid", "invalid");
    assertThat(s().page.getByRole(AriaRole.ALERT)).hasText("Invalid credentials");
    assertThat(
            s().page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Your tasks")))
        .isHidden();
  }

  @Test(
      groups = {"smoke", "regression", "positive"},
      retryAnalyzer = Retry.class)
  public void listLoads() {
    login();
    assertThat(new TasksPage(s().page).tasks.row("Welcome task")).hasText("Welcome task");
  }

  @Test(
      groups = {"regression", "positive", "crud"},
      retryAnalyzer = Retry.class)
  public void createTask() {
    login();
    String title = TaskPayload.unique().title();
    try {
      Response response =
          s().page
              .waitForResponse(
                  r -> r.request().method().equals("POST") && r.url().endsWith("/api/tasks"),
                  () -> new TasksPage(s().page).create(title));
      Assert.assertEquals(response.status(), 201);
      s().client.own(Json.read(response.text()).path("id").asText());
      assertThat(new TasksPage(s().page).tasks.row(title)).containsText(title);
    } finally {
      ApiResponse r = s().client.send("GET", "/api/tasks", null);
      for (var task : r.json().path("tasks"))
        if (task.path("title").asText().equals(title)) s().client.own(task.path("id").asText());
    }
  }

  @Test(
      groups = {"regression", "negative"},
      retryAnalyzer = Retry.class)
  public void requiredTitle() {
    login();
    assertThat(new TasksPage(s().page).tasks.row("Welcome task")).isVisible();
    java.util.List<String> submissions = new java.util.ArrayList<>();
    s().page
        .onRequest(
            request -> {
              if (request.method().equals("POST") && request.url().endsWith("/api/tasks"))
                submissions.add(request.url());
            });
    s().page
        .getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add task").setExact(true))
        .click();
    Locator field = s().page.getByLabel("Task title", new Page.GetByLabelOptions().setExact(true));
    Assert.assertEquals(field.evaluate("input => input.validity.valueMissing"), true);
    assertThat(field).isEmpty();
    Assert.assertEquals(field.evaluate("input => input.validationMessage.length > 0"), true);
    Assert.assertTrue(submissions.isEmpty(), "Invalid form must not send a create request");
    assertThat(
            s().page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Your tasks")))
        .isVisible();
  }
}
