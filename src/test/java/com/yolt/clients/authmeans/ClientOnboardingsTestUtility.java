package com.yolt.clients.authmeans;

import lombok.NonNull;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.awaitility.Durations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.yolt.clients.authmeans.ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansMessageType.CLIENT_AUTHENTICATION_MEANS_UPDATED;
import static com.yolt.clients.authmeans.ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansMessageType.CLIENT_GROUP_AUTHENTICATION_MEANS_UPDATED;
import static org.awaitility.Awaitility.await;

/**
 * This class "opens up" the package-private functionality under {@link com.yolt.clients.authmeans} to
 * other tests.
 */
@Component
public class ClientOnboardingsTestUtility {

    @Autowired
    private KafkaTemplate<String, ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO> kafkaTemplate;

    @Value("${yolt.kafka.topics.clientAuthenticationMeans.topic-name}")
    String authenticationMeansTopic;

    @Autowired
    ClientOnboardedProviderRepository clientOnboardedProviderRepository;

    @Autowired
    ClientGroupOnboardedProviderRepository clientGroupOnboardedProviderRepository;

    public void addOnboardedAISProviderForClientGroup(
            @NonNull UUID clientGroupId,
            @NonNull String provider
    ) {
        var event = new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                clientGroupId,
                null,
                null,
                provider,
                ServiceType.AIS
        );

        send(event, CLIENT_GROUP_AUTHENTICATION_MEANS_UPDATED);

        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientGroupOnboardedProviderRepository.findById(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                        .clientGroupId(clientGroupId)
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .provider(provider)
                        .build()
                ).isPresent()
        );

    }

    public void addOnboardedAISProviderForClient(
            @NonNull UUID clientId,
            @NonNull UUID redirectUrlId,
            @NonNull String provider
    ) {
        var event = new ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO(
                null,
                clientId,
                redirectUrlId,
                provider,
                ServiceType.AIS
        );

        send(event, CLIENT_AUTHENTICATION_MEANS_UPDATED);

        await().atMost(Durations.TEN_SECONDS).until(() ->
                clientOnboardedProviderRepository.findById(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                        .clientId(clientId)
                        .redirectUrlId(redirectUrlId)
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .provider(provider)
                        .build()
                ).isPresent()
        );
    }

    private void send(ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansEventDTO event, ClientAuthenticationMeansKafkaConsumer.ClientAuthenticationMeansMessageType messageType) {
        kafkaTemplate.send(MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, authenticationMeansTopic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, UUID.randomUUID().toString())
                .setHeader("payload-type", messageType.name())
                .build()
        ).completable().join();
    }

}
