package workshop.support;

import java.util.UUID;

public record TaskPayload(String title, boolean completed) {
  public static TaskPayload unique() {
    return new TaskPayload("task-" + UUID.randomUUID(), false);
  }
}
