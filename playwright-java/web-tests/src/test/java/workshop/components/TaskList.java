package workshop.components;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;

public final class TaskList {
  private final Locator list;

  public TaskList(Page page) {
    list = page.getByRole(AriaRole.LIST, new Page.GetByRoleOptions().setName("Tasks"));
  }

  public Locator row(String title) {
    return list.getByRole(AriaRole.LISTITEM).filter(new Locator.FilterOptions().setHasText(title));
  }
}
