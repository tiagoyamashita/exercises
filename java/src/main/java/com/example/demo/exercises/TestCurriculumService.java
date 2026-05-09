package com.example.demo.exercises;

import com.example.demo.maven.TestDiscoveryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TestCurriculumService {

  private final RequiredTestCatalog catalog;
  private final TestDiscoveryService discovery;

  public TestCurriculumService(RequiredTestCatalog catalog, TestDiscoveryService discovery) {
    this.catalog = catalog;
    this.discovery = discovery;
  }

  /** Builds checklist rows: comparing curriculum {@link RequiredTestEntry#targetFqcn()} to disk. */
  public List<RequiredTestRowView> buildRows(DashboardNotesPayload notes) {
    Set<String> onDisk = discovery.allowedFqcnSet();
    List<RequiredTestRowView> rows = new ArrayList<>();
    var byId = notes.getByTestId();
    for (RequiredTestEntry spec : catalog.entries()) {
      boolean present = onDisk.contains(spec.targetFqcn());
      String path = relativeSrcPath(spec.targetFqcn());
      String draft = byId.getOrDefault(spec.id(), "");
      rows.add(new RequiredTestRowView(spec, present, path, draft));
    }
    return rows;
  }

  /** {@code com.foo.BarTest} → {@code src/test/java/com/foo/BarTest.java}. */
  public static String relativeSrcPath(String fqcn) {
    int last = fqcn.lastIndexOf('.');
    String pkgPath = last < 0 ? "" : fqcn.substring(0, last).replace('.', '/');
    String simple = last < 0 ? fqcn : fqcn.substring(last + 1);
    if (pkgPath.isEmpty()) {
      return "src/test/java/" + simple + ".java";
    }
    return "src/test/java/" + pkgPath + "/" + simple + ".java";
  }
}
