package com.kotopai.outing.controller;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.outing.service.OutingAccessPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Serves the new-outing wizard shell. The wizard itself is a small script (static/js/outing-wizard.js) that talks to
 * the JSON API, so this controller only checks that the user may edit before showing an existing outing.
 */
@Controller
public class OutingWizardController {

    private final OutingAccessPolicy access;

    public OutingWizardController(OutingAccessPolicy access) {
        this.access = access;
    }

    @GetMapping("/outings/new")
    public String create(@AuthenticationPrincipal KotopaiUserDetails currentUser, Model model) {
        fill(model, currentUser, null);
        return "outings/wizard";
    }

    @GetMapping("/outings/{id:\\d+}/edit")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal KotopaiUserDetails currentUser,
                       Model model, RedirectAttributes redirect) {
        if (!access.canEdit(id, currentUser.getId())) {
            redirect.addFlashAttribute("flashError", "You cannot edit this outing.");
            return "redirect:/outings";
        }
        fill(model, currentUser, id);
        return "outings/wizard";
    }

    private static void fill(Model model, KotopaiUserDetails currentUser, Long outingId) {
        model.addAttribute("pageTitle", outingId == null ? "New outing" : "Edit outing");
        model.addAttribute("outingId", outingId);
        model.addAttribute("meId", currentUser.getId());
        model.addAttribute("meName", currentUser.getDisplayName());
    }
}
