package com.example.demo.web;

import com.example.demo.ProjectLayout;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  private final ProjectLayout projectLayout;
  private final StackPingProperties stackPing;

  public HomeController(ProjectLayout projectLayout, StackPingProperties stackPing) {
    this.projectLayout = projectLayout;
    this.stackPing = stackPing;
  }

  @GetMapping("/")
  public String home(Model model, HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
    Path projectRoot = projectLayout.resolveProjectRoot().normalize().toAbsolutePath();
    Path runDashboardHtml =
        projectRoot.resolve("src/main/resources/static/run-dashboard.html").normalize().toAbsolutePath();
    model.addAttribute("localProjectRoot", projectRoot.toString());
    model.addAttribute("localRunDashboardPath", runDashboardHtml.toString());
    Path readmeAtRepoRoot =
        projectRoot.getParent() == null ? null : projectRoot.getParent().resolve("README.md");
    if (readmeAtRepoRoot != null && Files.isRegularFile(readmeAtRepoRoot)) {
      model.addAttribute("localReadmePath", readmeAtRepoRoot.toAbsolutePath().toString());
    }
    model.addAttribute("offlineFetchHint", offlineFetchHint(runDashboardHtml));
    model.addAttribute("stackLinks", stackPing);
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
}
