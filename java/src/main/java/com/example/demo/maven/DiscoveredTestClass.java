package com.example.demo.maven;

/** A {@code *Test.java} source file under {@code src/test/java}, as a runnable Surefire class name. */
public record DiscoveredTestClass(String fqcn, String simpleName, String javaPackage) {}
