package workshop.support;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public record ApiResponse(int status, Map<String, String> headers, String body) {
  public JsonNode json() {
    return Json.read(body);
  }
}
