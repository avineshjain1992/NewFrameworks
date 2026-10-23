package workshop.support;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class TraceRedactor {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Set<String> BLOCKED =
      Set.of(
          "headers",
          "extrahttpheaders",
          "result",
          "value",
          "localstorage",
          "sessionstorage",
          "origins",
          "postdata",
          "cookies",
          "storagestate",
          "httpcredentials",
          "password",
          "token",
          "authorization",
          "params",
          "snapshot",
          "sha1",
          "_sha1");

  private TraceRedactor() {}

  private static JsonNode clean(JsonNode node, Config config) {
    if (node.isObject()) {
      ObjectNode out = MAPPER.createObjectNode();
      node.fields()
          .forEachRemaining(
              e ->
                  out.set(
                      e.getKey(),
                      BLOCKED.contains(e.getKey().toLowerCase(Locale.ROOT))
                          ? e.getValue().isArray()
                              ? MAPPER.createArrayNode()
                              : e.getValue().isObject()
                                  ? MAPPER.createObjectNode()
                                  : TextNode.valueOf("[REDACTED]")
                          : clean(e.getValue(), config)));
      return out;
    }
    if (node.isArray()) {
      ArrayNode out = MAPPER.createArrayNode();
      for (JsonNode n : node) out.add(clean(n, config));
      return out;
    }
    if (node.isTextual()) {
      String text = node.asText();
      for (String secret : List.of(config.username, config.password, config.token))
        if (!secret.isBlank()) text = text.replace(secret, "[REDACTED]");
      return TextNode.valueOf(text.replaceAll("(?i)Bearer\\s+[^\\s\"\\\\]+", "Bearer [REDACTED]"));
    }
    return node;
  }

  public static void sanitize(Path file, Config config) throws IOException {
    Path temporary = file.resolveSibling(file.getFileName() + ".sanitized");
    try (ZipInputStream input = new ZipInputStream(Files.newInputStream(file));
        ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temporary))) {
      for (ZipEntry entry; (entry = input.getNextEntry()) != null; ) {
        String name = entry.getName();
        byte[] data = input.readAllBytes();
        if (name.startsWith("resources/") && !name.endsWith(".jpeg") && !name.endsWith(".png"))
          continue;
        if (name.endsWith(".network")) data = new byte[0];
        if (name.endsWith(".trace") || name.endsWith(".stacks")) {
          StringBuilder text = new StringBuilder();
          for (String line : new String(data, StandardCharsets.UTF_8).split("\\n")) {
            if (line.isBlank()) continue;
            JsonNode event = MAPPER.readTree(line);
            if (Set.of("frame-snapshot", "console", "event").contains(event.path("type").asText()))
              continue;
            text.append(MAPPER.writeValueAsString(clean(event, config))).append('\n');
          }
          data = text.toString().getBytes(StandardCharsets.UTF_8);
        }
        output.putNextEntry(new ZipEntry(name));
        output.write(data);
        output.closeEntry();
      }
    } catch (IOException | RuntimeException e) {
      Files.deleteIfExists(file);
      Files.deleteIfExists(temporary);
      throw e;
    }
    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
  }
}
