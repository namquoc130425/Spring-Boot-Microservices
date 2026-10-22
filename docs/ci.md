# Build và test trong CI

Workflow `.github/workflows/ci.yml` chạy khi mở/cập nhật PR vào `main`, khi push
vào `main`, hoặc được chạy thủ công. Mỗi thư mục có một job Java 21 riêng:

- `service-discovery`
- `config-service`
- `user-service`
- `api-gateway`
- `nesus-repository` (build cả ba module trong reactor)

Không có POM tổng ở thư mục gốc. CI chạy `clean verify` từ từng thư mục,
dùng `bash mvnw` để không phụ thuộc executable bit trên Linux. Reactor thư viện
dùng wrapper tại `security-starter/mvnw` từ thư mục `nesus-repository`.

Check tổng hợp **Build & Test** chỉ pass khi tất cả các job build pass; job thất
bại, bị hủy hoặc bị bỏ qua không được coi là thành công. Giữ tên check này để
tương thích với tên check cũ. Nếu repository yêu cầu check bắt buộc, chọn
**Build & Test** trong branch protection/ruleset.

CI upload Surefire reports khi một job thất bại. `verify` không deploy lên Nexus
và không cần mật khẩu Nexus.

## Chạy lại trên máy

Từ một thư mục service, với JDK 21:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Trên Linux:

```bash
bash mvnw -B -ntp clean verify
```

Với thư viện, từ `nesus-repository`:

```powershell
.\security-starter\mvnw.cmd -B -ntp clean verify
```

```bash
bash security-starter/mvnw -B -ntp clean verify
```

## Phạm vi test

- Discovery kiểm tra Spring context khởi tạo được.
- User và Gateway là smoke test, dùng profile `test` và nạp riêng
  `application-test.yaml` bằng `spring.config.location`. Không import cấu hình
  runtime từ `localhost:5555`; không đăng ký Eureka. User dùng H2 trong bộ nhớ,
  không cần PostgreSQL đang chạy. Chưa kiểm tra nghiệp vụ demo hay route thực.
- Config Server chạy HTTP trên cổng ngẫu nhiên với backend `native`; test xác
  nhận HTTP 200 và giá trị cấu hình từ fixture trong `src/test/resources`.
  Không cần clone Git hay cung cấp GitHub token.
- Thư viện kiểm tra startup, auto-configuration, JWT RS256, JSON lỗi 401/403 và
  xác thực username/password.

Nếu Windows báo `Unable to establish loopback connection` / `Invalid argument:
connect` khi mở NIO selector, kiểm tra môi trường JDK/Windows hoặc chạy test
trong Linux/Java 21 như CI. Không bỏ qua test HTTP để che lỗi môi trường này.
