package com.nvqn.security_starter;

import com.nvqn.security_starter.autoConfig.SecurityAutoConfig;
import com.nvqn.security_starter.property.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigurationTests {
    @Test
    void decoderRequiresBothIssuerAndAudience() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var key = (RSAPublicKey) generator.generateKeyPair().getPublic();
        var properties = new SecurityProperties();
        var config = new SecurityAutoConfig(properties);
        assertThatThrownBy(() -> config.jwtDecoder(key))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("issuer");
        properties.setIssuer("identity-service");
        assertThatThrownBy(() -> config.jwtDecoder(key))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("audiences");
    }

    @Test
    void converterReadsSubjectRolesAndScopes() {
        var jwt = Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject("CLI-123").claim("role", List.of("client", "ROLE_ADMIN"))
                .claim("scope", "product.read product.write").build();
        var authentication = new SecurityAutoConfig(new SecurityProperties())
                .jwtAuthenticationConverter().convert(jwt);
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("CLI-123");
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_CLIENT", "ROLE_ADMIN", "SCOPE_product.read", "SCOPE_product.write");
    }

    @Test
    void converterNormalizesCommaSeparatedRoles() {
        var jwt = Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject("CLI-123").claim("role", "client, ROLE_ADMIN client").build();
        var authentication = new SecurityAutoConfig(new SecurityProperties())
                .jwtAuthenticationConverter().convert(jwt);
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_CLIENT", "ROLE_ADMIN");
    }
}
