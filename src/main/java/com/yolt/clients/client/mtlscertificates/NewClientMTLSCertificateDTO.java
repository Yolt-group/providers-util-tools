package com.yolt.clients.client.mtlscertificates;

import lombok.Value;
import nl.ing.lovebird.validation.bc.PEM;
import org.bouncycastle.cert.X509CertificateHolder;

import javax.validation.constraints.NotNull;

@Value
public class NewClientMTLSCertificateDTO {
    @NotNull
    @PEM(expectedTypes = X509CertificateHolder.class, maxObjects = 256)
    String certificateChain;
}
