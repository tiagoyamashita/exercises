package com.example.demo.web.maventest;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.testreports.TestResultRow;
import com.example.demo.testreports.TestResultRow.Status;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BuildSurefireTestFilterOneSkippedBuildsIncludeForRestTest {

  @Test
  void buildSurefireTestFilter_oneSkipped_buildsIncludeForRest() {
    var a = new TestResultRow("a", "com.example.T", Status.PASSED, 0.1, "");
    var b = new TestResultRow("b", "com.example.T", Status.PASSED, 0.2, "");
    String filter = MavenTestRunnerService.buildSurefireTestFilter(Set.of(a.caseId()), List.of(a, b));
    assertThat(filter).isEqualTo("com.example.T#b");
  }
}
