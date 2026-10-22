package com.nvqn.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.config.location=classpath:/application-test.yaml")
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}
	//Gateway vẫn kiểm tra bean, controller và dependency có khởi tạo được không.
	//Không cố gọi config-service:5555.
	//Không cố đăng ký vào Eureka.
	//Đây chưa phải test route thật; route thật sẽ nằm ở integration test.

}
