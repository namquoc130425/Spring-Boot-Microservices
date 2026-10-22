package com.nvqn.api_gateway.controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@Slf4j
public class SwaggerProxyController {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.api-prefix:api}")
    private String apiPrefix;

    @GetMapping("/swagger-docs/{serviceName}")
    public Mono<Map<String, Object>> getSwagger(
            @PathVariable String serviceName
    ) {

        String service = serviceName.toLowerCase();

        return webClientBuilder
                .build()
                .get()
                .uri("http://" + service + "/v3/api-docs")
                .retrieve()
                .bodyToMono(
                        new ParameterizedTypeReference<
                                Map<String, Object>
                                >() {}
                )
                .map(swagger ->
                        configureGatewayServer(swagger, service)
                );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> configureGatewayServer(
            Map<String, Object> swagger,
            String service
    ) {

        // =========================
        // 1. Execute qua Gateway
        // =========================
        swagger.put(
                "servers",
                List.of(
                        Map.of(
                                "url",
                                "/" + apiPrefix + "/" + service,
                                "description",
                                "API Gateway"
                        )
                )
        );

        // =========================
        // 2. Title + Version
        // =========================
        Map<String, Object> info;

        Object infoObj = swagger.get("info");

        if (infoObj instanceof Map<?, ?>) {
            info = new LinkedHashMap<>(
                    (Map<String, Object>) infoObj
            );
        } else {
            info = new LinkedHashMap<>();
        }

        info.put("title", "NQT API");
        info.put("version", "1.0.0.1");

        swagger.put("info", info);


        // =========================
        // 3. JWT Bearer Authentication
        // =========================

        Map<String, Object> securityScheme = new LinkedHashMap<>();

        securityScheme.put("type", "http");
        securityScheme.put("scheme", "bearer");
        securityScheme.put("bearerFormat", "JWT");


        Map<String, Object> securitySchemes = new LinkedHashMap<>();

        securitySchemes.put(
                "bearerAuth",
                securityScheme
        );


        Map<String, Object> components;

        Object componentsObj = swagger.get("components");

        if (componentsObj instanceof Map<?, ?>) {

            components = new LinkedHashMap<>(
                    (Map<String, Object>) componentsObj
            );

        } else {

            components = new LinkedHashMap<>();
        }


        components.put(
                "securitySchemes",
                securitySchemes
        );

        swagger.put(
                "components",
                components
        );


        // =========================
        // 4. Áp dụng JWT cho API
        // =========================

        swagger.put(
                "security",
                List.of(
                        Map.of(
                                "bearerAuth",
                                List.of()
                        )
                )
        );


        return swagger;
    }
}