package com.example.demo.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.stack")
public class StackPingProperties {

  /** Base URL for the Rust dashboard (no trailing slash required). */
  private String rustBaseUrl = "http://127.0.0.1:8082";

  /** Base URL for the Python dashboard. */
  private String pythonBaseUrl = "http://127.0.0.1:5000";

  /** Base URL for the Prometheus UI. */
  private String prometheusBaseUrl = "http://127.0.0.1:9090";

  public String getRustBaseUrl() {
    return rustBaseUrl;
  }

  public void setRustBaseUrl(String rustBaseUrl) {
    this.rustBaseUrl = rustBaseUrl;
  }

  public String getPythonBaseUrl() {
    return pythonBaseUrl;
  }

  public void setPythonBaseUrl(String pythonBaseUrl) {
    this.pythonBaseUrl = pythonBaseUrl;
  }

  public String getPrometheusBaseUrl() {
    return prometheusBaseUrl;
  }

  public void setPrometheusBaseUrl(String prometheusBaseUrl) {
    this.prometheusBaseUrl = prometheusBaseUrl;
  }
}
