package com.example.demo.web.maventest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.testreports.TestResultRow;
import com.example.demo.testreports.TestResultRow.Status;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BuildSurefireTestFilterAllSkippedThrowsTest {

  @Test
  void buildSurefireTestFilter_allSkipped_throws() {
    var a = new TestResultRow("a", "com.example.T", Status.PASSED, 0.1, "");
    assertThatThrownBy(() -> MavenTestRunnerService.buildSurefireTestFilter(Set.of(a.caseId()), List.of(a)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
