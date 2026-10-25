package com.nvqn.controller;

import com.nvqn.dto.request.TokenPrincipal;
import com.nvqn.service.TokenService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(
        name = "app.security.test-token.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class TestController {

    TokenService tokenService;


    @PostMapping("/access-token")
    public ResponseEntity<String> generateAccessToken(@RequestBody @Valid TokenPrincipal principal) {
        return ResponseEntity.ok(tokenService.generateAccessToken(principal));
    }


}