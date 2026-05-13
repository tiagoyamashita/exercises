package com.example.demo.web;

import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class StackPingService {

  private final RestClient restClient;
  private final StackPingProperties properties;

  public StackPingService(
      @Qualifier("stackPingRestClient") RestClient stackPingRestClient,
      StackPingProperties properties) {
    this.restClient = stackPingRestClient;
    this.properties = properties;
  }

  public Map<String, Object> emptyGet(String stack, String baseUrl) {
    String root = normalizeRoot(baseUrl);
    try {
      ResponseEntity<Void> res =
          restClient.get().uri(URI.create(root)).retrieve().toBodilessEntity();
      return Map.of(
          "stack", stack,
          "url", root,
          "ok", true,
          "status", res.getStatusCode().value());
    } catch (RestClientException e) {
      return Map.of(
          "stack", stack,
          "url", root,
          "ok", false,
          "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
    }
  }

  public Map<String, Object> pingRust() {
    return emptyGet("rust", properties.getRustBaseUrl());
  }

  public Map<String, Object> pingPython() {
    return emptyGet("python", properties.getPythonBaseUrl());
  }

  public Map<String, Object> pingPrometheus() {
    return emptyGet("prometheus", properties.getPrometheusBaseUrl());
  }

  private static String normalizeRoot(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "http://127.0.0.1/";
    }
    String t = baseUrl.trim();
    return t.endsWith("/") ? t : t + "/";
  }
}
