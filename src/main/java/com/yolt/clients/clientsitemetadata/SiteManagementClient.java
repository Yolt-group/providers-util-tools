package com.yolt.clients.clientsitemetadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
class SiteManagementClient {

    private final RestTemplate restTemplate;
    private final URI endpoint;

    public SiteManagementClient(RestTemplateBuilder restTemplateBuilder, @Value("${service.site-management.url}") String providersUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.endpoint = URI.create(providersUrl + "/internal/client-site-entities");
    }

    public Optional<List<ClientSiteFromSiteManagement>> retrieveAllClientSiteMetadata() {
        RequestEntity<?> entity = new RequestEntity<Void>(HttpMethod.GET, endpoint);
        var response = restTemplate.exchange(entity, new ParameterizedTypeReference<List<ClientSiteFromSiteManagement>>() {
        });

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("retrieveAllClientSiteMetadata failed with http {}", response.getStatusCode().value());
            return Optional.empty();
        }

        return Optional.ofNullable(response.getBody());
    }

    /**
     * Copied from nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteEntity
     */
    @lombok.Value
    public static class ClientSiteFromSiteManagement {

        UUID clientId;
        UUID siteId;
        boolean enabled;
        boolean usingExperimentalVersion;
        boolean available;
        List<String> tags;

    }

}
