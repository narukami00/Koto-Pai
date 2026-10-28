package com.kotopai.home;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final JdbcTemplate jdbc;

    public HomeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/")
    public String home(Model model) {
        try {
            String version = jdbc.queryForObject("select version()", String.class);
            String now = jdbc.queryForObject("select cast(current_timestamp as varchar)", String.class);
            model.addAttribute("dbUp", true);
            model.addAttribute("dbVersion", version);
            model.addAttribute("dbTime", now);
        } catch (Exception e) {
            model.addAttribute("dbUp", false);
            model.addAttribute("dbError", e.getClass().getSimpleName());
        }
        return "home";
    }
}
