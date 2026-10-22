package com.nvqn.security_starter.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")

public class SecurityProperties {
    private boolean enabled = false;
    private String publicKey; //có 1 service xứ lý tạo ra 1 cái mã rồi service khác dùng public key này đễ verify cái mã đó (đúng thì là service của mình sai thì hacker tấn công)
    private String issuer;
    private List<String> audiences = new ArrayList<>();
    private List<String> publicEndpoints = new ArrayList<>();
}
