package com.example.demo.web;

import com.example.demo.testreports.SurefireReportService;
import com.example.demo.testreports.TestResultRow;
import com.example.demo.web.maventest.MavenTestRunnerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
public class HomeController {

  /** Session key for checkbox state after a streamed full-suite run ({@code skip} query params). */
  public static final String SESSION_SKIPPED_CASE_IDS = "skippedTestCaseIds";

  private final SurefireReportService surefireReportService;
  private final MavenTestRunnerService mavenTestRunnerService;

  public HomeController(
      SurefireReportService surefireReportService,
      MavenTestRunnerService mavenTestRunnerService) {
    this.surefireReportService = surefireReportService;
    this.mavenTestRunnerService = mavenTestRunnerService;
  }

  @GetMapping("/")
  public String home(
      Model model,
      HttpServletRequest request,
      HttpServletResponse response,
      HttpSession session) {
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
    Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
    if (flashMap != null) {
      model.addAllAttributes(flashMap);
    }
    List<TestResultRow> testResults = surefireReportService.loadLatestResults();

    @SuppressWarnings("unchecked")
    Set<String> skippedFromSession =
        (Set<String>) session.getAttribute(SESSION_SKIPPED_CASE_IDS);
    Set<String> skippedTestKeys =
        skippedFromSession == null ? Set.of() : Set.copyOf(skippedFromSession);
    if (skippedFromSession != null) {
      session.removeAttribute(SESSION_SKIPPED_CASE_IDS);
    }
    model.addAttribute("skippedTestKeys", skippedTestKeys);

    model.addAttribute("testResults", testResults);
    model.addAttribute(
        "reportSources",
        testResults.isEmpty()
            ? surefireReportService.getExistingReportDirectoryPaths()
            : surefireReportService.getResolvedReportDirectoryPaths());
    model.addAttribute("testRunnerEnabled", mavenTestRunnerService.isEnabled());
    return "home";
  }

  @GetMapping({"/tests/run", "/tests/run/"})
  public String runTestsGet(RedirectAttributes redirect) {
    redirect.addFlashAttribute(
        "testRunError", "Use the \"Run tests again\" button on the home page (form POST).");
    return "redirect:/";
  }

  @PostMapping({"/tests/run", "/tests/run/"})
  public String runTestsPost(
      @RequestParam(value = "only", required = false) String onlyCaseId,
      RedirectAttributes redirect) {
    if (!mavenTestRunnerService.isEnabled()) {
      redirect.addFlashAttribute(
          "testRunError", "Re-running tests from the UI is disabled (app.test-runner.enabled=false).");
      return "redirect:/";
    }
    String only = onlyCaseId == null ? null : onlyCaseId.strip();
    boolean single = only != null && !only.isEmpty();

    MavenTestRunnerService.TestRunOutcome outcome;
    if (single) {
      outcome = mavenTestRunnerService.runSingleTest(only);
    } else {
      outcome = mavenTestRunnerService.runTests(Set.of());
    }

    if (outcome.success()) {
      redirect.addFlashAttribute("testRunMessage", outcome.message());
    } else {
      redirect.addFlashAttribute("testRunError", outcome.message());
      if (!outcome.logTail().isBlank()) {
        redirect.addFlashAttribute("testRunLogTail", outcome.logTail());
      }
    }
    return "redirect:/";
  }
}
