package com.example.demo.testreports;

import com.example.demo.MavenProjectLayout;
import com.example.demo.testreports.TestResultRow.Status;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class SurefireReportService {

  private final MavenProjectLayout mavenProjectLayout;
  private final Path configuredReportsDir;

  public SurefireReportService(
      MavenProjectLayout mavenProjectLayout,
      @Value("${app.surefire-reports-dir:}") String reportsDir) {
    this.mavenProjectLayout = mavenProjectLayout;
    this.configuredReportsDir =
        reportsDir == null || reportsDir.isBlank()
            ? null
            : Path.of(reportsDir).toAbsolutePath().normalize();
  }

  /** Absolute paths that were scanned for {@code TEST-*.xml} (may be empty). */
  public List<String> getResolvedReportDirectoryPaths() {
    return resolveExistingReportDirectories().stream().map(Path::toString).toList();
  }

  /**
   * Directories that typically hold Surefire XML and currently exist (including empty folders). Used
   * for UI hints when no {@code TEST-*.xml} files are present yet.
   */
  public List<String> getExistingReportDirectoryPaths() {
    LinkedHashSet<Path> dirs = new LinkedHashSet<>();
    if (configuredReportsDir != null) {
      Path p = configuredReportsDir;
      if (Files.isDirectory(p)) {
        dirs.add(p);
      }
      return dirs.stream().map(Path::toString).toList();
    }
    Path primary = mavenProjectLayout.resolveSurefireReportsDir();
    Path base = Path.of("").toAbsolutePath().normalize();
    for (Path candidate :
        List.of(primary, base.resolve("java/target/surefire-reports"), base.resolve("target/surefire-reports"))) {
      Path norm = candidate.normalize();
      if (Files.isDirectory(norm)) {
        dirs.add(norm);
      }
    }
    return dirs.stream().map(Path::toString).toList();
  }

  public List<TestResultRow> loadLatestResults() {
    record RowSource(TestResultRow row, long sourceFileMtimeMillis) {}

    Map<String, RowSource> bestByCaseId = new HashMap<>();
    for (Path dir : resolveExistingReportDirectories()) {
      List<Path> reportFiles = listReportXmlFiles(dir);
      for (Path file : reportFiles) {
        long mtimeMillis;
        try {
          mtimeMillis = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
          continue;
        }
        for (TestResultRow row : parseReportFile(file)) {
          String caseId = row.caseId();
          RowSource prev = bestByCaseId.get(caseId);
          if (prev == null || mtimeMillis > prev.sourceFileMtimeMillis()) {
            bestByCaseId.put(caseId, new RowSource(row, mtimeMillis));
          }
        }
      }
    }
    return bestByCaseId.values().stream()
        .map(RowSource::row)
        .sorted(
            Comparator.comparing(SurefireReportService::statusSortKey)
                .thenComparing(TestResultRow::className)
                .thenComparing(TestResultRow::methodName))
        .toList();
  }

  /**
   * Prefer the same {@code target/surefire-reports} tree Maven uses for the resolved module (see
   * {@link MavenProjectLayout}), so re-runs replace XML in that folder and this page refreshes.
   * When an explicit {@code app.surefire-reports-dir} is set, only that directory is used.
   * Otherwise, if the module reports dir contains at least one {@code TEST-*.xml}, only that
   * directory is scanned (avoids merging stale copies from a parent {@code target/}). If that
   * folder is missing or empty of reports, fallbacks under the process working directory are used.
   */
  private List<Path> resolveExistingReportDirectories() {
    LinkedHashSet<Path> existing = new LinkedHashSet<>();
    if (configuredReportsDir != null) {
      if (Files.isDirectory(configuredReportsDir)) {
        existing.add(configuredReportsDir);
      }
      return new ArrayList<>(existing);
    }
    Path primary = mavenProjectLayout.resolveSurefireReportsDir();
    // Prefer the module's surefire tree only when it actually has reports. An empty directory
    // (e.g. leftover folder without TEST-*.xml) must not block fallbacks, or the page shows nothing.
    if (Files.isDirectory(primary) && !listReportXmlFiles(primary).isEmpty()) {
      existing.add(primary);
      return new ArrayList<>(existing);
    }
    Path base = Path.of("").toAbsolutePath().normalize();
    for (Path extra :
        List.of(
            primary,
            base.resolve("java/target/surefire-reports"),
            base.resolve("target/surefire-reports"))) {
      Path norm = extra.normalize();
      if (Files.isDirectory(norm) && !listReportXmlFiles(norm).isEmpty()) {
        existing.add(norm);
      }
    }
    return new ArrayList<>(existing);
  }

  private static List<Path> listReportXmlFiles(Path dir) {
    List<Path> out = new ArrayList<>();
    try (Stream<Path> stream = Files.list(dir)) {
      stream
          .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().startsWith("TEST-"))
          .filter(p -> p.getFileName().toString().endsWith(".xml"))
          .sorted(Comparator.comparing(p -> p.getFileName().toString()))
          .forEach(out::add);
    } catch (IOException e) {
      return List.of();
    }
    return out;
  }

  private List<TestResultRow> parseReportFile(Path path) {
    try (InputStream in = Files.newInputStream(path)) {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      var doc = factory.newDocumentBuilder().parse(in);
      doc.getDocumentElement().normalize();
      List<TestResultRow> rows = new ArrayList<>();
      collectTestCasesRecursively(doc.getDocumentElement(), rows);
      return rows;
    } catch (Exception e) {
      return List.of();
    }
  }

  /** Picks up {@code testcase} nodes and nested {@code testsuite} blocks (e.g. JUnit {@code @Nested}). */
  private void collectTestCasesRecursively(Element suite, List<TestResultRow> rows) {
    String suiteFallbackClass = suite.getAttribute("name");
    NodeList children = suite.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (!(children.item(i) instanceof Element el)) {
        continue;
      }
      if (localNameIs(el, "testcase")) {
        String name = el.getAttribute("name");
        String classname = el.getAttribute("classname");
        if (classname.isBlank()) {
          classname = suiteFallbackClass;
        }
        double seconds = parseTime(el.getAttribute("time"));
        Status status = statusFor(el);
        String detail = extractDetail(el, status);
        rows.add(new TestResultRow(name, classname, status, seconds, detail));
      } else if (localNameIs(el, "testsuite")) {
        collectTestCasesRecursively(el, rows);
      }
    }
  }

  /**
   * Namespaced Surefire reports use a default xmlns; {@link Element#getElementsByTagName(String)} then
   * misses {@code <failure>}/{@code <error>}, so everything looks PASSED. Match by local name with NS
   * wildcard and legacy fallback.
   */
  private static Status statusFor(Element testcase) {
    if (hasOutcomeElement(testcase, "failure")
        || hasOutcomeElement(testcase, "error")
        || hasOutcomeElement(testcase, "rerunFailure")) {
      return Status.FAILED;
    }
    if (hasOutcomeElement(testcase, "skipped")) {
      return Status.SKIPPED;
    }
    return Status.PASSED;
  }

  private static int statusSortKey(TestResultRow row) {
    return switch (row.status()) {
      case FAILED -> 0;
      case SKIPPED -> 1;
      case PASSED -> 2;
    };
  }

  private static String extractDetail(Element testcase, Status status) {
    if (status == Status.PASSED) {
      return "";
    }
    if (status == Status.FAILED) {
      for (String tag : List.of("failure", "error", "rerunFailure")) {
        String from = firstMessageFrom(testcase, tag);
        if (!from.isBlank()) {
          return from;
        }
      }
      return "";
    }
    return firstMessageFrom(testcase, "skipped");
  }

  private static boolean localNameIs(Element el, String localName) {
    String ln = el.getLocalName();
    if (ln != null && localName.equals(ln)) {
      return true;
    }
    return localName.equals(el.getTagName());
  }

  /** Any descendant with this local name (Surefire outcome tags), including default-namespace XML. */
  private static boolean hasOutcomeElement(Element parent, String localName) {
    var ns = parent.getElementsByTagNameNS("*", localName);
    if (ns.getLength() > 0) {
      return true;
    }
    return parent.getElementsByTagName(localName).getLength() > 0;
  }

  private static String firstMessageFrom(Element testcase, String tag) {
    Element el = firstDescendantElement(testcase, tag);
    if (el == null) {
      return "";
    }
    String message = el.getAttribute("message");
    if (message != null && !message.isBlank()) {
      return truncateOneLine(message, 450);
    }
    String type = el.getAttribute("type");
    String body = el.getTextContent();
    if (body != null && !body.isBlank()) {
      return truncateOneLine(type + ": " + body.strip(), 450);
    }
    return truncateOneLine(type, 450);
  }

  private static Element firstDescendantElement(Element parent, String localName) {
    var ns = parent.getElementsByTagNameNS("*", localName);
    if (ns.getLength() > 0) {
      return (Element) ns.item(0);
    }
    var legacy = parent.getElementsByTagName(localName);
    if (legacy.getLength() > 0) {
      return (Element) legacy.item(0);
    }
    return null;
  }

  private static String truncateOneLine(String s, int max) {
    if (s == null) {
      return "";
    }
    String one = s.replaceAll("\\s+", " ").strip();
    if (one.length() <= max) {
      return one;
    }
    return one.substring(0, max - 1) + "…";
  }

  private static double parseTime(String raw) {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    try {
      return Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
