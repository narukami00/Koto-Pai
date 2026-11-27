package com.kotopai.home;

import com.kotopai.auth.service.KotopaiUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The signed-in user's dashboard. It always shows the caller's own ledger and outings; the old {@code ?userId=}
 * preview parameter was removed because it let anyone read another user's balances. The path is public in the
 * security config, so visitors who are not signed in get the welcome page instead.
 */
@Controller
public class HomeController {

    private final HomePageFacade home;

    public HomeController(HomePageFacade home) {
        this.home = home;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal KotopaiUserDetails currentUser, Model model) {
        if (currentUser == null) {
            return "landing";
        }
        model.addAttribute("home", home.forUser(currentUser.getId(), currentUser.getDisplayName()));
        return "home";
    }
}
