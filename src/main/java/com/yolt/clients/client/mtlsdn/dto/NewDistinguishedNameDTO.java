package com.yolt.clients.client.mtlsdn.dto;

import lombok.Value;
import nl.ing.lovebird.validation.bc.PEM;
import org.bouncycastle.cert.X509CertificateHolder;

import javax.validation.constraints.NotNull;

@Value
public class NewDistinguishedNameDTO {
    @NotNull
    @PEM(expectedTypes = X509CertificateHolder.class, maxObjects = 256)
    String certificateChain;
}
