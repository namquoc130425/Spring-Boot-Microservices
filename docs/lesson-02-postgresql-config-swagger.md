# Ghi chú ôn tập: PostgreSQL, Config Service và Swagger

> Mục tiêu của ghi chú này là giúp đọc lại nhanh sau một thời gian: biết hệ thống đang có gì, mỗi phần nằm ở đâu và request đi theo đường nào. Đây không phải tài liệu lý thuyết đầy đủ.

## Trạng thái Git khi viết tài liệu

| Phần | Trạng thái |
| --- | --- |
| Nhánh đang làm | `learning/lesson-02` |
| Commit bài trước | `8bfd19d - connected Db` |
| Remote | Đã push lên `origin/learning/lesson-02` |
| PR bài trước | [Tạo PR trên GitHub](https://github.com/namquoc130425/Spring-Boot-Microservices/pull/new/learning/lesson-02) |
| Bài hôm nay | Chưa commit |

Lưu ý quan trọng: commit `8bfd19d` mới tạo bộ khung `Student`, repository và service. Phần triển khai JPA/PostgreSQL đầy đủ, Config Service và Swagger đang cùng nằm trong working tree chưa commit. Khi chuẩn bị commit tiếp, nên tách chúng thành các commit nhỏ thay vì gom tất cả vào một commit.

---

# 1. Bài kết nối PostgreSQL

## Mình đã thêm gì?

- Docker Compose chạy PostgreSQL (image có sẵn `pgvector` trên PostgreSQL 15).
- File `init.sql` tạo database `student_management` ngay lần khởi tạo volume đầu tiên.
- File `.env` đặt thông tin kết nối cho Docker Compose.
- `user-service` thêm PostgreSQL driver, Spring Data JPA và Lombok.
- Model `Student`, `StudentRepository`, `StudentService` và endpoint đọc danh sách học viên.

Các file chính:

- `docker-compose.yml`
- `init.sql`
- `.env` — chỉ dùng ở máy local, không đưa mật khẩu thật lên Git public.
- `user-service/pom.xml`
- `user-service/src/main/java/com/nvqn/user_service/entity/Student.java`
- `user-service/src/main/java/com/nvqn/user_service/repository/StudentRepository.java`
- `user-service/src/main/java/com/nvqn/user_service/service/implement/StudentService.java`
- `user-service/src/main/java/com/nvqn/user_service/controller/TestController.java`

## Các dependency cần hiểu

| Dependency | Vai trò dễ nhớ |
| --- | --- |
| `org.postgresql:postgresql` | Driver để Java nói chuyện với PostgreSQL. |
| `spring-boot-starter-data-jpa` | Cho phép khai báo entity, repository và để JPA sinh câu SQL cơ bản. |
| `org.projectlombok:lombok` | Sinh getter/setter, constructor qua annotation để bớt mã lặp. |
| `spring-boot-starter-actuator` | Cung cấp endpoint theo dõi sức khỏe/metrics; hữu ích khi có nhiều service. |
| `spring-boot-devtools` | Tự reload khi phát triển local, không phải dependency nghiệp vụ. |

## Phần cấu hình local

`docker-compose.yml` đọc mật khẩu từ `.env`. Khối dưới đây là mẫu để nhớ cấu trúc, **không dùng mật khẩu production ở đây**.

```yaml
# .env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=student_management
DB_USERNAME=postgres
DB_PASSWORD=<mat-khau-local>
```

```yaml
# docker-compose.yml (ý chính)
services:
  postgres:
    image: pgvector/pgvector:pg15
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres-data-v2:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
```

```sql
-- init.sql
CREATE DATABASE student_management;
```

Điểm dễ quên: script trong `docker-entrypoint-initdb.d` chỉ chạy khi volume database còn mới. Nếu volume `postgres-data-v2` đã tồn tại thì sửa `init.sql` rồi chạy lại container sẽ **không** tự chạy script lại.

## Luồng đọc `GET /student`

```text
Client
  -> TestController: GET /student
  -> IStudent / StudentService.findAll()
  -> StudentRepository.findAll()
  -> Spring Data JPA tạo SQL
  -> PostgreSQL: bảng Student
  -> List<Student> trả về client
```

### Vai trò từng lớp

- `Student` là entity: một object Java đại diện cho dữ liệu học viên. `@Entity` báo cho JPA đây là dữ liệu cần map xuống database; `UUID id`, `name`, `age` là các field hiện có.
- `StudentRepository extends JpaRepository<Student, UUID>`: nhận sẵn các thao tác như `findAll`, `findById`, `save`, `deleteById`, nên chưa cần tự viết SQL cho các thao tác này.
- `StudentService`: nằm giữa controller và repository, để phần nghiệp vụ có chỗ đặt khi chương trình lớn dần.
- `TestController`: nhận HTTP request, gọi service và trả `ResponseEntity<List<Student>>`.

## Cần có cấu hình datasource ở đâu?

Sau bài Config Service, cấu hình datasource không còn nhất thiết nằm trong `user-service/src/main/resources/application.yaml`; nó nên nằm trong repository cấu hình bên ngoài. Mẫu tối thiểu để `user-service` kết nối PostgreSQL là:

```yaml
# user-service.yml trong config repository (mẫu)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/student_management
    username: postgres
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

`ddl-auto: update` tiện cho lúc học vì Hibernate tự tạo/chỉnh schema theo entity. Khi làm dự án thật, dùng migration (Flyway/Liquibase) sẽ an toàn và dễ kiểm soát hơn.

---

# 2. Bài hôm nay: Config Service + Swagger

## Mục tiêu của bài

Không để mọi service đều giữ bản sao cấu hình port, Eureka, Gateway route, datasource… trong chính source code. Thay vào đó:

```text
Git config repository
          |
          v
Config Service :5555
       |                 |
       v                 v
user-service         api-gateway
```

Mỗi service khi khởi động lấy file cấu hình theo tên ứng dụng. Ví dụ `spring.application.name: user-service` sẽ làm Config Client tìm cấu hình dành cho `user-service`.

## Config Service đã được tạo như thế nào?

Module mới là `config-service`. Điểm bật chức năng nằm tại:

`config-service/src/main/java/com/nvqn/config_service/ConfigServiceApplication.java`

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
```

`@EnableConfigServer` biến ứng dụng Spring Boot bình thường thành Spring Cloud Config Server: nó đọc cấu hình từ Git backend và trả cấu hình đó qua HTTP cho các service khác.

## Dependency của Config Service

| Dependency | Dùng để làm gì |
| --- | --- |
| `spring-cloud-config-server` | Cung cấp Config Server. Đây là dependency cốt lõi. |
| `org.eclipse.jgit:jgit` | Giúp Config Server clone/pull repository cấu hình Git. |
| `spring-boot-starter-actuator` | Kiểm tra health và các endpoint quản trị. |
| `spring-boot-devtools` | Hỗ trợ phát triển local. |
| `spring-cloud-dependencies` | BOM đồng bộ version các dependency Spring Cloud. |

## Cấu hình Config Service

Config Service chạy cổng `5555` và đọc từ repository Git cấu hình.

```yaml
# config-service/src/main/resources/application.yaml
server:
  port: 5555

spring:
  application:
    name: config-service
  cloud:
    config:
      server:
        git:
          uri: https://github.com/<owner>/<config-repository>.git
          default-label: main
          username: ${CONFIG_GIT_USERNAME}
          password: ${CONFIG_GIT_TOKEN}
          force-pull: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

**Không hard-code GitHub Personal Access Token trong `application.yaml`.** Token đã từng xuất hiện trong working tree nên cần thu hồi (revoke/rotate) token đó ngay trên GitHub trước khi có ai commit hoặc push phần bài hôm nay. Dùng biến môi trường `CONFIG_GIT_USERNAME` và `CONFIG_GIT_TOKEN`, hoặc credential manager/secret store, thay cho token viết thẳng trong file.

`force-pull: true` có ý nghĩa: trước khi phục vụ cấu hình, Config Server cố lấy phiên bản mới từ nhánh Git. Nó tiện lúc học; đổi lại service phụ thuộc vào việc Git remote còn truy cập được.

## Cấu hình client ở `user-service` và `api-gateway`

Hai service đã thêm dependency sau:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

Và cùng dùng import:

```yaml
spring:
  application:
    name: user-service # api-gateway đổi thành api-gateway
  config:
    import: configserver:http://localhost:5555
```

Đây là phần quan trọng nhất của client:

1. Spring Boot đọc `spring.application.name`.
2. Client gọi Config Service ở `http://localhost:5555`.
3. Config Service lấy file phù hợp từ config repository, ví dụ `user-service.yml`.
4. Client gộp cấu hình nhận được rồi mới hoàn tất khởi động.

Vì không có tiền tố `optional:`, nếu Config Service chưa chạy hoặc không kết nối được, client nên dừng khởi động. Khi học đây là tín hiệu tốt: nó nhắc mình chạy Config Service trước.

## Config nào đã chuyển ra ngoài?

Trong source hiện tại, `application.yaml` của `user-service` và `api-gateway` đã bỏ các cấu hình local về port, Eureka và Gateway. Điều đó có nghĩa config repository cần chứa lại những phần này. Ví dụ:

```yaml
# user-service.yml ở config repository
server:
  port: 8082

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

```yaml
# api-gateway.yml ở config repository
server:
  port: 8080

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

spring:
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true

app:
  api-prefix: api
```

Các file trong config repository không nằm trong workspace này, vì vậy trước khi chạy cần kiểm tra chúng đã tồn tại và đúng tên ứng dụng (`user-service`, `api-gateway`) hay chưa.

## Swagger/OpenAPI đã thêm gì?

| Module | Dependency | Mục đích |
| --- | --- | --- |
| `user-service` | `springdoc-openapi-starter-webmvc-ui` | Tạo `/v3/api-docs` và giao diện Swagger UI cho service MVC. |
| `api-gateway` | `springdoc-openapi-starter-webflux-ui` | Tạo Swagger UI cho Gateway WebFlux. |

API Gateway có thêm ba phần:

- `WebclientConfig`: tạo `WebClient.Builder` có `@LoadBalanced`, nên URL như `http://user-service/...` được resolve qua service discovery thay vì tự ghi host/port.
- `SwaggerProxyController`: lấy OpenAPI của **một** service qua `/swagger-docs/{serviceName}`, rồi đổi `servers` để nút **Try it out** gọi qua Gateway.
- `SwaggerAggregateController`: lấy danh sách service từ `DiscoveryClient`, đọc `/v3/api-docs` từng service, gộp `paths` và schema lại thành `/swagger-docs/all`.

## Luồng Swagger tổng hợp

```text
Browser mở Swagger UI tại API Gateway
  -> Gateway gọi DiscoveryClient để lấy các service đã đăng ký Eureka
  -> Gateway dùng LoadBalanced WebClient gọi http://<service>/v3/api-docs
  -> SwaggerAggregateController gộp paths/schemas
  -> Browser nhận OpenAPI đã gộp ở /swagger-docs/all
  -> Khi “Try it out”, request đi qua /api/<service>/<endpoint>
  -> Gateway route request đến service đích
```

Hai điểm cần nhớ:

- Service nào chưa đăng ký với Eureka, hoặc không có `/v3/api-docs`, sẽ bị bỏ qua trong Swagger tổng hợp; một service lỗi không làm toàn bộ endpoint `/swagger-docs/all` lỗi.
- `SwaggerAggregateController` thêm prefix service vào đường dẫn (`/api/<service>/...`) để Swagger gọi qua Gateway, không gọi thẳng port của từng service.

## Thứ tự chạy để đỡ lỗi

1. Khởi động PostgreSQL: `docker compose up -d postgres`.
2. Chạy `service-discovery` (Eureka) ở cổng `8761`.
3. Đặt `CONFIG_GIT_USERNAME` và `CONFIG_GIT_TOKEN` trong môi trường local, rồi chạy `config-service` ở cổng `5555`.
4. Kiểm tra Config Service đọc được config repository; ví dụ thử endpoint cấu hình của `user-service`.
5. Chạy `user-service` và `api-gateway`.
6. Kiểm tra hai service xuất hiện trong Eureka.
7. Mở Swagger UI của Gateway và kiểm tra OpenAPI tổng hợp.

## Checklist trước commit bài hôm nay

- [ ] Thu hồi/rotate token GitHub đã lộ trong file cấu hình; không đưa token vào Git history.
- [ ] Sửa Config Service dùng biến môi trường hoặc secret store.
- [ ] Bổ sung `config-service` vào root `pom.xml` nếu vẫn dùng Maven multi-module. Hiện file root `pom.xml` đang bị xóa trong working tree, nên Maven chạy từ root sẽ không còn biết các module.
- [ ] Không commit `.idea/`, `config-service.zip`, `.env` hay file datasource local, trừ khi cả nhóm chủ động muốn chia sẻ chúng.
- [ ] Kiểm tra config repository có `user-service.yml` và `api-gateway.yml`.
- [ ] Chạy test/build từng module sau khi Config Service đã an toàn.
- [ ] Chia commit gợi ý: (1) PostgreSQL/JPA, (2) Config Service + config client, (3) Swagger Gateway, (4) docs.

## Những phần chưa kết luận

- Chưa xác minh build hoặc chạy thực tế trong lúc viết ghi chú này.
- Chưa kiểm tra repository cấu hình ngoài workspace, nên không thể khẳng định các file remote đã có đầy đủ datasource, Eureka và Gateway route.
- Chưa commit bất kỳ thay đổi nào của bài hôm nay.
