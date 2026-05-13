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
}
