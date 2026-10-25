package com.nvqn.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nvqn.properties.IdentityJwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
@Configuration
public class JwtSighingConfig {  // lấy  cấu hình từ IdentityJwtProperties

    // private key và public key đưa cho JwtEncoder được TokenSeviceImpl sử dụng

    @Bean
    public JwtEncoder jwtEncoder(IdentityJwtProperties jwtProperties) throws Exception {  // hãy tạo 1 đối tượng và đưa đối tượng đó cho 1 class cần dùng .

        RSAPrivateKey privateKey = parsePrivateKey(jwtProperties.getPrivateKey()); // từ văn bản -> đối tượng khóa java mà thư viện ký  sử dụng đc

        RSAPublicKey publicKey = parsePublicKey(jwtProperties.getPublicKey()); // từ văn bản -> đối tượng khóa java  mà thư viện ký java sử dụng đc

        RSAKey rsaKey = new RSAKey.Builder(publicKey)  // gom cặp khóa và thông tin cách ký lại thành 1 đối tượng
                .privateKey(privateKey)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("identity-rs256-v1")
                .build();

        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));  // ra đc 1 đối tượng đã đượcc chuẩn bị sẵn khóa để kí thì TokenService sẻ dùng cái này để tạo ra được token
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    private RSAPrivateKey parsePrivateKey(String pem)
            throws Exception {

        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(base64);

        PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(decoded);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private RSAPublicKey parsePublicKey(String pem)
            throws Exception {

        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(base64);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);

        return (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(keySpec);
    }
}
