package com.nvqn.product_service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nvqn.product_service.repository.ProductRepository;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.location=classpath:/application-test.yaml")
@ActiveProfiles("test")
class ProductServiceApplicationTests {
    private static final KeyPair KEYS = generateKeys();
    @Autowired WebApplicationContext context;
    @Autowired ProductRepository products;
    private MockMvc mvc;

    @DynamicPropertySource
    static void configureKey(DynamicPropertyRegistry properties) {
        properties.add("app.security.public-key",
                () -> Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
    }

    @BeforeEach
    void setUp() {
        products.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @Test
    void productApiRequiresToken() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("code").value(401))
                .andExpect(jsonPath("success").value(false));
        mvc.perform(post("/products").contentType(APPLICATION_JSON).content(productJson("Demo", 10, 1)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/products").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-issuer", "wrong-audience", "missing-audience", "wrong-key", "expired", "future"})
    void rejectsInvalidJwtClaimsAndSignatures(String kind) throws Exception {
        mvc.perform(get("/products").header("Authorization", "Bearer " + token(kind)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientJwtCanCreateReadUpdateAndDeleteProductsForDemo() throws Exception {
        String bearer = "Bearer " + token("valid");
        mvc.perform(post("/products").header("Authorization", bearer)
                        .contentType(APPLICATION_JSON).content(productJson("  Demo  ", 25, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("name").value("Demo"))
                .andExpect(jsonPath("quantity").value(2));

        var saved = products.findAll().getFirst();
        UUID id = saved.getId();
        assertThat(saved.getCreatedAt()).isNotNull();

        mvc.perform(get("/products").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Demo"));
        mvc.perform(get("/products/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("price").value(25));
        mvc.perform(put("/products/{id}", id).header("Authorization", bearer)
                        .contentType(APPLICATION_JSON).content(productJson("Updated", 30, 5)))
                .andExpect(status().isOk()).andExpect(jsonPath("name").value("Updated"))
                .andExpect(jsonPath("quantity").value(5));
        assertThat(products.findById(id).orElseThrow().getName()).isEqualTo("Updated");
        mvc.perform(delete("/products/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNoContent());
        assertThat(products.count()).isZero();
        mvc.perform(get("/products/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\" \",\"price\":10,\"quantity\":1,\"status\":\"AVAILABLE\"}",
            "{\"name\":\"Demo\",\"price\":-1,\"quantity\":1,\"status\":\"AVAILABLE\"}",
            "{\"name\":\"Demo\",\"price\":10,\"quantity\":-1,\"status\":\"AVAILABLE\"}",
            "{\"name\":\"Demo\",\"price\":10,\"quantity\":1}"
    })
    void invalidProductReturnsBadRequestWithoutPersisting(String body) throws Exception {
        mvc.perform(post("/products").header("Authorization", "Bearer " + token("valid"))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(products.count()).isZero();
    }

    @Test
    void openApiIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("paths['/products']").exists());
    }

    private static String token(String kind) throws Exception {
        Instant now = Instant.now();
        Instant issuedAt = kind.equals("expired") ? now.minusSeconds(7200) : now.minusSeconds(5);
        Instant expiresAt = kind.equals("expired") ? now.minusSeconds(3600) : now.plusSeconds(900);
        Instant notBefore = kind.equals("future") ? now.plusSeconds(300) : issuedAt;
        var claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject("CLI-demo")
                .issuer(kind.equals("wrong-issuer") ? "other-issuer" : "identity-service")
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiresAt))
                .claim("role", List.of("CLIENT"));
        if (!kind.equals("missing-audience")) {
            claims.audience(kind.equals("wrong-audience") ? "other-service" : "product-service");
        }
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner(kind.equals("wrong-key") ? generateKeys().getPrivate() : KEYS.getPrivate()));
        return jwt.serialize();
    }

    private static String productJson(String name, int price, int quantity) {
        return """
                {"name":"%s","description":" Demo description ","price":%d,"quantity":%d,"status":"AVAILABLE"}
                """.formatted(name, price, quantity);
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
