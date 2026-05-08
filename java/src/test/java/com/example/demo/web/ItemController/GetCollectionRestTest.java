package com.example.demo.web.ItemController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class GetCollectionRestTest extends RestTestSupport {

  @Test
  void getCollection_returnsOk() throws Exception {
    mockMvc.perform(get("/api/items").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }
}
