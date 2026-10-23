import { TaskList } from "../components/task-list.js";
export class TasksPage {
  constructor(page) {
    this.page = page;
    this.tasks = new TaskList(page);
  }
  async create(title) {
    await this.page.getByLabel("Task title", { exact: true }).fill(title);
    await this.page
      .getByRole("button", { name: "Add task", exact: true })
      .click();
  }
}
