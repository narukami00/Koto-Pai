package com.kotopai.outing.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.web.LayoutModelAdvice;
import com.kotopai.outing.controller.OutingPageFacade.ItemRow;
import com.kotopai.outing.controller.OutingPageFacade.OutingDetail;
import com.kotopai.outing.controller.OutingPageFacade.OutingList;
import com.kotopai.outing.controller.OutingPageFacade.OutingRow;
import com.kotopai.outing.controller.OutingPageFacade.PersonRow;
import com.kotopai.outing.domain.IllegalOutingTransitionException;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationResult;
import com.kotopai.outing.service.OutingOperationException;
import com.kotopai.outing.service.OutingOperationException.Reason;
import com.kotopai.outing.service.OutingService;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OutingPageController.class)
@Import({SecurityConfig.class, LayoutModelAdvice.class})
class OutingPageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OutingPageFacade pages;

    @MockitoBean
    private OutingService outings;

    @MockitoBean
    private KotopaiUserDetailsService userDetailsService;

    private KotopaiUserDetails me;

    @BeforeEach
    void setUp() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        me = new KotopaiUserDetails(user);
    }

    private static OutingDetail detail(boolean calculated, boolean actions) {
        return new OutingDetail(7L, "Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 24), OutingStatus.DRAFT,
            "Rafsan", true, actions, actions, actions, actions, new BigDecimal("1900.00"), new BigDecimal("1900.00"),
            List.of(new ItemRow("Large pizza", new BigDecimal("1000.00"), "By percentage", "Rafsan 50%, Tanvir 50%")),
            List.of(new PersonRow("Rafsan", "R", "#18604B", false, "@rafsan10",
                calculated ? new BigDecimal("500.00") : null, calculated ? new BigDecimal("1900.00") : null,
                calculated ? new BigDecimal("700.00") : null)),
            calculated ? List.of() : List.of("Add at least one item."));
    }

    @Test
    @DisplayName("The list page renders each group with its outings")
    void listPage() throws Exception {
        OutingRow row = new OutingRow(7L, "Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 24),
            OutingStatus.ACTIVE, "Rafsan", true, new BigDecimal("1900.00"));
        when(pages.list(1L)).thenReturn(new OutingList(List.of(row), List.of(), List.of()));

        mvc.perform(get("/outings").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(view().name("outings/list"))
            .andExpect(content().string(containsString("Burger night")))
            .andExpect(content().string(containsString("৳1900.00")))
            .andExpect(content().string(containsString("/outings/7")));
    }

    @Test
    @DisplayName("With no outings the list invites the user to create one")
    void emptyListPage() throws Exception {
        when(pages.list(1L)).thenReturn(new OutingList(List.of(), List.of(), List.of()));

        mvc.perform(get("/outings").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Create your first outing")));
    }

    @Test
    @DisplayName("The detail page shows people with their net, items and the creator's buttons")
    void detailPage() throws Exception {
        when(pages.detail(1L, 7L)).thenReturn(detail(true, true));

        mvc.perform(get("/outings/7").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(view().name("outings/detail"))
            .andExpect(content().string(containsString("Burger night")))
            .andExpect(content().string(containsString("+৳700.00")))
            .andExpect(content().string(containsString("Rafsan 50%, Tanvir 50%")))
            .andExpect(content().string(containsString("Submit and split")))
            .andExpect(content().string(containsString("Cancel outing")))
            .andExpect(content().string(containsString("/outings/7/edit")));
    }

    @Test
    @DisplayName("An incomplete outing explains what is missing and does not offer a working submit")
    void incompleteDetailPage() throws Exception {
        when(pages.detail(1L, 7L)).thenReturn(detail(false, true));

        mvc.perform(get("/outings/7").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("not ready to split yet")))
            .andExpect(content().string(containsString("Add at least one item.")))
            .andExpect(content().string(containsString("disabled")));
    }

    @Test
    @DisplayName("An outing that is not visible sends the user back to the list with a message")
    void invisibleOuting() throws Exception {
        when(pages.detail(1L, 9L)).thenThrow(new OutingOperationException(Reason.NOT_FOUND, "Outing not found"));

        mvc.perform(get("/outings/9").with(user(me)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/outings"))
            .andExpect(flash().attribute("flashError", "Outing not found"));
    }

    @Test
    @DisplayName("Pages need a login")
    void loginRequired() throws Exception {
        mvc.perform(get("/outings")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Submit and cancel call the service and come back to the outing with a success message")
    void submitAndCancel() throws Exception {
        mvc.perform(post("/outings/7/submit").with(user(me)).with(csrf()))
            .andExpect(redirectedUrl("/outings/7"))
            .andExpect(flash().attributeExists("flashSuccess"));
        verify(outings).submit(1L, 7L);

        mvc.perform(post("/outings/7/cancel").with(user(me)).with(csrf()))
            .andExpect(redirectedUrl("/outings/7"))
            .andExpect(flash().attributeExists("flashSuccess"));
        verify(outings).cancel(1L, 7L);
    }

    @Test
    @DisplayName("Refusals become a friendly error message, never a stack trace")
    void refusalsBecomeMessages() throws Exception {
        doThrow(new EngineValidationException(new ValidationResult(List.of(new ValidationIssue("NO_ITEMS", "Add at least one item.")))))
            .when(outings).submit(1L, 7L);
        doThrow(new IllegalOutingTransitionException(OutingStatus.CANCELLED, "cancel")).when(outings).cancel(1L, 8L);
        doThrow(new OutingOperationException(Reason.DRAFT_ONLY, "Only a draft can be deleted")).when(outings).delete(1L, 9L);

        mvc.perform(post("/outings/7/submit").with(user(me)).with(csrf()))
            .andExpect(redirectedUrl("/outings/7")).andExpect(flash().attributeExists("flashError"));
        mvc.perform(post("/outings/8/cancel").with(user(me)).with(csrf()))
            .andExpect(redirectedUrl("/outings/8")).andExpect(flash().attributeExists("flashError"));
        mvc.perform(post("/outings/9/delete").with(user(me)).with(csrf()))
            .andExpect(redirectedUrl("/outings/9")).andExpect(flash().attribute("flashError", "Only a draft can be deleted"));
    }

    @Test
    @DisplayName("Deleting a draft goes back to the list")
    void deleteDraft() throws Exception {
        mvc.perform(post("/outings/7/delete").with(user(me)).with(csrf()))
            .andExpect(redirectedUrl("/outings"))
            .andExpect(flash().attribute("flashSuccess", "Draft deleted."));
        verify(outings).delete(1L, 7L);
    }

    @Test
    @DisplayName("A form post without a CSRF token is refused")
    void csrfRequired() throws Exception {
        mvc.perform(post("/outings/7/submit").with(user(me))).andExpect(status().isForbidden());
    }
}
