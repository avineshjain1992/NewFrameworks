export class LoginPage {
  constructor(page) {
    this.page = page;
  }
  async open() {
    await this.page.goto("/");
  }
  async login(username, password) {
    await this.page.getByLabel("Username", { exact: true }).fill(username);
    await this.page.getByLabel("Password", { exact: true }).fill(password);
    await this.page
      .getByRole("button", { name: "Log in", exact: true })
      .click();
  }
}
