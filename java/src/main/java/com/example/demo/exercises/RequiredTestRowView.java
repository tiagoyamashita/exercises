package com.example.demo.exercises;

/** One curriculum row: spec + whether the test class file exists + draft note for the UI. */
public record RequiredTestRowView(
    RequiredTestEntry spec,
    boolean filePresent,
    String suggestedPath,
    String noteDraft) {}
