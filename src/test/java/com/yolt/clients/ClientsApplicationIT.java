package com.yolt.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class ClientsApplicationIT {
    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void applicationStartsUp() {
        HttpStatus statusCode = restTemplate.getForEntity("http://localhost:" + managementPort + "/actuator/info", String.class).getStatusCode();
        assertThat(statusCode).isEqualTo(HttpStatus.OK);
    }
}
