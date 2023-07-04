package com.yolt.clients.client.mtlscertificates;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MTLSCertificateProbeRegistryTest {

    private static final UUID CLIENT_ID_1 = UUID.randomUUID();
    private static final UUID CLIENT_ID_2 = UUID.randomUUID();
    private static final UUID CLIENT_ID_3 = UUID.randomUUID();

    @Mock
    ClientMTLSCertificateRepository repository;

    private final Clock clock = Clock.fixed(Instant.parse("2022-01-14T12:00:00.00Z"), ZoneId.of("Europe/Amsterdam"));

    @Test
    void shouldUpdateCertificateCache() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MTLSCertificateProbeRegistry config = new MTLSCertificateProbeRegistry(registry, repository, clock);

        ClientMTLSCertificate cert1 = new ClientMTLSCertificate();
        cert1.setClientId(CLIENT_ID_1);
        cert1.setFingerprint("fingerprint_cert_1");
        cert1.setValidEnd(LocalDateTime.of(2022, 1, 19, 12, 0, 0));
        cert1.setLastSeen(LocalDateTime.of(2022, 1, 10, 12, 0, 0));

        ClientMTLSCertificate cert2 = new ClientMTLSCertificate();
        cert2.setClientId(CLIENT_ID_2);
        cert2.setFingerprint("fingerprint_cert_2");
        cert2.setValidEnd(LocalDateTime.of(2023, 1, 19, 12, 0, 0));
        cert2.setLastSeen(LocalDateTime.of(2020, 1, 19, 12, 0, 0));

        ClientMTLSCertificate cert3 = new ClientMTLSCertificate();
        cert3.setClientId(CLIENT_ID_3);
        cert3.setFingerprint("fingerprint_cert_3");
        cert3.setValidEnd(LocalDateTime.of(2019, 1, 19, 12, 0, 0));
        cert3.setLastSeen(LocalDateTime.of(2019, 1, 10, 12, 0, 0));

        when(repository.findAll()).thenReturn(List.of(cert1, cert2, cert3));

        config.customizeMeterRegistry();

        List<Double> lastSeenMeasures = registry.find("client_mtls_certificate_last_seen").gauges().stream()
                .flatMap(gauge -> stream(gauge.measure().spliterator(), false))
                .map(Measurement::getValue)
                .collect(Collectors.toList());

        assertThat(lastSeenMeasures).hasSize(2)
                .containsExactlyInAnyOrder(726.0, 4.0);

        config.updateCertificateCache(CLIENT_ID_1, "fingerprint_cert_1", LocalDateTime.of(2022, 1, 14, 12, 0, 0));

        lastSeenMeasures = registry.find("client_mtls_certificate_last_seen").gauges().stream()
                .flatMap(gauge -> stream(gauge.measure().spliterator(), false))
                .map(Measurement::getValue)
                .collect(Collectors.toList());

        assertThat(lastSeenMeasures).hasSize(2)
                .containsExactlyInAnyOrder(726.0, 0.0);
    }

    @Test
    void shouldRegisterMTLSGauges() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MTLSCertificateProbeRegistry config = new MTLSCertificateProbeRegistry(registry, repository, clock);

        ClientMTLSCertificate cert1 = new ClientMTLSCertificate();
        cert1.setClientId(CLIENT_ID_1);
        cert1.setFingerprint("fingerprint_cert_1");
        cert1.setValidEnd(LocalDateTime.of(2022, 1, 19, 12, 0, 0));
        cert1.setLastSeen(LocalDateTime.of(2022, 1, 10, 12, 0, 0));

        ClientMTLSCertificate cert2 = new ClientMTLSCertificate();
        cert2.setClientId(CLIENT_ID_2);
        cert2.setFingerprint("fingerprint_cert_2");
        cert2.setValidEnd(LocalDateTime.of(2023, 1, 19, 12, 0, 0));
        cert2.setLastSeen(LocalDateTime.of(2020, 1, 19, 12, 0, 0));

        ClientMTLSCertificate cert3 = new ClientMTLSCertificate();
        cert3.setClientId(CLIENT_ID_3);
        cert3.setFingerprint("fingerprint_cert_3");
        cert3.setValidEnd(LocalDateTime.of(2019, 1, 19, 12, 0, 0));
        cert3.setLastSeen(LocalDateTime.of(2019, 1, 10, 12, 0, 0));

        when(repository.findAll()).thenReturn(List.of(cert1, cert2, cert3));

        config.customizeMeterRegistry();

        assertThat(registry.getMeters()).hasSize(4)
                .extracting("id.name", "id.tags")
                .contains(
                        tuple("client_mtls_certificate_expiration", List.of(
                                Tag.of("client_id", cert1.getClientId().toString()),
                                Tag.of("fingerprint", cert1.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_expiration", List.of(
                                Tag.of("client_id", cert2.getClientId().toString()),
                                Tag.of("fingerprint", cert2.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_last_seen", List.of(
                                Tag.of("client_id", cert1.getClientId().toString()),
                                Tag.of("fingerprint", cert1.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_last_seen", List.of(
                                Tag.of("client_id", cert2.getClientId().toString()),
                                Tag.of("fingerprint", cert2.getFingerprint())
                        ))
                );

        List<Double> gauges = registry.getMeters().stream()
                .flatMap(meter -> stream(meter.measure().spliterator(), false))
                .map(Measurement::getValue)
                .collect(Collectors.toList());

        assertThat(gauges).hasSize(4)
                .containsExactlyInAnyOrder(726.0, 4.0, 4.0, 369.0);
    }

    @Test
    void shouldRegisterMTLSGaugeAndCertificate() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MTLSCertificateProbeRegistry config = new MTLSCertificateProbeRegistry(registry, repository, clock);

        ClientMTLSCertificate cert1 = new ClientMTLSCertificate();
        cert1.setClientId(CLIENT_ID_1);
        cert1.setFingerprint("fingerprint_cert_1");
        cert1.setValidEnd(LocalDateTime.of(2022, 1, 19, 12, 0, 0));
        cert1.setLastSeen(LocalDateTime.of(2022, 1, 10, 12, 0, 0));

        ClientMTLSCertificate cert2 = new ClientMTLSCertificate();
        cert2.setClientId(CLIENT_ID_2);
        cert2.setFingerprint("fingerprint_cert_2");
        cert2.setValidEnd(LocalDateTime.of(2023, 1, 19, 12, 0, 0));
        cert2.setLastSeen(LocalDateTime.of(2020, 1, 19, 12, 0, 0));

        ClientMTLSCertificate cert3 = new ClientMTLSCertificate();
        cert3.setClientId(CLIENT_ID_3);
        cert3.setFingerprint("fingerprint_cert_3");
        cert3.setValidEnd(LocalDateTime.of(2023, 1, 19, 12, 0, 0));
        cert3.setLastSeen(LocalDateTime.of(2022, 1, 10, 12, 0, 0));

        when(repository.findAll()).thenReturn(List.of(cert1, cert2));

        config.customizeMeterRegistry();

        assertThat(registry.getMeters()).hasSize(4)
                .extracting("id.name", "id.tags")
                .contains(
                        tuple("client_mtls_certificate_expiration", List.of(
                                Tag.of("client_id", cert1.getClientId().toString()),
                                Tag.of("fingerprint", cert1.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_expiration", List.of(
                                Tag.of("client_id", cert2.getClientId().toString()),
                                Tag.of("fingerprint", cert2.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_last_seen", List.of(
                                Tag.of("client_id", cert1.getClientId().toString()),
                                Tag.of("fingerprint", cert1.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_last_seen", List.of(
                                Tag.of("client_id", cert2.getClientId().toString()),
                                Tag.of("fingerprint", cert2.getFingerprint())
                        ))
                );

        List<Double> gauges = registry.getMeters().stream()
                .flatMap(meter -> stream(meter.measure().spliterator(), false))
                .map(Measurement::getValue)
                .collect(Collectors.toList());

        assertThat(gauges).hasSize(4)
                .containsExactlyInAnyOrder(726.0, 4.0, 4.0, 369.0);

        config.registerProbeAndCertificate(cert3, now(clock));

        assertThat(registry.getMeters()).hasSize(6)
                .extracting("id.name", "id.tags")
                .contains(
                        tuple("client_mtls_certificate_expiration", List.of(
                                Tag.of("client_id", cert1.getClientId().toString()),
                                Tag.of("fingerprint", cert1.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_expiration", List.of(
                                Tag.of("client_id", cert2.getClientId().toString()),
                                Tag.of("fingerprint", cert2.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_expiration", List.of(
                                Tag.of("client_id", cert3.getClientId().toString()),
                                Tag.of("fingerprint", cert3.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_last_seen", List.of(
                                Tag.of("client_id", cert1.getClientId().toString()),
                                Tag.of("fingerprint", cert1.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_last_seen", List.of(
                                Tag.of("client_id", cert2.getClientId().toString()),
                                Tag.of("fingerprint", cert2.getFingerprint())
                        )),
                        tuple("client_mtls_certificate_last_seen", List.of(
                                Tag.of("client_id", cert3.getClientId().toString()),
                                Tag.of("fingerprint", cert3.getFingerprint())
                        ))
                );

        gauges = registry.getMeters().stream()
                .flatMap(meter -> stream(meter.measure().spliterator(), false))
                .map(Measurement::getValue)
                .collect(Collectors.toList());

        assertThat(gauges).hasSize(6)
                .containsExactlyInAnyOrder(4.0, 369.0, 4.0, 4.0, 726.0, 369.0);
    }

}
