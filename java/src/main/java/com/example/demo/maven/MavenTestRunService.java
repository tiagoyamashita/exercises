package com.example.demo.maven;

import com.example.demo.MavenProjectLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MavenTestRunService {

  private static final int MAX_LOG_CHARS = 120_000;
  private static final int TIMEOUT_MINUTES = 15;

  private final MavenProjectLayout layout;
  private final TestDiscoveryService discovery;

  public MavenTestRunService(MavenProjectLayout layout, TestDiscoveryService discovery) {
    this.layout = layout;
    this.discovery = discovery;
  }

  public record RunOutcome(int exitCode, String combinedOutput, boolean allowedSelection) {}

  /**
   * Runs {@code mvn test -Dtest=a,b,c} for classes that appear under {@code src/test/java} (whitelist).
   */
  public RunOutcome runTestClasses(Collection<String> requestedFqcn) {
    Set<String> allowed = discovery.allowedFqcnSet();
    List<String> toRun =
        requestedFqcn.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .filter(allowed::contains)
            .sorted()
            .collect(Collectors.toList());
    if (toRun.isEmpty()) {
      return new RunOutcome(
          -1,
          "No allowed test classes in selection. (Only classes discovered under src/test/java are run.)",
          false);
    }
    String dtest = String.join(",", toRun);
    Path mavenRoot = layout.resolveMavenProjectRoot();
    List<String> command = mvnCommand(mavenRoot, dtest);
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(mavenRoot.toFile());
    pb.redirectErrorStream(true);
    try {
      Process process = pb.start();
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      Thread drain =
          new Thread(
              () -> {
                try {
                  process.getInputStream().transferTo(buf);
                } catch (IOException ignored) {
                  // ignore
                }
              },
              "maven-test-output-drain");
      drain.setDaemon(true);
      drain.start();
      boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
      if (!finished) {
        process.destroyForcibly();
        drain.join();
        return new RunOutcome(
            -124,
            truncate(
                buf.toString(StandardCharsets.UTF_8)
                    + "\n[process destroyed: exceeded "
                    + TIMEOUT_MINUTES
                    + " minute timeout]"),
            true);
      }
      drain.join();
      String raw = buf.toString(StandardCharsets.UTF_8);
      return new RunOutcome(process.exitValue(), truncate(raw), true);
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      return new RunOutcome(
          -1, "Could not run Maven: " + e.getMessage() + "\nCommand: " + String.join(" ", command), false);
    }
  }

  private static List<String> mvnCommand(Path mavenRoot, String dtestArg) {
    boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    List<String> cmd = new ArrayList<>();
    if (win) {
      Path mvnw = mavenRoot.resolve("mvnw.cmd");
      if (Files.isRegularFile(mvnw)) {
        cmd.add(mvnw.toAbsolutePath().toString());
      } else {
        cmd.add("mvn.cmd");
      }
    } else {
      Path mvnw = mavenRoot.resolve("mvnw");
      if (Files.isRegularFile(mvnw)) {
        cmd.add(mvnw.toAbsolutePath().toString());
      } else {
        cmd.add("mvn");
      }
    }
    cmd.add("test");
    cmd.add("-Dtest=" + dtestArg);
    return cmd;
  }

  private static String truncate(String s) {
    if (s.length() <= MAX_LOG_CHARS) {
      return s;
    }
    return "…[truncated head]\n\n"
        + s.substring(s.length() - MAX_LOG_CHARS + 40)
        + "\n…[end]";
  }
}
