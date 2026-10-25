package com.nvqn;

import com.nvqn.Repository.UserRepository;
import com.nvqn.constant.Role;
import com.nvqn.constant.UserStatus;
import com.nvqn.controller.TestController;
import com.nvqn.dto.request.TokenPrincipal;
import com.nvqn.entity.User;
import com.nvqn.service.TokenService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.config.location=classpath:/application-test.yaml")
@ActiveProfiles("test")
class IdentityServiceApplicationTests {
    private static final KeyPair KEYS = generateKeys();
    private static final String EMAIL = "client@example.test";
    private static final String PASSWORD = "Test-Only-123!";

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired TokenService tokenService;
    private MockMvc mvc;

    @DynamicPropertySource
    static void configureKeys(DynamicPropertyRegistry properties) {
        properties.add("app.security.public-key",
                () -> Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
        properties.add("app.security.private-key",
                () -> Base64.getEncoder().encodeToString(KEYS.getPrivate().getEncoded()));
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        users.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @Test
    void registerPersistsDateAndHashedPasswordWithoutAcceptingClientSuppliedRole() throws Exception {
        register(EMAIL, "0901234567");

        User saved = users.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode()).startsWith("CLI-");
        assertThat(saved.getRole()).isEqualTo(Role.CLIENT);
        assertThat(saved.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDateTime.of(2000, 5, 20, 0, 0));
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPassword())).isTrue();
    }

    @Test
    void registerThenLoginIssuesVerifiableRs256TokenWithProductAudience() throws Exception {
        register(EMAIL, "0901234567");

        String token = mvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson("CLIENT@example.test", PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        var jwt = jwtDecoder.decode(token);
        User saved = users.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("identity-service");
        assertThat(jwt.getSubject()).isEqualTo(saved.getCode());
        assertThat(jwt.getAudience()).containsExactly("product-service");
        assertThat(jwt.getClaimAsStringList("role")).containsExactly("CLIENT");
        assertThat(jwt.getClaimAsString("username")).isEqualTo(EMAIL);
        assertThat(jwt.getId()).isNotBlank();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void missingDateOfBirthReturnsBadRequestWithoutSavingUser() throws Exception {
        mvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content("""
                {"phone":"0901234567","email":"client@example.test","password":"Test-Only-123!"}
                """))
                .andExpect(status().isBadRequest());
        assertThat(users.count()).isZero();
    }

    @Test
    void malformedDateReturnsBadRequest() throws Exception {
        mvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content("""
                {"phone":"0901234567","email":"client@example.test",
                 "password":"Test-Only-123!","dateOfBirth":"20/05/2000"}
                """))
                .andExpect(status().isBadRequest());
        assertThat(users.count()).isZero();
    }

    @Test
    void blankRegistrationFieldsReturnBadRequest() throws Exception {
        mvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content("""
                {"phone":" ","email":" ","password":" ","dateOfBirth":"2000-05-20T00:00:00"}
                """))
                .andExpect(status().isBadRequest());
        assertThat(users.count()).isZero();
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        register(EMAIL, "0901234567");
        mvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content(registrationJson("CLIENT@example.test", "0907654321")))
                .andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void duplicatePhoneReturnsConflict() throws Exception {
        register(EMAIL, "0901234567");
        mvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content(registrationJson("other@example.test", "0901234567")))
                .andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void wrongPasswordAndUnknownUserReturnUnauthorized() throws Exception {
        register(EMAIL, "0901234567");
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content(loginJson(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content(loginJson("absent@example.test", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"INACTIVE", "BLOCKED"})
    void disabledAccountsCannotLogin(UserStatus accountStatus) throws Exception {
        register(EMAIL, "0901234567");
        User saved = users.findByEmailIgnoreCase(EMAIL).orElseThrow();
        saved.setUserStatus(accountStatus);
        users.saveAndFlush(saved);

        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenMintingTestControllerIsAbsentUnlessExplicitlyEnabled() throws Exception {
        assertThat(context.getBeansOfType(TestController.class)).isEmpty();
        String token = tokenService.generateAccessToken(new TokenPrincipal("CLI-test", EMAIL, "CLIENT"));
        mvc.perform(post("/test/access-token").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"userCode":"CLI-test","username":"client@example.test","role":"ADMIN"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiIsPublicAndDescribesLoginAndRegister() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("/auth/login", "/auth/register").doesNotContain("/test/access-token");
    }

    private void register(String email, String phone) throws Exception {
        mvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content(registrationJson(email, phone)))
                .andExpect(status().isOk());
    }

    private static String registrationJson(String email, String phone) {
        return """
                {"phone":"%s","email":"%s","password":"%s",
                 "dateOfBirth":"2000-05-20T00:00:00","role":"ADMIN"}
                """.formatted(phone, email, PASSWORD);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private static KeyPair generateKeys() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot generate test RSA keys", exception);
        }
    }
}
