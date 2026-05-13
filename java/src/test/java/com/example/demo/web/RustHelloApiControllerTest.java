package com.example.demo.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RustHelloApiController.class)
class RustHelloApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void helloFromRust_returnsJson() throws Exception {
    mockMvc
        .perform(get("/api/hello-from-rust"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Hello from Java"))
        .andExpect(jsonPath("$.path").value("/api/hello-from-rust"));
  }
}
