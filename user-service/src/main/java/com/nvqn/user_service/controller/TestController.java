package com.nvqn.user_service.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class TestController {

    @GetMapping
    public String getUsers() {
        return "User Service";
    }
}
