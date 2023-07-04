package com.yolt.clients.client.mtlscertificates;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@IntegrationTest
class ClientMtlsCertificateEventConsumerIT {

    private static final String CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            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
            -----END CERTIFICATE-----
            """;
    private static final String CERTIFICATE_FINGERPRINT = "c46804af9e57b9bcb6f0887585e94402f77e212d";
    @Value("${yolt.kafka.topics.client-mtls-certificate.topic-name}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, ClientCertificateEvent> kafkaTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private Clock clock;

    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private ClientMTLSCertificateRepository clientMTLSCertificateRepository;

    private UUID clientId;
    private ClientToken clientToken;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        var clientGroupId = UUID.randomUUID();
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Set.of()));
        clientsRepository.save(new Client(
                clientId,
                clientGroupId,
                "client",
                "NL",
                true,
                true,
                "12.1",
                4000,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                1L,
                Set.of()));
    }

    @Test
    void clientMtlsCertificateEventConsumer_without_existing_entry_gets_inserted() {
        assertThat(clientMTLSCertificateRepository.existsByClientIdAndFingerprint(clientId, CERTIFICATE_FINGERPRINT))
                .isFalse();

        var event = new ClientCertificateEvent(
                clientId,
                CERTIFICATE_FINGERPRINT,
                CERTIFICATE,
                LocalDateTime.now(clock)
        );
        Message<ClientCertificateEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientId.toString())
                .build();

        kafkaTemplate.send(message);

        await().untilAsserted(() -> assertThat(clientMTLSCertificateRepository.findByClientIdAndFingerprint(clientId, CERTIFICATE_FINGERPRINT))
                .contains(
                        new ClientMTLSCertificate(
                                clientId,
                                CERTIFICATE_FINGERPRINT,
                                new BigInteger("17763103175091891438"),
                                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                                LocalDateTime.of(2017, 8, 21, 11, 26, 44),
                                LocalDateTime.of(2027, 8, 19, 11, 26, 44),
                                LocalDateTime.now(clock),
                                LocalDateTime.now(clock),
                                CERTIFICATE,
                                LocalDateTime.now(clock)
                        )
                )
        );
    }

    @Test
    void clientMtlsCertificateEventConsumer_with_existing_entry_gets_updated() {
        clientMTLSCertificateRepository.save(
                new ClientMTLSCertificate(
                        clientId,
                        CERTIFICATE_FINGERPRINT,
                        new BigInteger("17763103175091891438"),
                        "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                        "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                        LocalDateTime.of(2017, 8, 21, 11, 26, 44),
                        LocalDateTime.of(2027, 8, 19, 11, 26, 44),
                        LocalDateTime.of(2017, 9, 21, 11, 26, 44),
                        LocalDateTime.of(2017, 10, 21, 11, 26, 44),
                        CERTIFICATE,
                        LocalDateTime.of(2017, 10, 21, 11, 26, 44)
                )
        );

        var event = new ClientCertificateEvent(
                clientId,
                CERTIFICATE_FINGERPRINT,
                CERTIFICATE,
                LocalDateTime.now(clock)
        );
        Message<ClientCertificateEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientId.toString())
                .build();

        kafkaTemplate.send(message);

        await().untilAsserted(() -> assertThat(clientMTLSCertificateRepository.findByClientIdAndFingerprint(clientId, CERTIFICATE_FINGERPRINT))
                .contains(
                        new ClientMTLSCertificate(
                                clientId,
                                CERTIFICATE_FINGERPRINT,
                                new BigInteger("17763103175091891438"),
                                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                                LocalDateTime.of(2017, 8, 21, 11, 26, 44),
                                LocalDateTime.of(2027, 8, 19, 11, 26, 44),
                                LocalDateTime.of(2017, 9, 21, 11, 26, 44),
                                LocalDateTime.now(clock),
                                CERTIFICATE,
                                LocalDateTime.now(clock)
                        )
                )
        );
    }

    @Test
    void clientMtlsCertificateEventConsumer_with_pre_added_entry_gets_updated() {
        clientMTLSCertificateRepository.save(
                new ClientMTLSCertificate(
                        clientId,
                        CERTIFICATE_FINGERPRINT,
                        new BigInteger("17763103175091891438"),
                        "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                        "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                        LocalDateTime.of(2017, 8, 21, 11, 26, 44),
                        LocalDateTime.of(2027, 8, 19, 11, 26, 44),
                        null,
                        null,
                        CERTIFICATE,
                        LocalDateTime.of(2017, 8, 21, 11, 26, 44)
                )
        );

        var event = new ClientCertificateEvent(
                clientId,
                CERTIFICATE_FINGERPRINT,
                CERTIFICATE,
                LocalDateTime.now(clock)
        );
        Message<ClientCertificateEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientId.toString())
                .build();

        kafkaTemplate.send(message);

        await().untilAsserted(() -> assertThat(clientMTLSCertificateRepository.findByClientIdAndFingerprint(clientId, CERTIFICATE_FINGERPRINT))
                .contains(
                        new ClientMTLSCertificate(
                                clientId,
                                CERTIFICATE_FINGERPRINT,
                                new BigInteger("17763103175091891438"),
                                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                                LocalDateTime.of(2017, 8, 21, 11, 26, 44),
                                LocalDateTime.of(2027, 8, 19, 11, 26, 44),
                                LocalDateTime.now(clock),
                                LocalDateTime.now(clock),
                                CERTIFICATE,
                                LocalDateTime.now(clock)
                        )
                )
        );
    }
}
