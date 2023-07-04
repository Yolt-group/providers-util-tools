package com.yolt.clients.client.mtlscertificates;

import lombok.Value;
import nl.ing.lovebird.validation.bc.PEM;
import org.bouncycastle.cert.X509CertificateHolder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class ClientCertificateEvent {
    @NotNull
    UUID clientId;
    @NotNull
    @Pattern(regexp = "^[a-fA-F0-9]{40}$", message = "expecting a SHA1 hash for the certificate fingerprint")
    String certificateFingerprint;
    @NotNull
    @PEM(expectedTypes = X509CertificateHolder.class)
    String certificate;
    @NotNull
    LocalDateTime seen;
}
