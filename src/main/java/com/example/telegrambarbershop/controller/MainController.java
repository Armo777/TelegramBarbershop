package com.example.telegrambarbershop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/about.html")
    public String about() {
        return "about";
    }

    @GetMapping("/contacts.html")
    public String contacts() {
        return "contacts";
    }

    @GetMapping("/portfolio.html")
    public String portfolio() {
        return "portfolio";
    }

    @GetMapping("/reviews.html")
    public String reviews() {
        return "reviews";
    }

    @GetMapping("/services.html")
    public String services() {
        return "services";
    }
}
