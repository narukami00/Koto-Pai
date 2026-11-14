package com.kotopai.outing.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kotopai.outing.api.dto.OutingDtos.OutingResponse;
import com.kotopai.outing.api.dto.OutingDtos.BalanceView;
import com.kotopai.outing.api.dto.OutingDtos.IssueView;
import com.kotopai.outing.api.dto.OutingDtos.OutingSummary;
import com.kotopai.outing.api.dto.OutingDtos.PreviewResponse;
import com.kotopai.outing.api.dto.OutingDtos.UserNetView;
import com.kotopai.outing.domain.IllegalOutingTransitionException;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationResult;
import com.kotopai.outing.port.CurrentUserProvider;
import com.kotopai.outing.service.OutingOperationException;
import com.kotopai.outing.service.OutingOperationException.Reason;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OutingApiController.class)
@Import({OutingApiSecurityConfig.class, OutingApiExceptionHandler.class})
@WithMockUser(username = "1")
class OutingApiControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OutingApiFacade facade;

    @MockitoBean
    private CurrentUserProvider currentUser;

    @BeforeEach
    void setUp() {
        when(currentUser.currentUserId()).thenReturn(1L);
    }

    private static OutingResponse response(long id, OutingStatus status) {
        return new OutingResponse(id, "Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 14), status, 1L,
            new BigDecimal("1900.00"), new BigDecimal("1900.00"), List.of(), List.of(), List.of());
    }

    @Test
    @DisplayName("401 when nobody is logged in (plain status, not a login redirect)")
    void unauthenticatedGets401() throws Exception {
        mvc.perform(get("/api/outings").with(anonymous())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST without a CSRF token is refused")
    void csrfRequired() throws Exception {
        mvc.perform(post("/api/outings").contentType(JSON)
                .content("{\"title\":\"x\",\"category\":\"EATING\",\"date\":\"2026-11-14\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Create returns 201, a Location header and the new outing")
    void createReturns201() throws Exception {
        when(facade.create(eq(1L), any())).thenReturn(response(7, OutingStatus.DRAFT));

        mvc.perform(post("/api/outings").with(csrf()).contentType(JSON)
                .content("{\"title\":\"Burger night\",\"category\":\"EATING\",\"date\":\"2026-11-14\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/outings/7"))
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("400 with per-field errors on an invalid body")
    void invalidBodyGets400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/outings").with(csrf()).contentType(JSON)
                .content("{\"title\":\"  \",\"category\":null,\"date\":\"2026-11-14\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.title").exists())
            .andExpect(jsonPath("$.errors.category").exists());
    }

    @Test
    @DisplayName("400 on malformed JSON")
    void malformedJson() throws Exception {
        mvc.perform(post("/api/outings").with(csrf()).contentType(JSON).content("{not json"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("List passes the limit through and returns summaries")
    void listOutings() throws Exception {
        when(facade.list(1L, 5)).thenReturn(List.of(
            new OutingSummary(7L, "Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 14), OutingStatus.ACTIVE, 1L)));

        mvc.perform(get("/api/outings").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Burger night"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        verify(facade).list(1L, 5);
    }

    @Test
    @DisplayName("404 for an unknown or invisible outing, 403 when it is not yours to change")
    void notFoundAndForbidden() throws Exception {
        when(facade.get(1L, 9L)).thenThrow(new OutingOperationException(Reason.NOT_FOUND, "Outing not found"));
        when(facade.cancel(1L, 8L)).thenThrow(new OutingOperationException(Reason.FORBIDDEN, "Only the creator can change this outing"));

        mvc.perform(get("/api/outings/9")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Outing not found"));
        mvc.perform(post("/api/outings/8/cancel").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Update returns the saved outing; bad references come back as 400")
    void updateOuting() throws Exception {
        String body = "{\"participants\":[{\"userId\":1}],\"items\":[],\"contributions\":[]}";
        when(facade.updateContents(eq(1L), eq(7L), any())).thenReturn(response(7, OutingStatus.DRAFT));

        mvc.perform(put("/api/outings/7").with(csrf()).contentType(JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));

        when(facade.updateContents(eq(1L), eq(8L), any()))
            .thenThrow(new IllegalArgumentException("item 'Tea' refers to user #99, who is not on the outing"));
        mvc.perform(put("/api/outings/8").with(csrf()).contentType(JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("Tea")));
    }

    @Test
    @DisplayName("Update validates nested items: a negative amount is a 400")
    void nestedValidation() throws Exception {
        String body = "{\"participants\":[{\"userId\":1}],"
            + "\"items\":[{\"name\":\"Tea\",\"amount\":-5,\"splitType\":\"EQUAL\",\"shares\":[]}],\"contributions\":[]}";

        mvc.perform(put("/api/outings/7").with(csrf()).contentType(JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors['items[0].amount']").exists());
    }

    @Test
    @DisplayName("Submit of an incomplete outing is 422 and lists every engine issue")
    void submitEngineIssues() throws Exception {
        when(facade.submit(1L, 7L)).thenThrow(new EngineValidationException(new ValidationResult(List.of(
            new ValidationIssue("NO_ITEMS", "Add at least one item."),
            new ValidationIssue("PAID_NOT_EQUAL_BILL", "Payments total 0.00 but the bill is 0.00")))));

        mvc.perform(post("/api/outings/7/submit").with(csrf()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.issues.length()").value(2))
            .andExpect(jsonPath("$.issues[0].code").value("NO_ITEMS"));
    }

    @Test
    @DisplayName("An illegal status change is 409; deleting a non-draft is 409")
    void conflicts() throws Exception {
        when(facade.cancel(1L, 7L)).thenThrow(new IllegalOutingTransitionException(OutingStatus.CANCELLED, "cancel"));
        doThrow(new OutingOperationException(Reason.DRAFT_ONLY, "Only a draft can be deleted")).when(facade).delete(1L, 8L);

        mvc.perform(post("/api/outings/7/cancel").with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/outings/8").with(csrf())).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Friend and guest problems are 422")
    void unprocessable() throws Exception {
        when(facade.updateContents(eq(1L), eq(7L), any()))
            .thenThrow(new OutingOperationException(Reason.NOT_A_FRIEND, "user #5 is not your friend"));

        mvc.perform(put("/api/outings/7").with(csrf()).contentType(JSON)
                .content("{\"participants\":[],\"items\":[],\"contributions\":[]}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Submit and delete of a draft succeed")
    void submitAndDelete() throws Exception {
        when(facade.submit(1L, 7L)).thenReturn(response(7, OutingStatus.ACTIVE));

        mvc.perform(post("/api/outings/7/submit").with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(delete("/api/outings/7").with(csrf())).andExpect(status().isNoContent());
        verify(facade).delete(1L, 7L);
    }

    @Test
    @DisplayName("Preview returns the numbers for unsaved input")
    void previewValid() throws Exception {
        when(facade.preview(eq(1L), any())).thenReturn(new PreviewResponse(true, List.of(),
            new BigDecimal("1900.00"), new BigDecimal("1900.00"),
            List.of(new BalanceView(1L, null, new BigDecimal("700.00"), new BigDecimal("1400.00"), new BigDecimal("700.00"))),
            List.of(new UserNetView(1L, new BigDecimal("700.00")))));

        mvc.perform(post("/api/outings/preview").with(csrf()).contentType(JSON)
                .content("{\"participants\":[{\"userId\":1}],\"items\":[],\"contributions\":[]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.balances[0].net").value(700.00))
            .andExpect(jsonPath("$.userNets[0].userId").value(1));
    }

    @Test
    @DisplayName("Incomplete input is still a 200: valid=false and every issue listed")
    void previewInvalidIsStill200() throws Exception {
        when(facade.preview(eq(1L), any())).thenReturn(new PreviewResponse(false,
            List.of(new IssueView("NO_ITEMS", "Add at least one item."), new IssueView("PAID_NOT_EQUAL_BILL", "short by 50.00")),
            new BigDecimal("200.00"), new BigDecimal("150.00"), List.of(), List.of()));

        mvc.perform(post("/api/outings/preview").with(csrf()).contentType(JSON)
                .content("{\"participants\":[],\"items\":[],\"contributions\":[]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.issues.length()").value(2))
            .andExpect(jsonPath("$.balances.length()").value(0));
    }

    @Test
    @DisplayName("Preview needs login and still validates the body shape")
    void previewSecurityAndShape() throws Exception {
        mvc.perform(post("/api/outings/preview").with(anonymous()).with(csrf()).contentType(JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/outings/preview").with(csrf()).contentType(JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.participants").exists());
    }
}
