package com.yolt.clients.client.mtlscertificates;

import com.yolt.clients.client.mtlscertificates.ClientMTLSCertificate.CompositeKey;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.StreamSupport.stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MTLSCertificateProbeRegistry {

    @Autowired
    private final MeterRegistry meterRegistry;
    @Autowired
    private final ClientMTLSCertificateRepository repository;
    @Autowired
    private final Clock clock;

    private ConcurrentMap<CompositeKey, ClientMTLSCertificate> CERTS;

    @PostConstruct
    void customizeMeterRegistry() {
        log.info("Creating gauges for mTLS certificates");
        LocalDateTime today = now(clock);
        CERTS = stream(repository.findAll().spliterator(), false)
                .filter(cert -> cert.getValidEnd().isAfter(today))
                .collect(toConcurrentMap(cert -> new CompositeKey(cert.getClientId(), cert.getFingerprint()), identity()));

        CERTS.values().forEach(certificate -> registerProbe(certificate, today));
    }

    public void updateCertificateCache(UUID clientId, String fingerprint, LocalDateTime lastSeen) {
        CERTS.computeIfPresent(new CompositeKey(clientId, fingerprint), (key, cert) -> {
            cert.setLastSeen(lastSeen);
            return cert;
        });
    }

    public void registerProbeAndCertificate(ClientMTLSCertificate certificate, LocalDateTime today) {
        CERTS.put(new CompositeKey(certificate.getClientId(), certificate.getFingerprint()), certificate);
        registerProbe(certificate, today);
    }

    public void registerProbe(ClientMTLSCertificate certificate, LocalDateTime today) {
        Gauge.builder("client_mtls_certificate_expiration", certificate, cert -> getDaysUntilExpiration(cert, today))
                .description("days left for mTLS certificate")
                .tags(Tags.of(
                        "client_id", certificate.getClientId().toString(),
                        "fingerprint", certificate.getFingerprint()
                ))
                .baseUnit("days")
                .register(meterRegistry);

        Gauge.builder("client_mtls_certificate_last_seen", certificate, cert -> getDaysSinceLastSeen(cert, today))
                .description("days passed last seen")
                .tags(Tags.of(
                        "client_id", certificate.getClientId().toString(),
                        "fingerprint", certificate.getFingerprint()
                ))
                .baseUnit("days")
                .register(meterRegistry);
    }

    private double getDaysUntilExpiration(ClientMTLSCertificate certificate, LocalDateTime today) {
        return DAYS.between(today, certificate.getValidEnd());
    }

    private double getDaysSinceLastSeen(ClientMTLSCertificate certificate, LocalDateTime today) {
        return Optional.ofNullable(CERTS.get(new CompositeKey(certificate.getClientId(), certificate.getFingerprint())))
                .map(ClientMTLSCertificate::getLastSeen)
                .map(lastSeen -> DAYS.between(lastSeen, today))
                .orElse(-1L);
    }

}
