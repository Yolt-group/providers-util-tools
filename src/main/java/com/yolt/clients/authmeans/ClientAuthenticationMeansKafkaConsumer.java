package com.yolt.clients.authmeans;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yolt.clients.clientsite.ClientSitesUpdateProducer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class ClientAuthenticationMeansKafkaConsumer {

    private final ClientGroupOnboardedProviderRepository clientGroupOnboardedProviderRepository;
    private final ClientOnboardedProviderRepository clientOnboardedProviderRepository;
    private final ClientOnboardedScrapingProviderRepository clientOnboardedScrapingProviderRepository;
    private final ClientSitesUpdateProducer clientSitesUpdateProducer;
    @KafkaListener(
            topics = "${yolt.kafka.topics.clientAuthenticationMeans.topic-name}",
            concurrency = "${yolt.kafka.topics.clientAuthenticationMeans.listener-concurrency}"
    )
    public void clientAuthenticationMeansUpdate(
            @Payload final ClientAuthenticationMeansEventDTO clientAuthenticationMeansUpdated,
            @Header(value = "payload-type") final String payloadType
    ) {
        ClientAuthenticationMeansMessageType clientAuthenticationMeansMessageType = parse(payloadType);

        try {
            log.info("ClientAuthenticationMeansKafkaConsumer consumed payload-type={} with dto={}", clientAuthenticationMeansMessageType, clientAuthenticationMeansUpdated); //NOSHERIFF
            clientAuthenticationMeansUpdated(clientAuthenticationMeansMessageType, clientAuthenticationMeansUpdated);
        } catch (Exception e) {
            log.error("Unexpected exception reading client authentication means update: {}", e.getMessage(), e); //NOSHERIFF
        }
    }

    private ClientAuthenticationMeansMessageType parse(String payloadType) {
        // Handle JSON encoded strings. Remove when all pods on > 13.0.25
        // See: https://yolt.atlassian.net/browse/CHAP-145
        if (payloadType.length() > 1 && payloadType.startsWith("\"") && payloadType.endsWith("\"")) {
            payloadType = payloadType.substring(1, payloadType.length() - 1);
        }
        return ClientAuthenticationMeansMessageType.valueOf(payloadType);
    }

    void clientAuthenticationMeansUpdated(
            ClientAuthenticationMeansMessageType messageTypeValue,
            ClientAuthenticationMeansEventDTO dto
    ) {
        switch (messageTypeValue) {
            case CLIENT_AUTHENTICATION_MEANS_UPDATED -> {
                if (dto.isForScrapingProvider()) {
                    clientOnboardedScrapingProviderRepository.save(ClientOnboardedScrapingProvider.builder()
                            .clientOnboardedScrapingProviderId(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                                    .clientId(dto.getClientId())
                                    .provider(dto.getProvider())
                                    .serviceType(ServiceType.valueOf(dto.getServiceType().name()))
                                    .build())
                            .build());
                } else {
                    clientOnboardedProviderRepository.save(ClientOnboardedProvider.builder()
                            .clientOnboardedProviderId(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                                    .clientId(dto.getClientId())
                                    .redirectUrlId(dto.getRedirectUrlId())
                                    .provider(dto.getProvider())
                                    .serviceType(ServiceType.valueOf(dto.getServiceType().name()))
                                    .build())
                            .build());
                }
            }
            case CLIENT_AUTHENTICATION_MEANS_DELETED -> {
                if (dto.isForScrapingProvider()) {
                    clientOnboardedScrapingProviderRepository.deleteById(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                            .clientId(dto.getClientId())
                            .provider(dto.getProvider())
                            .serviceType(ServiceType.valueOf(dto.getServiceType().name()))
                            .build());
                } else {
                    clientOnboardedProviderRepository.deleteById(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                            .clientId(dto.getClientId())
                            .redirectUrlId(dto.getRedirectUrlId())
                            .provider(dto.getProvider())
                            .serviceType(ServiceType.valueOf(dto.getServiceType().name()))
                            .build());
                }
            }
            case CLIENT_GROUP_AUTHENTICATION_MEANS_UPDATED -> {
                clientGroupOnboardedProviderRepository.save(ClientGroupOnboardedProvider.builder()
                        .clientGroupOnboardedProviderId(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                                .clientGroupId(dto.getClientGroupId())
                                .provider(dto.getProvider())
                                .serviceType(ServiceType.valueOf(dto.getServiceType().name()))
                                .build())
                        .build());
            }
            case CLIENT_GROUP_AUTHENTICATION_MEANS_DELETED -> {
                clientGroupOnboardedProviderRepository.deleteById(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                        .clientGroupId(dto.getClientGroupId())
                        .provider(dto.getProvider())
                        .serviceType(ServiceType.valueOf(dto.getServiceType().name()))
                        .build());

            }
        }
        // This has potential effect on the client-sites-list.
        clientSitesUpdateProducer.sendMessage();
    }

    @Data
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ClientAuthenticationMeansEventDTO {
        private UUID clientGroupId;

        private UUID clientId;
        /**
         * If present, this authentication means is *restricted to* this redirectUrlId.
         */
        @Nullable
        private UUID redirectUrlId;
        @NotNull
        private String provider;
        @NotNull
        private nl.ing.lovebird.providerdomain.ServiceType serviceType;

        boolean isForScrapingProvider() {
            return redirectUrlId == null;
        }
    }

    enum ClientAuthenticationMeansMessageType {
        CLIENT_AUTHENTICATION_MEANS_UPDATED,
        CLIENT_AUTHENTICATION_MEANS_DELETED,
        CLIENT_GROUP_AUTHENTICATION_MEANS_UPDATED,
        CLIENT_GROUP_AUTHENTICATION_MEANS_DELETED
    }

}