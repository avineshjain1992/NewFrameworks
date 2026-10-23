package workshop.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class LoginPage {
  private final Page page;

  public LoginPage(Page page) {
    this.page = page;
  }

  public void login(String username, String password) {
    page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true)).fill(username);
    page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(password);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in").setExact(true))
        .click();
  }
}
