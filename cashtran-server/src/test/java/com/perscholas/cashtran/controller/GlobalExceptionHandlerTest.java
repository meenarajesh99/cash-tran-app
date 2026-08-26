package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dto.UpdateEmailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @RestController
  @RequestMapping("/test")
  public static class TestController {

    @PostMapping("/validate")
    public void validate(@Valid @RequestBody UpdateEmailDTO dto) {}

    @GetMapping("/data-integrity")
    public void dataIntegrity() {
      throw new DataIntegrityViolationException("unique constraint violation");
    }

    @GetMapping("/runtime")
    public void runtime() {
      throw new RuntimeException("boom");
    }
  }

  @BeforeEach
  void setup() {
    org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator =
        new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
            .setValidator(validator)
            .build();
  }

  @Test
  void validationErrorsReturn400AndFieldDetails() throws Exception {
    mockMvc
        .perform(
            post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"\" }"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors[0].field").value("email"));
  }

  @Test
  void dataIntegrityReturns409() throws Exception {
    mockMvc.perform(get("/test/data-integrity")).andExpect(status().isConflict()).andExpect(
        jsonPath("$.status").value(409));
  }

  @Test
  void unexpectedExceptionReturns500() throws Exception {
    mockMvc.perform(get("/test/runtime")).andExpect(status().isInternalServerError()).andExpect(
        jsonPath("$.status").value(500)).andExpect(jsonPath("$.message").value("boom"));
  }
}

