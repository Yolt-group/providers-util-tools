package com.yolt.clients.client.mtlscertificates;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.io.StringReader;
import java.time.Clock;
import java.time.ZoneOffset;

import static java.time.LocalDateTime.now;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientMtlsCertificateEventConsumer {

    private final ClientIdVerificationService clientIdVerificationService;
    private final ClientMTLSCertificateRepository clientMTLSCertificateRepository;
    private final MTLSCertificateProbeRegistry certificateProbeRegistry;
    private final Clock clock;

    @SneakyThrows
    @KafkaListener(
            topics = "${yolt.kafka.topics.client-mtls-certificate.topic-name}",
            concurrency = "${yolt.kafka.topics.client-mtls-certificate.listener-concurrency}"
    )
    @Transactional
    public void consumeClientCertificateEvent(@Payload @Valid ClientCertificateEvent event,
                                              @Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, event.getClientId());

        if (clientMTLSCertificateRepository.existsByClientIdAndFingerprint(event.getClientId(), event.getCertificateFingerprint())) {
            clientMTLSCertificateRepository.updateFirstSeen(event.getClientId(), event.getCertificateFingerprint(), event.getSeen());
            clientMTLSCertificateRepository.updateLastSeen(event.getClientId(), event.getCertificateFingerprint(), event.getSeen());
            certificateProbeRegistry.updateCertificateCache(event.getClientId(), event.getCertificateFingerprint(), event.getSeen());
        } else {
            PEMParser pemParser = new PEMParser(new StringReader(event.getCertificate()));
            // Object type has been validated by the @Pem annotation on the input object.
            X509CertificateHolder pemObject = (X509CertificateHolder) pemParser.readObject();

            ClientMTLSCertificate clientMTLSCertificate = new ClientMTLSCertificate(
                    event.getClientId(),
                    event.getCertificateFingerprint(),
                    pemObject.getSerialNumber(),
                    StringUtils.substring(pemObject.getSubject().toString(), 0, 1024),
                    StringUtils.substring(pemObject.getIssuer().toString(), 0, 1024),
                    pemObject.getNotBefore().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime(),
                    pemObject.getNotAfter().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime(),
                    event.getSeen(),
                    event.getSeen(),
                    event.getCertificate(),
                    event.getSeen()
            );
            clientMTLSCertificateRepository.save(clientMTLSCertificate);
            certificateProbeRegistry.registerProbeAndCertificate(clientMTLSCertificate, now(clock));
        }
    }
}
