package com.example.demo.exercises.controller.item;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * End-to-end REST CRUD on {@code /api/items}: create → verify → update → verify → delete → verify
 * gone.
 */
class CrudFlowRestTest extends RestTestSupport {

  @Test
  void crud_create_confirmExists_update_confirmUpdated_delete_confirmGone() throws Exception {
    String initialName = "crud-alpha";
    String updatedName = "crud-beta";

    long id = createItemAndReturnId(initialName);

    mockMvc
        .perform(get("/api/items/" + id).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value(initialName));

    mockMvc
        .perform(
            put("/api/items/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + updatedName + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value(updatedName));

    mockMvc
        .perform(get("/api/items/" + id).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(updatedName));

    mockMvc.perform(delete("/api/items/" + id)).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/items/" + id)).andExpect(status().isNotFound());
  }
}
