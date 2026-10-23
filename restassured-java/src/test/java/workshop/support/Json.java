package workshop.support;

import com.fasterxml.jackson.databind.*;

public final class Json {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Json() {}

  public static String write(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot serialize payload", e);
    }
  }

  public static JsonNode read(String value) {
    try {
      return MAPPER.readTree(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Response is not valid JSON", e);
    }
  }
}
