package com.nvqn.api_gateway.controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SwaggerAggregateController {
    private final DiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.api-prefix:api}")
    private String apiPrefix;

    @GetMapping("/swagger-docs/all")
    public Mono<Map<String, Object>> getAllSwagger() {

        List<String> services = discoveryClient
                .getServices()
                .stream()
                .map(String::toLowerCase)
                .filter(service ->
                        !service.equalsIgnoreCase("gateway")
                                && !service.equalsIgnoreCase("api-gateway")
                )
                .sorted()
                .toList();

        return Flux.fromIterable(services)

                .concatMap(service ->
                        getSwagger(service)
                                .map(swagger ->
                                        Map.entry(service, swagger)
                                )
                                .onErrorResume(error -> {
                                    log.warn(
                                            "Cannot load Swagger from {}: {}",
                                            service,
                                            error.getMessage()
                                    );

                                    // một service lỗi không làm Swagger toàn hệ thống lỗi
                                    return Mono.empty();
                                })
                )

                .collectList()
                .map(this::mergeSwagger);
    }

    private Mono<Map<String, Object>> getSwagger(
            String service
    ) {

        return webClientBuilder
                .build()
                .get()
                .uri(
                        "http://"
                                + service
                                + "/v3/api-docs"
                )
                .retrieve()
                .bodyToMono(
                        new ParameterizedTypeReference<
                                Map<String, Object>
                                >() {}
                );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeSwagger(
            List<Map.Entry<String, Map<String, Object>>> documents
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        // =========================
        // OpenAPI
        // =========================
        result.put("openapi", "3.1.0");

        // =========================
        // NQT API
        // =========================
        result.put(
                "info",
                Map.of(
                        "title", "NQT API",
                        "version", "1.0.0.1"
                )
        );

        // Swagger Execute qua Gateway
        result.put(
                "servers",
                List.of(
                        Map.of(
                                "url", "/",
                                "description", "API Gateway"
                        )
                )
        );

        Map<String, Object> mergedPaths =
                new LinkedHashMap<>();

        Map<String, Object> mergedSchemas =
                new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Object>> document
                : documents) {

            String service = document.getKey();
            Map<String, Object> swagger =
                    document.getValue();

            // =========================
            // Merge Paths
            // =========================

            Object pathsObj =
                    swagger.get("paths");

            if (pathsObj instanceof Map<?, ?>) {

                Map<String, Object> paths =
                        (Map<String, Object>) pathsObj;

                paths.forEach((path, detail) -> {

                    String normalizedPath =
                            path.startsWith("/")
                                    ? path
                                    : "/" + path;

                    /*
                     * /demo
                     *
                     * thành
                     *
                     * /api/demo/demo
                     */
                    String gatewayPath =
                            "/"
                                    + normalizePrefix(apiPrefix)
                                    + "/"
                                    + service
                                    + normalizedPath;

                    // Đổi tag để phân biệt service
                    addServiceTag(
                            detail,
                            service
                    );

                    mergedPaths.put(
                            gatewayPath,
                            detail
                    );
                });
            }

            // =========================
            // Merge Schemas
            // =========================

            Object componentsObj =
                    swagger.get("components");

            if (componentsObj
                    instanceof Map<?, ?> components) {

                Object schemasObj =
                        components.get("schemas");

                if (schemasObj
                        instanceof Map<?, ?> schemas) {

                    schemas.forEach((name, schema) -> {

                        String schemaName =
                                name.toString();

                        /*
                         * Nếu hai service có DTO cùng tên,
                         * prefix service để tránh ghi đè.
                         */
                        if (mergedSchemas
                                .containsKey(schemaName)) {

                            schemaName =
                                    service
                                            .replace("-", "_")
                                            + "_"
                                            + schemaName;
                        }

                        mergedSchemas.put(
                                schemaName,
                                schema
                        );
                    });
                }
            }
        }

        result.put(
                "paths",
                mergedPaths
        );

        // =========================
        // JWT Authorization
        // =========================

        Map<String, Object> securitySchemes =
                new LinkedHashMap<>();

        securitySchemes.put(
                "bearerAuth",
                Map.of(
                        "type", "http",
                        "scheme", "bearer",
                        "bearerFormat", "JWT"
                )
        );

        Map<String, Object> components =
                new LinkedHashMap<>();

        components.put(
                "schemas",
                mergedSchemas
        );

        components.put(
                "securitySchemes",
                securitySchemes
        );

        result.put(
                "components",
                components
        );

        result.put(
                "security",
                List.of(
                        Map.of(
                                "bearerAuth",
                                List.of()
                        )
                )
        );

        return result;
    }

    @SuppressWarnings("unchecked")
    private void addServiceTag(
            Object detail,
            String service
    ) {

        if (!(detail instanceof Map<?, ?>)) {
            return;
        }

        Map<String, Object> pathItem =
                (Map<String, Object>) detail;

        List<String> methods = List.of(
                "get",
                "post",
                "put",
                "patch",
                "delete"
        );

        for (String method : methods) {

            Object operationObj =
                    pathItem.get(method);

            if (!(operationObj
                    instanceof Map<?, ?>)) {
                continue;
            }

            Map<String, Object> operation =
                    (Map<String, Object>) operationObj;

            Object tagsObj =
                    operation.get("tags");

            if (tagsObj instanceof List<?> tags
                    && !tags.isEmpty()) {

                List<String> newTags =
                        tags.stream()
                                .map(tag ->
                                        service
                                                + " / "
                                                + tag
                                )
                                .toList();

                operation.put(
                        "tags",
                        newTags
                );

            } else {

                operation.put(
                        "tags",
                        List.of(service)
                );
            }
        }
    }

    private String normalizePrefix(
            String prefix
    ) {

        return prefix
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
