package com.kotopai.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

import com.kotopai.auth.dto.RegisterRequest;
import com.kotopai.auth.service.AuthService;
import com.kotopai.common.exception.DuplicateResourceException;

/**
 * Page controller for user login and registration forms.
 */
@Controller
public class AuthPageController {

    private final AuthService authService;

    public AuthPageController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest("", "", "", ""));
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
            Model model) {
        // The confirmation only exists on this web form (the REST API takes the password once), so it is checked
        // here rather than added to RegisterRequest.
        boolean confirmMismatch = confirmPassword == null || !confirmPassword.equals(request.password());
        if (confirmMismatch) {
            model.addAttribute("confirmPasswordError", "Passwords do not match");
        }
        if (bindingResult.hasErrors() || confirmMismatch) {
            return "auth/register";
        }

        try {
            authService.register(request);
            return "redirect:/login?registered";
        } catch (DuplicateResourceException e) {
            model.addAttribute("registrationError", e.getMessage());
            return "auth/register";
        }
    }
}

