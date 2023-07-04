package com.yolt.clients.clientgroup.certificatemanagement.crypto.dto;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import lombok.Value;

@Value
public class KeyRequirementsDTO {
    CertificateUsageType type;
    String keyAlgorithm;
}
