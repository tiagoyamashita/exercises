package com.example.demo.web.maventest;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.testreports.TestResultRow;
import com.example.demo.testreports.TestResultRow.Status;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResolveKnownOnlyCaseIdFoundReturnsTrimmedIdTest {

  @Test
  void resolveKnownOnlyCaseId_found_returnsTrimmedId() {
    var row = new TestResultRow("m", "com.example.T", Status.PASSED, 0.1, "");
    assertThat(MavenTestRunnerService.resolveKnownOnlyCaseId("  com.example.T#m  ", List.of(row)))
        .isEqualTo("com.example.T#m");
  }
}
