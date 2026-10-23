package workshop.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import workshop.components.TaskList;

public final class TasksPage {
  private final Page page;
  public final TaskList tasks;

  public TasksPage(Page page) {
    this.page = page;
    tasks = new TaskList(page);
  }

  public void create(String title) {
    page.getByLabel("Task title", new Page.GetByLabelOptions().setExact(true)).fill(title);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add task").setExact(true))
        .click();
  }
}
