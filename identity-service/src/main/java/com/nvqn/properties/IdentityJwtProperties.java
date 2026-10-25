package com.nvqn.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")  // đọc cấu hình từ yaml
public class IdentityJwtProperties {

    private String issuer;
    private List<String> audiences = new ArrayList<>();
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private String privateKey;  // dùng để ký
    private String publicKey;   // dùng kiểm tra chử ký
}