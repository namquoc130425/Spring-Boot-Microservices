package com.nvqn.security_starter;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nvqn.security_starter.autoConfig.SecurityAutoConfig;
import com.nvqn.security_starter.security.CustomAccessDeniedHandler;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityStarterApplicationTests {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(TestApplication.class);

    @Test
    void customSecurityIsDisabledUnlessEnabled() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SecurityAutoConfig.class);
        });
    }

    @Test
    void bootDiscoversStarterAndAuthenticatesSignedJwt() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
                .subject("student-123")
                .issuer("https://auth.example.test")
                .audience("user-service")
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("scope", "student.read")
                .claim("role", "USER")
                .build());
        jwt.sign(new RSASSASigner(keys.getPrivate()));

        runner.withPropertyValues(
                "app.security.enabled=true",
                "app.security.public-key=" + Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()),
                "app.security.issuer=https://auth.example.test",
                "app.security.audiences[0]=user-service",
                "app.security.public-endpoints[0]=/public"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SecurityAutoConfig.class);
            var mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();

            mvc.perform(get("/public")).andExpect(status().isOk());
            var unauthenticated = mvc.perform(get("/private")).andExpect(status().isUnauthorized())
                    .andReturn().getResponse();
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            var error = mapper.readTree(unauthenticated.getContentAsString());
            assertThat(error.get("code").asInt()).isEqualTo(401);
            assertThat(error.get("success").asBoolean()).isFalse();
            assertThat(error.has("result")).isFalse();
            mvc.perform(get("/private").header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/private").header("Authorization", "Bearer " + jwt.serialize()))
                    .andExpect(status().isOk()).andExpect(content().string("student-123"));

            var response = new MockHttpServletResponse();
            context.getBean(CustomAccessDeniedHandler.class).handle(new MockHttpServletRequest(), response,
                    new AccessDeniedException("denied"));
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(mapper.readTree(response.getContentAsString()).get("code").asInt()).isEqualTo(403);
        });
    }

    @Test
    void authenticationManagerSupportsUserDetailsServiceOnSecurity7() {
        var properties = new com.nvqn.security_starter.property.SecurityProperties();
        var config = new SecurityAutoConfig(properties);
        var encoder = new BCryptPasswordEncoder();
        UserDetailsService users = new InMemoryUserDetailsManager(User.withUsername("student")
                .password(encoder.encode("test-password")).roles("USER").build());
        var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        beans.registerSingleton("users", users);
        AuthenticationManager manager = config.authenticationManager(beans.getBeanProvider(UserDetailsService.class),
                encoder, token -> { throw new AssertionError("Password login should not decode JWT"); },
                config.jwtAuthenticationConverter());

        var authentication = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("student", "test-password"));
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("student");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/public")
        String publicEndpoint() {
            return "public";
        }

        @GetMapping("/private")
        String privateEndpoint(java.security.Principal principal) {
            return principal.getName();
        }
    }
}
