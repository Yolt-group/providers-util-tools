package com.yolt.clients.clientgroup.certificatemanagement.openbanking.legacy;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateSigningRequestDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.SimpleDistinguishedNameElement;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@ValidOBLegacyCertificateRequest
public class OBLegacyCertificateSigningRequestDTO extends CertificateSigningRequestDTO {
    public OBLegacyCertificateSigningRequestDTO(
            String name,
            Set<ServiceType> serviceTypes,
            CertificateUsageType usageType,
            String keyAlgorithm,
            String signatureAlgorithm,
            List<SimpleDistinguishedNameElement> subjectDNs,
            Set<String> subjectAlternativeNames
    ) {
        super(name, serviceTypes, usageType, keyAlgorithm, signatureAlgorithm, subjectDNs, subjectAlternativeNames);
    }
}
