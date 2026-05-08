package com.example.demo.testreports;

public record TestResultRow(
    String methodName,
    String className,
    Status status,
    double seconds,
    /** Short message from Surefire {@code failure}/{@code error}/{@code skipped}, empty when passed. */
    String detail) {

  public String durationLabel() {
    return String.format("%.3f s", seconds);
  }

  /** Stable id for checkbox / Surefire-style {@code classname#methodName}. */
  public String caseId() {
    return className + "#" + methodName;
  }

  /** Package part of {@link #className} for display (Surefire FQCN). */
  public String javaPackage() {
    int dot = className.lastIndexOf('.');
    return dot < 0 ? "(default package)" : className.substring(0, dot);
  }

  /** Simple name part of {@link #className} (after last {@code '.'}). */
  public String simpleClassName() {
    int dot = className.lastIndexOf('.');
    return dot < 0 ? className : className.substring(dot + 1);
  }

  public enum Status {
    PASSED,
    FAILED,
    SKIPPED
  }
}
