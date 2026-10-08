package com.travel.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.rate-limit.enabled=false")
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
