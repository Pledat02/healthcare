package com.hehe.appointment_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Context-load test can ha tang (DB/Keycloak/Redis). Chay unit test bang: -Dtest=*ServiceTest")
@SpringBootTest
class AppointmentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
