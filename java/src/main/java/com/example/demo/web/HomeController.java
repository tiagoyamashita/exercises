package com.example.demo.web;

import com.example.demo.MavenProjectLayout;
import com.example.demo.exercises.DashboardNotesPayload;
import com.example.demo.exercises.ExerciseNotesService;
import com.example.demo.maven.DiscoveredTestClass;
import com.example.demo.maven.MavenTestRunService;
import com.example.demo.maven.TestCategoryClassifier;
import com.example.demo.maven.TestDiscoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
public class HomeController {

  private final TestDiscoveryService testDiscoveryService;
  private final MavenTestRunService mavenTestRunService;
  private final ExerciseNotesService exerciseNotesService;
  private final MavenProjectLayout mavenProjectLayout;

  public HomeController(
      TestDiscoveryService testDiscoveryService,
      MavenTestRunService mavenTestRunService,
      ExerciseNotesService exerciseNotesService,
      MavenProjectLayout mavenProjectLayout) {
    this.testDiscoveryService = testDiscoveryService;
    this.mavenTestRunService = mavenTestRunService;
    this.exerciseNotesService = exerciseNotesService;
    this.mavenProjectLayout = mavenProjectLayout;
  }

  @GetMapping("/")
  public String home(
      Model model, HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
    Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
    if (flashMap != null) {
      model.addAllAttributes(flashMap);
    }
    List<DiscoveredTestClass> discoveredTests;
    try {
      discoveredTests = testDiscoveryService.listTestClasses();
    } catch (RuntimeException e) {
      discoveredTests = List.of();
      model.addAttribute("discoveryError", e.getMessage());
    }
    model.addAttribute("discoveredTests", discoveredTests);
    model.addAttribute("testCategoryGroups", TestCategoryClassifier.groupByCategory(discoveredTests));
    Path mavenRoot = mavenProjectLayout.resolveMavenProjectRoot().normalize().toAbsolutePath();
    Path runDashboardHtml =
        mavenRoot.resolve("src/main/resources/static/run-dashboard.html").normalize().toAbsolutePath();
    model.addAttribute("localMavenModuleRoot", mavenRoot.toString());
    model.addAttribute("localRunDashboardPath", runDashboardHtml.toString());
    Path readmeAtRepoRoot =
        mavenRoot.getParent() == null ? null : mavenRoot.getParent().resolve("README.md");
    if (readmeAtRepoRoot != null && Files.isRegularFile(readmeAtRepoRoot)) {
      model.addAttribute("localReadmePath", readmeAtRepoRoot.toAbsolutePath().toString());
    }
    model.addAttribute("offlineFetchHint", offlineFetchHint(runDashboardHtml));
    return "home";
  }

  private static String offlineFetchHint(Path localRunDashboardHtml) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        " If this keeps happening, start the Spring Boot app from the java/ directory: ");
    sb.append("./mvnw spring-boot:run");
    sb.append(" (Windows from repo root: ");
    sb.append(".\\java\\mvnw.cmd spring-boot:run");
    sb.append(").");
    sb.append(" Offline checklist on this machine: ");
    sb.append(localRunDashboardHtml);
    sb.append(".");
    return sb.toString();
  }

  @PostMapping("/dashboard/notes")
  public String saveDashboardNotes(
      @RequestParam(value = "globalNotes", required = false, defaultValue = "") String globalNotes,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    DashboardNotesPayload payload = new DashboardNotesPayload();
    payload.setGlobalNotes(globalNotes);
    Map<String, String> byId = new HashMap<>();
    request
        .getParameterMap()
        .forEach(
            (key, values) -> {
              if (key.startsWith("note_") && values != null && values.length > 0) {
                byId.put(key.substring("note_".length()), values[0]);
              }
            });
    payload.setByTestId(byId);
    try {
      exerciseNotesService.save(payload);
      redirectAttributes.addFlashAttribute(
          "notesSaved",
          "Notes saved to reports/dashboard-notes.json under the Maven module.");
    } catch (IOException e) {
      redirectAttributes.addFlashAttribute(
          "notesSaveError", "Could not save notes: " + e.getMessage());
    }
    return "redirect:/";
  }

  @PostMapping("/tests/run")
  public String runTests(
      @RequestParam(value = "testClass", required = false) List<String> testClasses,
      RedirectAttributes redirectAttributes) {
    if (testClasses == null || testClasses.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "testRunError", "No test classes submitted. Select checkboxes or use Run one.");
      return "redirect:/";
    }
    MavenTestRunService.RunOutcome outcome = mavenTestRunService.runTestClasses(testClasses);
    redirectAttributes.addFlashAttribute("testRunLogTail", outcome.combinedOutput());
    if (!outcome.allowedSelection()) {
      redirectAttributes.addFlashAttribute("testRunError", outcome.combinedOutput());
      return "redirect:/";
    }
    if (outcome.exitCode() == 0) {
      redirectAttributes.addFlashAttribute(
          "testRunMessage",
          "Maven test finished successfully (exit 0). Refresh this page to reload Surefire results.");
    } else {
      redirectAttributes.addFlashAttribute(
          "testRunError",
          "Maven test finished with exit code " + outcome.exitCode() + ". See log below.");
    }
    return "redirect:/";
  }
}
