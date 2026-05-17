package com.example.demo.web;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard/items")
public class RustItemRelayApiController {

  private final RustItemRelayService rustItemRelayService;

  public RustItemRelayApiController(RustItemRelayService rustItemRelayService) {
    this.rustItemRelayService = rustItemRelayService;
  }

  /** AJAX from home page: calls Rust {@code POST /api/items?name=…} and returns JSON for display. */
  @PostMapping(value = "/add-via-rust", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> addViaRustJson(@RequestParam("name") String name) {
    return rustItemRelayService.addItemViaRust(name);
  }
}
