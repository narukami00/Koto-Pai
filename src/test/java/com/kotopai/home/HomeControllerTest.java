package com.kotopai.home;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kotopai.auth.config.SecurityConfig;
import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.auth.service.KotopaiUserDetailsService;
import com.kotopai.common.web.LayoutModelAdvice;
import com.kotopai.home.HomePageFacade.BalanceRow;
import com.kotopai.home.HomePageFacade.HomeView;
import com.kotopai.outing.controller.OutingPageFacade.OutingRow;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingStatus;
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

@WebMvcTest(controllers = HomeController.class)
@Import({SecurityConfig.class, LayoutModelAdvice.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HomePageFacade home;

    @MockitoBean
    private KotopaiUserDetailsService userDetailsService;

    private KotopaiUserDetails me;

    @BeforeEach
    void setUp() {
        User user = new User("rafsan10", "Rafsan", "rafsan@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        me = new KotopaiUserDetails(user);
    }

    private static BigDecimal bdt(String value) {
        return new BigDecimal(value);
    }

    @Test
    @DisplayName("The dashboard shows the user's name, net balance, friends and active outings")
    void dashboard() throws Exception {
        when(home.forUser(1L, "Rafsan")).thenReturn(new HomeView("Rafsan", bdt("250.00"), bdt("300.00"), bdt("50.00"),
            List.of(new BalanceRow(2L, "Tanvir", "T", false, bdt("300.00")),
                new BalanceRow(10L, "Rahim", "R", true, bdt("-50.00"))),
            0,
            List.of(new OutingRow(7L, "Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 24),
                OutingStatus.ACTIVE, "Rafsan", true, bdt("1900.00"))),
            1));

        mvc.perform(get("/").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(view().name("home"))
            .andExpect(content().string(containsString("Hi, ")))
            .andExpect(content().string(containsString("Rafsan")))
            .andExpect(content().string(containsString("৳250.00")))
            .andExpect(content().string(containsString("Tanvir")))
            .andExpect(content().string(containsString("/ledger/friends/2")))
            .andExpect(content().string(containsString("Rahim")))
            .andExpect(content().string(not(containsString("/ledger/friends/10"))))
            .andExpect(content().string(containsString("Burger night")))
            .andExpect(content().string(containsString("/outings/7")))
            .andExpect(content().string(containsString("unfinished")))
            .andExpect(content().string(not(containsString("Hello, world"))));
    }

    @Test
    @DisplayName("A negative net balance is shown with a minus sign")
    void negativeNet() throws Exception {
        when(home.forUser(1L, "Rafsan")).thenReturn(new HomeView("Rafsan", bdt("-120.00"), bdt("0.00"), bdt("120.00"),
            List.of(new BalanceRow(2L, "Tanvir", "T", false, bdt("-120.00"))), 0, List.of(), 0));

        mvc.perform(get("/").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("−৳120.00")))
            .andExpect(content().string(containsString("you owe")));
    }

    @Test
    @DisplayName("A settled-up user with no outings sees friendly empty states")
    void emptyStates() throws Exception {
        when(home.forUser(1L, "Rafsan")).thenReturn(new HomeView("Rafsan", bdt("0.00"), bdt("0.00"), bdt("0.00"),
            List.of(), 0, List.of(), 0));

        mvc.perform(get("/").with(user(me)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("all settled up")))
            .andExpect(content().string(containsString("No active outings")));
    }

    @Test
    @DisplayName("Visitors who are not signed in get the welcome page with sign-up and login, and nothing is looked up")
    void anonymousSeesLanding() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("landing"))
            .andExpect(content().string(containsString("/register")))
            .andExpect(content().string(containsString("/login")))
            .andExpect(content().string(containsString("Split the bill fairly")));

        verify(home, never()).forUser(anyLong(), anyString());
    }

    @Test
    @DisplayName("The old userId preview parameter is ignored, so nobody can read another user's ledger")
    void userIdParameterIgnored() throws Exception {
        when(home.forUser(1L, "Rafsan")).thenReturn(new HomeView("Rafsan", bdt("0.00"), bdt("0.00"), bdt("0.00"),
            List.of(), 0, List.of(), 0));

        mvc.perform(get("/").param("userId", "2").with(user(me))).andExpect(status().isOk());

        verify(home).forUser(1L, "Rafsan");
        verify(home, never()).forUser(2L, "Rafsan");
    }
}
