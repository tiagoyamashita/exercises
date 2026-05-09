package com.example.demo.exercises;

import com.example.demo.MavenProjectLayout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class ExerciseNotesService {

  private static final String FILE_NAME = "dashboard-notes.json";

  private final MavenProjectLayout layout;
  private final ObjectMapper objectMapper;

  public ExerciseNotesService(MavenProjectLayout layout, ObjectMapper objectMapper) {
    this.layout = layout;
    this.objectMapper = objectMapper;
  }

  public Path notesFilePath() {
    return layout.resolveMavenProjectRoot().resolve("reports").resolve(FILE_NAME);
  }

  public DashboardNotesPayload load() {
    Path p = notesFilePath();
    if (!Files.isRegularFile(p)) {
      return new DashboardNotesPayload();
    }
    try {
      return objectMapper.readValue(p.toFile(), DashboardNotesPayload.class);
    } catch (IOException e) {
      DashboardNotesPayload empty = new DashboardNotesPayload();
      empty.setGlobalNotes("(Could not read notes file: " + e.getMessage() + ")");
      return empty;
    }
  }

  public void save(DashboardNotesPayload payload) throws IOException {
    Path p = notesFilePath();
    Files.createDirectories(p.getParent());
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(p.toFile(), payload);
  }
}
