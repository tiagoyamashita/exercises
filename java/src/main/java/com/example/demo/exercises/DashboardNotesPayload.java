package com.example.demo.exercises;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardNotesPayload {

  private String globalNotes = "";
  private Map<String, String> byTestId = new HashMap<>();

  public String getGlobalNotes() {
    return globalNotes == null ? "" : globalNotes;
  }

  public void setGlobalNotes(String globalNotes) {
    this.globalNotes = globalNotes;
  }

  public Map<String, String> getByTestId() {
    return byTestId == null ? Map.of() : byTestId;
  }

  public void setByTestId(Map<String, String> byTestId) {
    this.byTestId = byTestId == null ? new HashMap<>() : new HashMap<>(byTestId);
  }
}
