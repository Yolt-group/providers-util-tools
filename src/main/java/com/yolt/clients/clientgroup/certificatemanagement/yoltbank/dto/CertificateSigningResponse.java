package com.yolt.clients.clientgroup.certificatemanagement.yoltbank.dto;

import lombok.Value;

@Value
public class CertificateSigningResponse {
    String kid;
    String signedCertificate;
}
