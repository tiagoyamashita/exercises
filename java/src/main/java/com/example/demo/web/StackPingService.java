package com.example.demo.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
    } catch (RestClientResponseException e) {
      return Map.of(
          "stack", stack,
          "url", root,
          "ok", false,
          "status", e.getStatusCode().value(),
          "error", e.getStatusText() != null ? e.getStatusText() : e.getMessage());
    } catch (ResourceAccessException e) {
      String hint =
          "Cannot connect (is the container running on the Compose network?). ";
      String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      return Map.of(
          "stack", stack,
          "url", root,
          "ok", false,
          "error", hint + msg);
    } catch (RestClientException e) {
      return Map.of(
          "stack", stack,
          "url", root,
          "ok", false,
          "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
    }
  }

  public Map<String, Object> pingAll() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put(
        "results",
        List.of(
            pingRust(),
            pingPython(),
            pingPrometheus(),
            pingGrafana(),
            pingElasticsearch(),
            pingKibana(),
            pingReachUi()));
    return body;
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

  public Map<String, Object> pingGrafana() {
    return emptyGet("grafana", properties.getGrafanaBaseUrl());
  }

  public Map<String, Object> pingElasticsearch() {
    return emptyGet("elasticsearch", properties.getElasticsearchBaseUrl());
  }

  public Map<String, Object> pingKibana() {
    return emptyGet("kibana", properties.getKibanaBaseUrl());
  }

  public Map<String, Object> pingReachUi() {
    return emptyGet("reach-ui", properties.getReachUiBaseUrl());
  }

  private static String normalizeRoot(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "http://127.0.0.1/";
    }
    String t = baseUrl.trim();
    return t.endsWith("/") ? t : t + "/";
  }
}
