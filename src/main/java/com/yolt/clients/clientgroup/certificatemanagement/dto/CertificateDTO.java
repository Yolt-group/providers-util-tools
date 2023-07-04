package com.yolt.clients.clientgroup.certificatemanagement.dto;

import lombok.Value;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.Set;

@Value
public class CertificateDTO {
    String name;
    CertificateType certificateType;
    String kid;
    CertificateUsageType usageType;
    Set<ServiceType> serviceTypes;
    String keyAlgorithm;
    String signatureAlgorithm;
    String certificateSigningRequest;
    String signedCertificateChain;
}
