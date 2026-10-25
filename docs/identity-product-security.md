# Identity and product security

Identity registers accounts with BCrypt password hashes and issues RS256 access tokens after email/password login. New accounts receive the CLIENT role. BLOCKED and INACTIVE accounts cannot log in. Accounts use the `app_users` table to avoid PostgreSQL's `USER` keyword.

Product provides authenticated CRUD at `/products` and `/products/{id}`. The demo requires a valid token for every product operation; it deliberately does not restrict writes to ADMIN.

## Configuration

Services import configuration from Config Server at `http://localhost:5555`, using their Spring application names. Identity normally listens on 8888 and product on 5054, as configured in the external repository.

Both services use `security-starter`. Set `app.security.enabled=true`, issuer `identity-service`, audience `product-service`, and the corresponding RSA public key. Only identity receives the matching private key. Keep actual keys and database credentials outside this repository.

The `/test/access-token` controller is disabled unless `app.security.test-token.enabled=true` is explicitly set. Keep it disabled outside isolated local experiments. Use `/auth/login` to obtain normal tokens.

## Try the flow

1. POST `/auth/register` with phone, email, password and `dateOfBirth` such as `2000-05-20T00:00:00`.
2. POST `/auth/login` with email and password; the response is the JWT string.
3. Call product with `Authorization: Bearer <token>`.

Login and registration must be public in identity's security configuration. Duplicate email/phone returns 409; invalid registration fields return 400; invalid credentials or JWTs return 401. Unknown products return 404.

Swagger through Gateway adds Bearer authentication centrally using `/swagger-docs/all` or `/swagger-docs/{serviceName}`. Configuring the Swagger document does not itself enforce security: each service validates JWTs through the starter.

## Tests and CI

Install the shared modules first, then verify applications:

```shell
mvn -f nesus-repository/pom.xml -pl security-starter -am clean install
mvn -f identity-service/pom.xml clean verify
mvn -f product-service/pom.xml clean verify
```

Identity/product tests use H2 in PostgreSQL compatibility mode and fresh RSA keys generated in memory. They disable Config Server and Eureka and do not access development databases or real signing keys.

Identity tests cover registration persistence, password hashing, date validation, duplicate accounts, case-insensitive login, JWT claims, blocked/inactive users, and the disabled test-token endpoint. Product tests exercise the actual security filter chain, invalid JWT signatures/issuer/audience/time claims, authenticated CRUD and invalid products. Shared starter tests verify automatic configuration, authority conversion and ERROR dispatch behavior. Config Server tests use MockMvc with the native test repository, avoiding a real HTTP listening port.

GitHub Actions verifies all seven service/platform projects. Consumer jobs install the shared modules from this checkout before building, so CI does not require a developer's local Nexus. Pushes to `feature/**`, `codex/**` and `main`, and pull requests to `main`, trigger the workflow.
