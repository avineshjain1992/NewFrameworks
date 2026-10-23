export class TaskList {
  constructor(page) {
    this.list = page.getByRole("list", { name: "Tasks" });
  }
  row(title) {
    return this.list.getByRole("listitem").filter({ hasText: title });
  }
}
