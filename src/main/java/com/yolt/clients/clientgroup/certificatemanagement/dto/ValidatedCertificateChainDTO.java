package com.yolt.clients.clientgroup.certificatemanagement.dto;

import lombok.Value;

@Value
public class ValidatedCertificateChainDTO {
    boolean valid;
    String message;
    CertificateInfoDTO info;
}
