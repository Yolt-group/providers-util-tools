package com.yolt.clients.clientgroup.certificatemanagement.dto;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Date;

@Value
public class CertificateInfoDTO {
    @NotNull
    String kid;
    @NotNull
    BigInteger serialNumber;
    @NotNull
    String subject;
    @NotNull
    String issuer;
    @NotNull
    Date validFrom;
    @NotNull
    Date validTo;
    @NotNull
    String publicKeyAlgorithm;
    @NotNull
    String signingAlgorithm;
}
