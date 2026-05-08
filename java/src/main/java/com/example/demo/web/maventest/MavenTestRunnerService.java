package com.example.demo.web.maventest;

import com.example.demo.MavenProjectLayout;
import com.example.demo.testreports.SurefireReportService;
import com.example.demo.testreports.TestResultRow;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MavenTestRunnerService {

  public static final int MAX_LOG_TAIL = 6000;

  private final SurefireReportService surefireReportService;
  private final MavenProjectLayout mavenProjectLayout;

  @Value("${app.test-runner.enabled:true}")
  private boolean enabled;

  public MavenTestRunnerService(
      SurefireReportService surefireReportService, MavenProjectLayout mavenProjectLayout) {
    this.surefireReportService = surefireReportService;
    this.mavenProjectLayout = mavenProjectLayout;
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** Runs {@code mvnw test} (or {@code mvn test}) in the Maven module root (quiet Maven output). */
  public TestRunOutcome runTests(Set<String> skipCaseIds) {
    return runTests(skipCaseIds, null, null, true);
  }

  /** Runs a single known test method ({@code -Dtest=fqcn#method}). */
  public TestRunOutcome runSingleTest(String onlyCaseId) {
    return runTests(null, onlyCaseId, null, true);
  }

  /**
   * Same as {@link #runTests(Set)} but forwards each stdout line to {@code logLineConsumer} as it
   * is read (for live UI). Uses non-quiet Maven so progress is visible.
   */
  public TestRunOutcome runTestsStreaming(Set<String> skipCaseIds, Consumer<String> logLineConsumer) {
    return runTests(skipCaseIds, null, logLineConsumer, false);
  }

  /** Same as {@link #runSingleTest(String)} with live log lines (non-quiet Maven). */
  public TestRunOutcome runSingleTestStreaming(String onlyCaseId, Consumer<String> logLineConsumer) {
    return runTests(null, onlyCaseId, logLineConsumer, false);
  }

  private TestRunOutcome runTests(
      Set<String> skipCaseIds,
      String onlyCaseId,
      Consumer<String> logLineConsumer,
      boolean quietMaven) {
    try {
      Path projectRoot = mavenProjectLayout.resolveMavenProjectRoot();
      List<String> command = buildCommand(projectRoot, skipCaseIds, onlyCaseId, quietMaven);
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.directory(projectRoot.toFile());
      pb.redirectErrorStream(true);
      Process process = pb.start();
      StringBuilder log = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          log.append(line).append('\n');
          if (logLineConsumer != null) {
            logLineConsumer.accept(line);
          }
          if (log.length() > MAX_LOG_TAIL * 2) {
            log.delete(0, log.length() - MAX_LOG_TAIL);
            log.insert(0, "…\n");
          }
        }
      }
      boolean finished = process.waitFor(12, TimeUnit.MINUTES);
      if (!finished) {
        process.destroyForcibly();
        return new TestRunOutcome(false, -1, "Timed out after 12 minutes.", log.toString());
      }
      int code = process.exitValue();
      boolean ok = code == 0;
      String msg = ok ? "Tests finished successfully." : "Tests failed (exit " + code + ").";
      return new TestRunOutcome(ok, code, msg, tail(log.toString()));
    } catch (IllegalArgumentException e) {
      return new TestRunOutcome(false, -1, e.getMessage(), "");
    } catch (IOException e) {
      return new TestRunOutcome(false, -1, "IO error: " + e.getMessage(), "");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new TestRunOutcome(false, -1, "Interrupted.", "");
    }
  }

  private List<String> buildCommand(
      Path projectRoot, Set<String> skipCaseIds, String onlyCaseId, boolean quiet) {
    List<String> command = new ArrayList<>();
    boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
    Path wrapper = projectRoot.resolve(win ? "mvnw.cmd" : "mvnw");
    if (Files.isRegularFile(wrapper)) {
      Path abs = wrapper.toAbsolutePath();
      // Windows: .cmd is not a native executable for CreateProcess — must go through cmd.exe
      if (win && abs.getFileName().toString().toLowerCase().endsWith(".cmd")) {
        command.add("cmd.exe");
        command.add("/c");
      }
      command.add(abs.toString());
    } else {
      command.add(win ? "mvn.cmd" : "mvn");
    }
    command.add("test");
    if (quiet) {
      command.add("-q");
    }

    List<TestResultRow> known = surefireReportService.loadLatestResults();
    String onlyResolved = resolveKnownOnlyCaseId(onlyCaseId, known);
    if (onlyResolved != null) {
      command.add("-Dtest=" + onlyResolved);
      return command;
    }
    String testFilter = buildSurefireTestFilter(skipCaseIds, known);
    if (testFilter != null && !testFilter.isBlank()) {
      command.add("-Dtest=" + testFilter);
    }
    return command;
  }

  /**
   * Returns trimmed {@code class#method} when it matches a row in {@code knownRows}; {@code null}
   * when {@code onlyCaseId} is null or blank. Throws if non-blank but not present (stale id /
   * tampering).
   */
  static String resolveKnownOnlyCaseId(String onlyCaseId, List<TestResultRow> knownRows) {
    if (onlyCaseId == null || onlyCaseId.isBlank()) {
      return null;
    }
    String trimmed = onlyCaseId.strip();
    boolean found = knownRows.stream().anyMatch(r -> r.caseId().equals(trimmed));
    if (!found) {
      throw new IllegalArgumentException(
          "Unknown test case id; reload the page and try again.");
    }
    return trimmed;
  }

  /**
   * If skip set is empty: run full suite (no -Dtest). Otherwise run only known non-skipped cases
   * from the last report (so Maven receives a concrete -Dtest list).
   */
  static String buildSurefireTestFilter(Set<String> skipCaseIds, List<TestResultRow> knownRows) {
    if (skipCaseIds == null || skipCaseIds.isEmpty()) {
      return null;
    }
    if (knownRows.isEmpty()) {
      return null;
    }
    Map<String, List<String>> includeByClass = new LinkedHashMap<>();
    for (TestResultRow row : knownRows) {
      if (skipCaseIds.contains(row.caseId())) {
        continue;
      }
      includeByClass
          .computeIfAbsent(row.className(), k -> new ArrayList<>())
          .add(row.methodName());
    }
    if (includeByClass.isEmpty()) {
      throw new IllegalArgumentException("Every known test is marked skip; uncheck at least one row.");
    }
    return includeByClass.entrySet().stream()
        .map(
            e ->
                e.getKey()
                    + "#"
                    + e.getValue().stream().distinct().sorted().collect(Collectors.joining("+")))
        .collect(Collectors.joining(","));
  }

  private static String tail(String s) {
    if (s.length() <= MAX_LOG_TAIL) {
      return s;
    }
    return "…\n" + s.substring(s.length() - MAX_LOG_TAIL);
  }

  public record TestRunOutcome(boolean success, int exitCode, String message, String logTail) {}
}
