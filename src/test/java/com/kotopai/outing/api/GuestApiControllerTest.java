package com.kotopai.outing.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.port.CurrentUserProvider;
import com.kotopai.outing.service.GuestOperationException;
import com.kotopai.outing.service.GuestOperationException.Reason;
import com.kotopai.outing.service.GuestService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GuestApiController.class)
@Import({OutingApiSecurityConfig.class, OutingApiExceptionHandler.class})
@WithMockUser(username = "1")
class GuestApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GuestService guests;

    @MockitoBean
    private CurrentUserProvider currentUser;

    @BeforeEach
    void setUp() {
        when(currentUser.currentUserId()).thenReturn(1L);
    }

    private static Guest guest(long id, String name) {
        Guest g = new Guest(1L, name);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    @Test
    @DisplayName("401 when nobody is logged in")
    void unauthenticated() throws Exception {
        mvc.perform(get("/api/guests").with(anonymous())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Lists the user guests")
    void listGuests() throws Exception {
        when(guests.list(1L)).thenReturn(List.of(guest(5, "Alvi"), guest(6, "Rahim")));

        mvc.perform(get("/api/guests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].name").value("Rahim"));
    }

    @Test
    @DisplayName("Create returns 201 with a Location header; a blank name is a 400; a duplicate is a 409")
    void createGuest() throws Exception {
        when(guests.create(1L, "Rahim")).thenReturn(guest(6, "Rahim"));
        when(guests.create(1L, "Dup")).thenThrow(new GuestOperationException(Reason.DUPLICATE_NAME, "You already have a guest named Dup"));

        mvc.perform(post("/api/guests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Rahim\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/guests/6"))
            .andExpect(jsonPath("$.name").value("Rahim"));
        mvc.perform(post("/api/guests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
        mvc.perform(post("/api/guests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Dup\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Delete: 204 when removed, 404 when not yours, 409 when used in an outing")
    void deleteGuest() throws Exception {
        doThrow(new GuestOperationException(Reason.NOT_FOUND, "Guest not found")).when(guests).delete(1L, 8L);
        doThrow(new GuestOperationException(Reason.IN_USE, "Rahim took part in an outing")).when(guests).delete(1L, 9L);

        mvc.perform(delete("/api/guests/6").with(csrf())).andExpect(status().isNoContent());
        verify(guests).delete(1L, 6L);
        mvc.perform(delete("/api/guests/8").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/guests/9").with(csrf())).andExpect(status().isConflict());
    }
}
