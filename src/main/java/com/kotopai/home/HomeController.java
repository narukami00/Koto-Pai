package com.kotopai.home;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kotopai.ledger.service.LedgerService;
import com.kotopai.ledger.service.LedgerSummary;

@Controller
public class HomeController {

    private final JdbcTemplate jdbc;
    private final LedgerService ledger;

    public HomeController(JdbcTemplate jdbc, LedgerService ledger) {
        this.jdbc = jdbc;
        this.ledger = ledger;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) Long userId, Model model) {
        model.addAttribute("ledger", userId == null ? LedgerSummary.empty(0) : ledger.summaryFor(userId));
        model.addAttribute("ledgerUserId", userId);
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
