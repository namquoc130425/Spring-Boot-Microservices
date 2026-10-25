package com.nvqn.security_starter.security;


import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;

// class này Chuyển sub, role, scope thành Authentication
public class SecurityJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
// nhận vào jwt và trả về AbstractAuthenticationToken
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>(); // tạo danh sách phân quyền -> dùng linkHashSet để tránh trùng lập

        Collection<GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt); // nhờ class spring  đọc các scope từ jwt
        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);   // add scope
        }

        addRoleAuthorities(authorities, jwt.getClaim("role")); // lấy clam tên role từ jwt
                                                               // đưa role vào phương thức addRoleAuthories
                                                               // chuyển nó thành GrantedAuthority
                                                               // thêm vào danh sách phân quyền

        String principalName = jwt.getSubject();
        if (principalName == null || principalName.isBlank()) {
            throw new IllegalArgumentException("JWT must contain a non-blank sub claim");
        }

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    @Override
    public <U> Converter<Jwt, U> andThen(Converter<? super AbstractAuthenticationToken, ? extends U> after) {
        return Converter.super.andThen(after);
    }

    private void addRoleAuthorities(
            Collection<GrantedAuthority> authorities,
            Object roleClaim
    ) {
        if (roleClaim instanceof String roleString) {
            Arrays.stream(roleString.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .map(this::normalizeRole)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            return;
        }

        if (roleClaim instanceof Collection<?> roles) {
            roles.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .map(this::normalizeRole)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_")
                ? normalized
                : "ROLE_" + normalized;
    }
}
