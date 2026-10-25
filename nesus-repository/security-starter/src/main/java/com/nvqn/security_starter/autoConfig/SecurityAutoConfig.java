package com.nvqn.security_starter.autoConfig;



import jakarta.servlet.DispatcherType;
import tools.jackson.databind.ObjectMapper;


import com.nvqn.security_starter.property.SecurityProperties;

import com.nvqn.security_starter.security.CustomAccessDeniedHandler;
import com.nvqn.security_starter.security.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.ObjectProvider;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.springframework.core.convert.converter.Converter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;


import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;


import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

//Request tới service
//        ↓
//Bộ lọc security do Spring đăng ký
//        ↓
//Các bộ lọc trong SecurityFilterChain của bạn
//        ↓
//Nếu được phép mới tới Controller

@AutoConfiguration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(
        prefix = "app.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false

)

@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)

public class SecurityAutoConfig {
    private final SecurityProperties securityProperties;

    public SecurityAutoConfig(
            SecurityProperties securityProperties
    ) {
        this.securityProperties = securityProperties;
    }


    // =========================================================
    // SECURITY HANDLERS
    // =========================================================

    @Bean
    @ConditionalOnMissingBean
    public CustomAccessDeniedHandler customAccessDeniedHandler(
            ObjectMapper objectMapper
    ) {
        return new CustomAccessDeniedHandler(
                objectMapper
        );
    }



    @Bean
    @ConditionalOnMissingBean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(
            ObjectMapper objectMapper
    ) {
        return new CustomAuthenticationEntryPoint(
                objectMapper
        );
    }


    // =========================================================
    // HTTP SECURITY
    // =========================================================

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)   // các  bộ lọc và quy tắc bảo vệ request
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,

            Converter<Jwt, ? extends AbstractAuthenticationToken>
                    jwtAuthenticationConverter,

            CustomAuthenticationEntryPoint authenticationEntryPoint,

            CustomAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        List<String> publicEndpoints =
                securityProperties.getPublicEndpoints();
        http
                .csrf(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .formLogin(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(authenticationEntryPoint)

                        .accessDeniedHandler(accessDeniedHandler))

                .authorizeHttpRequests(auth -> auth
                         // lỗi bỏ qua trả đúng lỗi
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        //kiểm tra phải public endpoints không
                        .requestMatchers(publicEndpoints.toArray(new String[0])).permitAll()

                        // nếu privated thì kiểm tra đã hợp lệ hay chưa nếu chưa thì bị chặn và sử lý loi xác thực và nếu rồi thì vào controller
                        .anyRequest().authenticated()
                )


               /**
                *  // khi 1 request vào sẻ nhảy vào đây bước 1 - đọc token
                   .oauth2ResourceServer(...).jwt(...)
                    ↓ đăng ký bộ lọc khi khởi tạo
         Spring: BearerTokenAuthenticationFilter.doFilterInternal()
                    ↓ khi request tới
         Spring đọc Authorization và lấy token

               *  hãy bảo về Api bằng bear token dạng iwt
               *  Spring thêm bộ lọc BearTokenAuthenticationFitler . bộ lọc này mặc định đọc token từ header Authozization theo dạng bear
                **/


                .oauth2ResourceServer(oauth2 -> oauth2

                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))

                        /*
                         * Quan trọng:
                         *
                         * Error xảy ra trực tiếp trong
                         * Resource Server cũng dùng
                         * custom handlers này.
                         */
                        .authenticationEntryPoint(authenticationEntryPoint)

                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }


    @Bean
    @ConditionalOnMissingBean(RSAPublicKey.class)
    public RSAPublicKey jwtPublicKey() {            // tạo đối tượng public key dạng java

        String publicKey =
                securityProperties.getPublicKey();

        if (publicKey == null || publicKey.isBlank()) {

            throw new IllegalStateException(
                    "Missing app.security.public-key"
            );
        }

        try {

            String normalizedKey =
                    publicKey
                            .replace(
                                    "-----BEGIN PUBLIC KEY-----",
                                    ""
                            )
                            .replace(
                                    "-----END PUBLIC KEY-----",
                                    ""
                            )
                            .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(normalizedKey);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            throw new IllegalStateException("Invalid app.security.public-key", e);
        }
    }


    // =========================================================
    // JWT DECODER
    //
    // NƠI VERIFY JWT
    // =========================================================

    /**
     * bước 2
     sau khi đọc token sẻ chuyển sang decoder
     Hàm này tạo decoder có ba phần:
     - Public key và RS256 để kiểm tra chữ ký.
     - issuerValidator kiểm tra issuer cùng các điều kiện thời gian mặc định.
     - audienceValidator kiểm tra audience.
     */

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(              // đọc và kiểm tra jwt
            RSAPublicKey jwtPublicKey
    ) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtPublicKey).signatureAlgorithm(SignatureAlgorithm.RS256).build();

        String issuer = securityProperties.getIssuer();

        if (issuer == null || issuer.isBlank()) {

            throw new IllegalStateException("Missing app.security.issuer");
        }

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);

        List<String> requiredAudiences = securityProperties.getAudiences();

        if (requiredAudiences == null || requiredAudiences.isEmpty()) {
            throw new IllegalStateException("Missing app.security.audiences; audience validation is required");
        }

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            boolean accepted = token.getAudience() != null
                    && token.getAudience().stream().anyMatch(requiredAudiences::contains);
            if (accepted) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "JWT audience is not allowed for this service", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

        return decoder;
    }


    // =========================================================
    // AUTHENTICATION MANAGER
    //
    // Có thể dùng cho:
    //
    // - Username / Password
    // - JWT
    // - gRPC
    // =========================================================

    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager(

            ObjectProvider<UserDetailsService> userDetailsServiceProvider,

            PasswordEncoder passwordEncoder,

            JwtDecoder jwtDecoder,

            Converter<Jwt, ? extends AbstractAuthenticationToken>
                    jwtAuthenticationConverter
    ) {

        List<AuthenticationProvider> providers =
                new ArrayList<>();


        // =====================================================
        // USERNAME / PASSWORD
        // =====================================================

        // khi có request đăng nhập DaoAuthenticationProvider dùng passwordEncoder để băm mật khẩu gửi từ request và so sánh với
        // mật khẩu đẵ băm trong database

        UserDetailsService userDetailsService = userDetailsServiceProvider.getIfAvailable();

        if (userDetailsService != null) {

            DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider(userDetailsService);

            daoProvider.setPasswordEncoder(passwordEncoder);

            providers.add(daoProvider);
        }


        // =====================================================
        // JWT
        // =====================================================

        JwtAuthenticationProvider jwtProvider = new JwtAuthenticationProvider(jwtDecoder);

        jwtProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);

        providers.add(jwtProvider);


        return new ProviderManager(providers);
    }


    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(10);
    }


    // =========================================================
    // JWT -> AUTHENTICATION
    // =========================================================

    /**
     bước 3
     Trong hàm securityFilterChain(), tham số này được Spring truyền vào:
     Converter<Jwt, ? extends AbstractAuthenticationToken>jwtAuthenticationConverter

     đưa  jwt để jwtAuthenticationConverter -> trả cho spring
     Người gọi: CLI-123
     Quyền: ROLE_CLIENT
     JWT đã được kiểm tra
     */
    @Bean
    @ConditionalOnMissingBean(name = "jwtAuthenticationConverter")
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {  // chuyển thông jwt thành thông tin người gọi

        JwtGrantedAuthoritiesConverter scopeConverter =
                new JwtGrantedAuthoritiesConverter();


        return jwt -> {

            LinkedHashSet<GrantedAuthority> authorities =
                    new LinkedHashSet<>();


            // =================================================
            // SCOPE
            // =================================================

            Collection<GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt
            );

            if (scopeAuthorities != null) {

                authorities.addAll(scopeAuthorities);
            }


            // =================================================
            // ROLE
            // =================================================

            Object roleClaim = jwt.getClaims().get("role");

            addRoleAuthorities(authorities, roleClaim);


            // =================================================
            // PRINCIPAL
            //
            // sử dụng JWT standard claim "sub"
            // sub = user id
            // =================================================

            String principalName = jwt.getSubject();

            if (principalName == null || principalName.isBlank()) {

                throw new IllegalArgumentException("JWT must contain sub");
            }


            return new JwtAuthenticationToken(jwt, authorities, principalName);
        };
    }


    // =========================================================
    // ROLE HELPER
    // =========================================================

    private void addRoleAuthorities(
            Collection<GrantedAuthority> authorities,
            Object roleClaim
    ) {

        if (roleClaim == null) {
            return;
        }


        // =====================================================
        // role = "ADMIN USER"
        // role = "ADMIN,USER"
        // =====================================================

        if (roleClaim instanceof String roleString) {

            Arrays.stream(
                            roleString.split(
                                    "[,\\s]+"
                            )
                    )

                    .map(
                            String::trim
                    )

                    .filter(
                            role -> !role.isBlank()
                    )

                    .map(
                            this::normalizeRole
                    )

                    .map(
                            SimpleGrantedAuthority::new
                    )

                    .forEach(
                            authorities::add
                    );

            return;
        }


        // =====================================================
        // role:
        //   - ADMIN
        //   - USER
        // =====================================================

        if (roleClaim instanceof Collection<?> roles) {

            roles.stream()

                    .map(
                            String::valueOf
                    )

                    .map(
                            String::trim
                    )

                    .filter(
                            role -> !role.isBlank()
                    )

                    .map(
                            this::normalizeRole
                    )

                    .map(
                            SimpleGrantedAuthority::new
                    )

                    .forEach(
                            authorities::add
                    );
        }
    }


    private String normalizeRole(
            String role
    ) {

        String normalized =
                role.trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }

        return "ROLE_" + normalized;
    }

}
