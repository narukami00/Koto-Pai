package com.kotopai.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unit test for {@link GlobalExceptionHandler}.
 *
 * <p>Uses a standalone MockMvc setup with a dummy controller that throws each
 * exception type. No Spring context is started — this is a pure unit test.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class DummyController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Thing", 99L);
        }

        @GetMapping("/test/forbidden")
        String forbidden() {
            throw new ForbiddenException("Access denied");
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new DuplicateResourceException("Already exists");
        }

        @GetMapping("/test/bad-argument")
        String badArgument() {
            throw new IllegalArgumentException("Bad input");
        }
    }

    @Test
    void notFoundException_returns404WithJsonBody() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Thing not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void forbiddenException_returns403WithJsonBody() throws Exception {
        mvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void duplicateResourceException_returns409WithJsonBody() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Already exists"));
    }

    @Test
    void illegalArgumentException_returns400WithJsonBody() throws Exception {
        mvc.perform(get("/test/bad-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bad input"));
    }
}
