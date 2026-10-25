package com.nvqn.service.impl;

import com.nvqn.Repository.UserRepository;
import com.nvqn.constant.Role;
import com.nvqn.constant.UserStatus;
import com.nvqn.dto.request.LoginRequest;
import com.nvqn.dto.request.RegisterRequest;
import com.nvqn.dto.request.TokenPrincipal;
import com.nvqn.entity.User;
import com.nvqn.service.AuthenticationService;
import com.nvqn.service.TokenService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal= true)
public class AuthenticationServiceImpl implements AuthenticationService {

    TokenService tokenService;

    UserRepository userRepository;

    AuthenticationManager authenticationManager;

    PasswordEncoder passwordEncoder;

    @Override
    public String register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmailIgnoreCase(registerRequest.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if(userRepository.existsByPhone(registerRequest.phone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already exists");
        }

        User user = User.builder()
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .phone(registerRequest.phone())
                .dateOfBirth(registerRequest.dateOfBirth())
                .role(Role.CLIENT)
                .userStatus(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        return "Register Successfully!";
    }

    @Override
    public String login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.email(), loginRequest.password())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        User user = (User) authentication.getPrincipal();

        TokenPrincipal tokenPrincipal = new TokenPrincipal(user.getCode(),user.getUsername(),user.getRole().toString());

        String token = tokenService.generateAccessToken(tokenPrincipal);



        return token;
    }
}
