package com.yolt.clients.client.mtlscertificates;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import nl.ing.lovebird.clienttokens.verification.exception.MismatchedClientIdAndClientTokenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientMtlsCertificateEventConsumerTest {
    private static final String CERTIFICATE_FINGERPRINT = "c46804af9e57b9bcb6f0887585e94402f77e212d";
    private static final String CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            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
            -----END CERTIFICATE-----
            """;
    public static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    private ClientMtlsCertificateEventConsumer clientMtlsCertificateEventConsumer;

    @Mock
    private ClientIdVerificationService clientIdVerificationService;
    @Mock
    private ClientMTLSCertificateRepository clientMTLSCertificateRepository;
    @Mock
    private MTLSCertificateProbeRegistry certificateProbeRegistry;
    private Clock clock = Clock.fixed(Instant.parse("2018-08-19T16:45:42.00Z"), ZoneId.of("Europe/Amsterdam"));

    @BeforeEach
    void setUp() {
        clientMtlsCertificateEventConsumer = new ClientMtlsCertificateEventConsumer(
                clientIdVerificationService,
                clientMTLSCertificateRepository,
                certificateProbeRegistry,
                clock
        );
    }

    @AfterEach
    void validateMocks() {
        verifyNoMoreInteractions(
                clientIdVerificationService,
                clientMTLSCertificateRepository
        );
    }

    @Test
    void consumeClientCertificateEvent_no_preexisting_cert() {
        var clientId = UUID.randomUUID();
        ClientCertificateEvent event = new ClientCertificateEvent(
                clientId,
                CERTIFICATE_FINGERPRINT,
                CERTIFICATE,
                NOW
        );
        ClientToken clientToken = mock(ClientToken.class);

        when(clientMTLSCertificateRepository.existsByClientIdAndFingerprint(any(), any())).thenReturn(false);

        clientMtlsCertificateEventConsumer.consumeClientCertificateEvent(event, clientToken);

        ClientMTLSCertificate expectedCertificate = new ClientMTLSCertificate(
                clientId,
                CERTIFICATE_FINGERPRINT,
                new BigInteger("17763103175091891438"),
                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                "C=NL,ST=Amsterdam,L=Amsterdam,O=ING,OU=Yolt,CN=Yolt",
                LocalDateTime.of(2017, 8, 21, 11, 26, 44),
                LocalDateTime.of(2027, 8, 19, 11, 26, 44),
                NOW,
                NOW,
                CERTIFICATE,
                NOW
        );

        verify(clientIdVerificationService).verify(clientToken, clientId);
        verify(clientMTLSCertificateRepository).existsByClientIdAndFingerprint(clientId, CERTIFICATE_FINGERPRINT);
        verify(clientMTLSCertificateRepository).save(expectedCertificate);
        verify(certificateProbeRegistry).registerProbeAndCertificate(expectedCertificate, LocalDateTime.now(clock));
    }

    @Test
    void consumeClientCertificateEvent_preexisting_cert() {
        var clientId = UUID.randomUUID();
        ClientCertificateEvent event = new ClientCertificateEvent(
                clientId,
                CERTIFICATE_FINGERPRINT,
                CERTIFICATE,
                NOW
        );
        ClientToken clientToken = mock(ClientToken.class);

        when(clientMTLSCertificateRepository.existsByClientIdAndFingerprint(any(), any())).thenReturn(true);

        clientMtlsCertificateEventConsumer.consumeClientCertificateEvent(event, clientToken);

        verify(clientIdVerificationService).verify(clientToken, clientId);
        verify(clientMTLSCertificateRepository).existsByClientIdAndFingerprint(clientId, CERTIFICATE_FINGERPRINT);
        verify(clientMTLSCertificateRepository).updateFirstSeen(clientId, CERTIFICATE_FINGERPRINT, NOW);
        verify(clientMTLSCertificateRepository).updateLastSeen(clientId, CERTIFICATE_FINGERPRINT, NOW);
        verify(certificateProbeRegistry).updateCertificateCache(clientId, CERTIFICATE_FINGERPRINT, NOW);
    }

    @Test
    void consumeClientCertificateEvent_mismatching_clientId() {
        var clientId = UUID.randomUUID();
        ClientCertificateEvent event = new ClientCertificateEvent(
                clientId,
                CERTIFICATE_FINGERPRINT,
                CERTIFICATE,
                NOW
        );
        ClientToken clientToken = mock(ClientToken.class);
        var cause = new MismatchedClientIdAndClientTokenException("oops");
        doThrow(cause).when(clientIdVerificationService).verify(clientToken, clientId);

        assertThrows(MismatchedClientIdAndClientTokenException.class, () -> clientMtlsCertificateEventConsumer.consumeClientCertificateEvent(event, clientToken));

        verify(clientIdVerificationService).verify(clientToken, clientId);
    }
}