package com.kotopai.outing.controller;

import com.kotopai.auth.service.KotopaiUserDetails;
import com.kotopai.outing.domain.IllegalOutingTransitionException;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.service.OutingOperationException;
import com.kotopai.outing.service.OutingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered outing pages: the list, one outing with its split, and the submit / cancel / delete buttons.
 * Every rule lives in {@link OutingService}; this class only turns its refusals into friendly messages.
 */
@Controller
public class OutingPageController {

    private final OutingPageFacade pages;
    private final OutingService outings;

    public OutingPageController(OutingPageFacade pages, OutingService outings) {
        this.pages = pages;
        this.outings = outings;
    }

    @GetMapping("/outings")
    public String list(@AuthenticationPrincipal KotopaiUserDetails currentUser, Model model) {
        model.addAttribute("outingList", pages.list(currentUser.getId()));
        return "outings/list";
    }

    @GetMapping("/outings/{id:\\d+}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal KotopaiUserDetails currentUser,
                         Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("outing", pages.detail(currentUser.getId(), id));
            return "outings/detail";
        } catch (OutingOperationException e) {
            redirect.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/outings";
        }
    }

    @PostMapping("/outings/{id:\\d+}/submit")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal KotopaiUserDetails currentUser,
                         RedirectAttributes redirect) {
        return act(id, redirect, "Outing submitted. Everyone on it can now see their share.",
            () -> outings.submit(currentUser.getId(), id));
    }

    @PostMapping("/outings/{id:\\d+}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal KotopaiUserDetails currentUser,
                         RedirectAttributes redirect) {
        return act(id, redirect, "Outing cancelled. Its debts were voided.",
            () -> outings.cancel(currentUser.getId(), id));
    }

    @PostMapping("/outings/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal KotopaiUserDetails currentUser,
                         RedirectAttributes redirect) {
        try {
            outings.delete(currentUser.getId(), id);
            redirect.addFlashAttribute("flashSuccess", "Draft deleted.");
            return "redirect:/outings";
        } catch (OutingOperationException | IllegalOutingTransitionException e) {
            redirect.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/outings/" + id;
        }
    }

    /** Runs a state-changing action and reports the outcome; the page then shows the new status. */
    private String act(Long id, RedirectAttributes redirect, String success, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("flashSuccess", success);
        } catch (EngineValidationException | OutingOperationException | IllegalOutingTransitionException e) {
            redirect.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/outings/" + id;
    }
}
