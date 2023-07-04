package com.yolt.clients.sites;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
class ProviderRestClient {
    private final RestTemplate restTemplate;

    ProviderRestClient(RestTemplateBuilder builder, @Value("${service.providers.url}") String endpointBaseUrl) {
        this.restTemplate = builder
                .rootUri(endpointBaseUrl)
                .setReadTimeout(Duration.ofSeconds(81)) // Match the read-timeout of provider plus some margin
                .build();
    }

    ProvidersSites getProvidersSites() {
        return restTemplate.getForEntity("/sites-details", ProvidersSites.class).getBody();
    }

}
