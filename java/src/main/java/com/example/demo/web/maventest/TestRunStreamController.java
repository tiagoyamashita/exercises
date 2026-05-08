package com.example.demo.web.maventest;

import com.example.demo.web.HomeController;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class TestRunStreamController {

  private final MavenTestRunnerService mavenTestRunnerService;

  public TestRunStreamController(MavenTestRunnerService mavenTestRunnerService) {
    this.mavenTestRunnerService = mavenTestRunnerService;
  }

  /**
   * Runs Maven like {@code POST /tests/run}, but writes each stdout line to the response body as it
   * arrives so the browser can show live output (see home page script).
   */
  @PostMapping(
      value = {"/tests/run-stream", "/tests/run-stream/"},
      produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<StreamingResponseBody> runTestsStream(
      @RequestParam(value = "skip", required = false) List<String> skip,
      @RequestParam(value = "only", required = false) String onlyCaseId,
      HttpSession session) {
    if (!mavenTestRunnerService.isEnabled()) {
      StreamingResponseBody err =
          out -> {
            try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
              w.write("Re-running tests from the UI is disabled (app.test-runner.enabled=false).\n");
            }
          };
      return ResponseEntity.status(403)
          .contentType(MediaType.TEXT_PLAIN)
          .body(err);
    }

    String only = onlyCaseId == null ? null : onlyCaseId.strip();
    boolean single = only != null && !only.isEmpty();
    final Set<String> skips = skip == null ? Set.of() : new HashSet<>(skip);
    if (!single) {
      session.setAttribute(HomeController.SESSION_SKIPPED_CASE_IDS, new HashSet<>(skips));
    }

    StreamingResponseBody body =
        out -> {
          try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            MavenTestRunnerService.TestRunOutcome outcome =
                single
                    ? mavenTestRunnerService.runSingleTestStreaming(
                        only,
                        line -> {
                          try {
                            writeLine(w, line);
                          } catch (IOException e) {
                            throw new UncheckedIOException(e);
                          }
                        })
                    : mavenTestRunnerService.runTestsStreaming(
                        skips,
                        line -> {
                          try {
                            writeLine(w, line);
                          } catch (IOException e) {
                            throw new UncheckedIOException(e);
                          }
                        });
            writeLine(w, "");
            writeLine(
                w,
                outcome.success()
                    ? "--- " + outcome.message() + " ---"
                    : "--- " + outcome.message() + " ---");
            if (!outcome.success() && outcome.logTail() != null && !outcome.logTail().isBlank()) {
              writeLine(w, "");
              writeLine(w, outcome.logTail());
            }
          }
        };

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8")
        .body(body);
  }

  private static void writeLine(Writer w, String line) throws IOException {
    w.write(line);
    w.write('\n');
    w.flush();
  }
}
