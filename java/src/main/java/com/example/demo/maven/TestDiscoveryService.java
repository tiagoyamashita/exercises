package com.example.demo.maven;

import com.example.demo.MavenProjectLayout;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class TestDiscoveryService {

  private final MavenProjectLayout layout;

  public TestDiscoveryService(MavenProjectLayout layout) {
    this.layout = layout;
  }

  public List<DiscoveredTestClass> listTestClasses() {
    Path root = layout.resolveMavenProjectRoot().resolve("src/test/java");
    if (!Files.isDirectory(root)) {
      return List.of();
    }
    List<DiscoveredTestClass> out = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith("Test.java"))
          .sorted()
          .map(p -> toDiscovered(root, p))
          .forEach(out::add);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out;
  }

  public Set<String> allowedFqcnSet() {
    return listTestClasses().stream()
        .map(DiscoveredTestClass::fqcn)
        .collect(Collectors.toCollection(HashSet::new));
  }

  private static DiscoveredTestClass toDiscovered(Path testJavaRoot, Path file) {
    Path rel = testJavaRoot.relativize(file);
    String fqcn = fqcnFromRelativePath(rel);
    int lastDot = fqcn.lastIndexOf('.');
    String simple = lastDot < 0 ? fqcn : fqcn.substring(lastDot + 1);
    String pkg = lastDot < 0 ? "(default package)" : fqcn.substring(0, lastDot);
    return new DiscoveredTestClass(fqcn, simple, pkg);
  }

  /** Converts {@code com/example/demo/FooTest.java} to {@code com.example.demo.FooTest}. */
  static String fqcnFromRelativePath(Path relativeToTestJava) {
    List<String> parts = new ArrayList<>();
    for (Path part : relativeToTestJava) {
      parts.add(part.toString());
    }
    if (parts.isEmpty()) {
      return "";
    }
    String last = parts.get(parts.size() - 1);
    if (!last.endsWith("Test.java")) {
      return "";
    }
    parts.set(parts.size() - 1, last.substring(0, last.length() - ".java".length()));
    return String.join(".", parts);
  }
}
