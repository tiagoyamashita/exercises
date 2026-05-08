package com.example.demo;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/** Deliberately failing test — no fix implemented; remove or rewrite when you want CI green again. */
class IntentionalFailureTest {

  @Test
  void failsUntilResolved() {
    fail("intentional failure — replace or delete this test when ready");
  }
}
