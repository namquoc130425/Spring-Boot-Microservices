package com.nvqn.config_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles({"test", "native"})
class ConfigServiceApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void shouldServeConfigFromLocalTestRepository() {
		var response = restTemplate.getForEntity(
				"/user-service/default",
				String.class
		);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody())
				.contains("user-service-from-local-test-repository");
	}
}
