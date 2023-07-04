package com.yolt.clients.authmeans;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.awaitility.Durations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;
import java.util.UUID;

import static com.yolt.clients.authmeans.ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansMessageType.*;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class ClientAuthenticationMeansKafkaConsumerTest {

    // For setup
    @Autowired
    private KafkaTemplate<String, ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO> kafkaTemplate;
    @Value("${yolt.kafka.topics.clientAuthenticationMeans.topic-name}")
    String authenticationMeansTopic;
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;
    @Autowired
    RedirectURLRepository redirectUrlRepository;
    // For checking
    @Autowired
    ClientGroupOnboardedProviderRepository clientGroupOnboardedProviderRepository;
    @Autowired
    ClientOnboardedProviderRepository clientOnboardedProviderRepository;
    @Autowired
    ClientOnboardedScrapingProviderRepository clientOnboardedScrapingProviderRepository;

    @Test
    void given_clientGroup_when_createAndThenDeleteClientGroupAuthenticationMeansOverKafka_ClientGroupOnboardedProviderIdIsAddedThenDeleted() {
        // given a ClientGroup
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + UUID.randomUUID()));

        // Add an onboarded provider.
        sendClientAuthenticationMeansEvent(CLIENT_GROUP_AUTHENTICATION_MEANS_UPDATED, new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                clientGroupId,
                /* clientId should be null, filled with clientGroupId for backward compat reasons */ clientGroupId,
                null,
                "garbage1",
                ServiceType.AIS
        ));
        // Check that it exists.
        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientGroupOnboardedProviderRepository.findById(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                        .clientGroupId(clientGroupId)
                        .provider("garbage1")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .build()
                ).isPresent()
        );

        // Delete it.
        sendClientAuthenticationMeansEvent(CLIENT_GROUP_AUTHENTICATION_MEANS_DELETED, new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                clientGroupId,
                /* clientId should be null, filled with clientGroupId for backward compat reasons */ clientGroupId,
                null,
                "garbage1",
                ServiceType.AIS
        ));
        // Check that it is gone.
        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientGroupOnboardedProviderRepository.findById(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                        .clientGroupId(clientGroupId)
                        .provider("garbage1")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .build()
                ).isEmpty()
        );
    }

    @Test
    void given_clientAndRedirectUrl_when_createAndThenDeleteClientAuthenticationMeansOverKafka_ClientOnboardedProviderIdIsAddedThenDeleted() {
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));
        // .. and a redirectUrl
        final UUID redirectUrlId = UUID.randomUUID();
        redirectUrlRepository.save(new RedirectURL(clientId, redirectUrlId, "https://example.com"));

        // Add an onboarded provider.
        sendClientAuthenticationMeansEvent(CLIENT_AUTHENTICATION_MEANS_UPDATED, new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                null,
                clientId,
                redirectUrlId,
                "garbage2",
                ServiceType.AIS
        ));
        // Check that it exists.
        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientOnboardedProviderRepository.findById(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                        .clientId(clientId)
                        .redirectUrlId(redirectUrlId)
                        .provider("garbage2")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .build()
                ).isPresent()
        );

        // Delete it.
        sendClientAuthenticationMeansEvent(CLIENT_AUTHENTICATION_MEANS_DELETED, new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                null,
                clientId,
                redirectUrlId,
                "garbage2",
                ServiceType.AIS
        ));
        // Check that it is gone.
        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientOnboardedProviderRepository.findById(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                        .clientId(clientId)
                        .redirectUrlId(redirectUrlId)
                        .provider("garbage2")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .build()
                ).isEmpty()
        );
    }

    @Test
    void given_client_when_createAndThenDeleteClientAuthenticationMeansForScrapingProviderOverKafka_ClientOnboardedScrapingProviderIdIsAddedThenDeleted() {
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));

        // Add an onboarded provider.
        sendClientAuthenticationMeansEvent(CLIENT_AUTHENTICATION_MEANS_UPDATED, new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                null,
                clientId,
                null,
                "garbage3",
                ServiceType.AIS
        ));
        // Check that it exists.
        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientOnboardedScrapingProviderRepository.findById(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                        .clientId(clientId)
                        .provider("garbage3")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .build()
                ).isPresent()
        );

        // Delete it.
        sendClientAuthenticationMeansEvent(CLIENT_AUTHENTICATION_MEANS_DELETED, new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                null,
                clientId,
                null,
                "garbage3",
                ServiceType.AIS
        ));
        // Check that it is gone.
        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientOnboardedScrapingProviderRepository.findById(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                        .clientId(clientId)
                        .provider("garbage3")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .build()
                ).isEmpty()
        );
    }

    void sendClientAuthenticationMeansEvent(
            ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansMessageType type,
            ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO event
    ) {
        kafkaTemplate.send(MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, authenticationMeansTopic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, UUID.randomUUID().toString())
                .setHeader("payload-type", type.name())
                .build()
        ).completable().join();
    }

    @NotNull
    private Client makeClient(UUID clientGroupId, UUID clientId) {
        return new Client(
                clientId,
                clientGroupId,
                "garbage-" + clientId,
                "NL",
                false,
                false,
                "10.71",
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptySet()
        );
    }
}
