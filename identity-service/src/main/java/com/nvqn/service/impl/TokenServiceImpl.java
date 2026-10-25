package com.nvqn.service.impl;

import com.nvqn.dto.request.TokenPrincipal;
import com.nvqn.properties.IdentityJwtProperties;
import com.nvqn.service.TokenService;
import org.springframework.stereotype.Service;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class TokenServiceImpl implements TokenService {

    JwtEncoder jwtEncoder;
    Clock clock;
    String issuer;
    List<String> audiences;
    Duration accessTokenTtl;

    public TokenServiceImpl(
            JwtEncoder jwtEncoder,
            Clock clock,
            IdentityJwtProperties jwtProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = jwtProperties.getIssuer();
        this.audiences = List.copyOf(jwtProperties.getAudiences());
        this.accessTokenTtl = jwtProperties.getAccessTokenTtl();
    }

    @Override
    public String generateAccessToken(TokenPrincipal principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        if (principal.userCode() == null || principal.userCode().isBlank()) {
            throw new IllegalArgumentException("userCode must not be blank");
        }
        if (audiences.isEmpty()) {
            throw new IllegalStateException("JWT audiences must not be empty");
        }

        Instant now = clock.instant();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(principal.userCode())
                .audience(audiences)
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(now.plus(accessTokenTtl))
                .claim("userCode", principal.userCode())
                .claim("username", principal.username())
                .claim("role", List.of(principal.role()))
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .keyId("identity-rs256-v1")
                .type("JWT")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
