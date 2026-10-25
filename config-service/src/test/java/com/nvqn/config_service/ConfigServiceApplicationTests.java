package com.nvqn.config_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"test", "native"})
class ConfigServiceApplicationTests {

	@Autowired
	private WebApplicationContext context;

	@Test
	void shouldServeConfigFromLocalTestRepository() throws Exception {
		MockMvcBuilders.webAppContextSetup(context).build()
				.perform(get("/user-service/default"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("user-service-from-local-test-repository")));
	}
}
