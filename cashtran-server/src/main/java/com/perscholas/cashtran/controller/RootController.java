package com.perscholas.cashtran.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public String index() {
        return "Cashtran API is running. See /swagger-ui/ for API docs.";
    }
}