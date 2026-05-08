package com.example.demo.web.maventest;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.testreports.TestResultRow;
import com.example.demo.testreports.TestResultRow.Status;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BuildSurefireTestFilterEmptySkipsReturnsNullTest {

  @Test
  void buildSurefireTestFilter_emptySkips_returnsNull() {
    var row =
        new TestResultRow("m", "com.example.T", Status.PASSED, 0.1, "");
    assertThat(MavenTestRunnerService.buildSurefireTestFilter(Set.of(), List.of(row))).isNull();
  }
}
