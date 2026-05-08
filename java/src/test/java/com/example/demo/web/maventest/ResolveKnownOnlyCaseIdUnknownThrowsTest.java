package com.example.demo.web.maventest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.testreports.TestResultRow;
import com.example.demo.testreports.TestResultRow.Status;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResolveKnownOnlyCaseIdUnknownThrowsTest {

  @Test
  void resolveKnownOnlyCaseId_unknown_throws() {
    var row = new TestResultRow("m", "com.example.T", Status.PASSED, 0.1, "");
    assertThatThrownBy(() -> MavenTestRunnerService.resolveKnownOnlyCaseId("com.other.X#m", List.of(row)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown");
  }
}
