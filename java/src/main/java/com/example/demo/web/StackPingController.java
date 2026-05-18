package com.example.demo.web;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard/stack-ping")
public class StackPingController {

  private static final Logger log = LoggerFactory.getLogger(StackPingController.class);

  private final StackPingService stackPingService;

  public StackPingController(StackPingService stackPingService) {
    this.stackPingService = stackPingService;
  }

  @GetMapping("/rust")
  public Map<String, Object> pingRust() {
    log.info("Dashboard stack ping: user clicked Rust — issuing outbound GET");
    return stackPingService.pingRust();
  }

  @GetMapping("/python")
  public Map<String, Object> pingPython() {
    log.info("Dashboard stack ping: user clicked Python — issuing outbound GET");
    return stackPingService.pingPython();
  }

  @GetMapping("/prometheus")
  public Map<String, Object> pingPrometheus() {
    log.info("Dashboard stack ping: user clicked Prometheus — issuing outbound GET");
    return stackPingService.pingPrometheus();
  }

  @GetMapping("/grafana")
  public Map<String, Object> pingGrafana() {
    log.info("Dashboard stack ping: user clicked Grafana — issuing outbound GET");
    return stackPingService.pingGrafana();
  }

  @GetMapping("/elasticsearch")
  public Map<String, Object> pingElasticsearch() {
    log.info("Dashboard stack ping: user clicked Elasticsearch — issuing outbound GET");
    return stackPingService.pingElasticsearch();
  }

  @GetMapping("/kibana")
  public Map<String, Object> pingKibana() {
    log.info("Dashboard stack ping: user clicked Kibana — issuing outbound GET");
    return stackPingService.pingKibana();
  }

  @GetMapping("/reach-ui")
  public Map<String, Object> pingReachUi() {
    log.info("Dashboard stack ping: user clicked Reach UI — issuing outbound GET");
    return stackPingService.pingReachUi();
  }
}
