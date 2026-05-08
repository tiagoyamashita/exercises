package com.example.demo.web.maventest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ResolveKnownOnlyCaseIdBlankReturnsNullTest {

  @Test
  void resolveKnownOnlyCaseId_blank_returnsNull() {
    assertThat(MavenTestRunnerService.resolveKnownOnlyCaseId(null, List.of())).isNull();
    assertThat(MavenTestRunnerService.resolveKnownOnlyCaseId("  ", List.of())).isNull();
  }
}
