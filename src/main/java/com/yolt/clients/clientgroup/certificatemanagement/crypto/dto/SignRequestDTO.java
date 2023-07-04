package com.yolt.clients.clientgroup.certificatemanagement.crypto.dto;

import com.yolt.clients.clientgroup.certificatemanagement.crypto.SignatureAlgorithm;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import lombok.Value;

import java.util.UUID;

@Value
public class SignRequestDTO {
    UUID privateKid;
    SignatureAlgorithm algorithm;
    String payload;
    CertificateUsageType keyType;
}
