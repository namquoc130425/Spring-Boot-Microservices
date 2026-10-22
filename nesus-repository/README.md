# NVQN common platform

Các module dùng chung sử dụng Spring Boot **4.1.1** và Java **21**,
đồng bộ phiên bản Boot với `service-discovery`.

## Build và kiểm tra

Chạy từ thư mục `nesus-repository`:

```powershell
mvn clean verify
```

`compare-core` và `security-starter` được đóng gói thành JAR thư viện;
`platform-bom` và `nvqn-common-parent` được đóng gói thành POM.
Phiên bản artifact nội bộ vẫn là `1.0.0-SNAPSHOT`, độc lập với phiên bản Boot.

## Thay đổi khi chuyển từ Boot 3

- Dùng `spring-boot-starter-security-oauth2-resource-server` và
  `spring-boot-starter-webmvc` của Boot 4.
- Các handler security nhận `tools.jackson.databind.ObjectMapper` (Jackson 3).
  Service tự khai báo handler hoặc mapper cần cập nhật import tương ứng.
  Annotation JSON vẫn thuộc `com.fasterxml.jackson.annotation`.
- `DaoAuthenticationProvider` nhận `UserDetailsService` qua constructor theo
  Spring Security 7.
- Maven khai báo Lombok annotation processor tường minh để sinh builder,
  getter và constructor, kể cả khi build bằng JDK mới.
- `platform-bom` quản lý đúng artifact `com.nvqn:compare-core`.

Starter dành cho ứng dụng Servlet/Spring MVC. Cấu hình được Spring Boot tìm qua
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
Để bật cấu hình này, service phải có `app.security.enabled=true`, public key RSA,
issuer và audiences phù hợp trong `app.security.*`.

Các test kiểm tra khả năng tự nạp starter trong Boot 4.1.1, truy cập public/private,
JWT RS256 hợp lệ và token sai, JSON lỗi 401/403, cùng xác thực username/password.

Tham khảo:

- [Hướng dẫn migration Boot 4](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [DaoAuthenticationProvider trên Security 7](https://docs.spring.io/spring-security/reference/7.0/api/java/org/springframework/security/authentication/dao/DaoAuthenticationProvider.html)
