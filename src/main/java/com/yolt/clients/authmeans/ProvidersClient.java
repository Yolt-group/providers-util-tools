package com.yolt.clients.authmeans;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providerdomain.ServiceType;
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
@lombok.Value
@Service
class ProvidersClient {

    RestTemplate restTemplate;
    URI endpoint;

    public ProvidersClient(RestTemplateBuilder restTemplateBuilder, @Value("${service.providers.url}") String providersUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.endpoint = URI.create(providersUrl + "/all-onboarded-providers");
    }

    public Optional<List<OnboardedProvider>> retrieveAllAuthenticationMeans() {
        RequestEntity<?> entity = new RequestEntity<Void>(HttpMethod.GET, endpoint);
        var response = restTemplate.exchange(entity, new ParameterizedTypeReference<List<OnboardedProvider>>() {
        });

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("retrieveAllAuthenticationMeans failed with http {}", response.getStatusCode().value());
            return Optional.empty();
        }

        return Optional.ofNullable(response.getBody());
    }

    /**
     * Copied from com.yolt.providers.web.authenticationmeans.ListOnboardedProvidersController
     *
     * Either {@link #clientGroupId} or {@link #clientId} is filled, never both.
     * {@link #serviceType} is **always either "AIS" or "PIS".
     * {@link #redirectUrlId} is filled iff {@link #provider} is a non-scraping provider.
     */
    @lombok.Value
    static class OnboardedProvider {
        /**
         * Identifier of a ClientGroup, if non-null then {@link #clientId} is null.
         */
        UUID clientGroupId;
        /**
         * Identifier of a Client, if non-null then {@link #clientGroupId} is null.
         */
        UUID clientId;
        /**
         * Identifier of a provider.
         */
        @NonNull String provider;
        /**
         * Which {@link nl.ing.lovebird.providerdomain.ServiceType} can be used, always either {@link nl.ing.lovebird.providerdomain.ServiceType#AIS} or {@link nl.ing.lovebird.providerdomain.ServiceType#PIS}.
         */
        @NonNull ServiceType serviceType;
        /**
         * Identifier of the redirectUrlId that has been onboarded at a bank.  Only null in case {@link #provider}
         */
        UUID redirectUrlId;

        boolean isClientGroupOnboardedProvider() {
            return clientGroupId != null;
        }

        boolean isClientOnboardedProvider() {
            return clientId != null && redirectUrlId != null;
        }

        boolean isClientOnboardedScrapingProvider() {
            return clientId != null && redirectUrlId == null;
        }

    }

}
