package com.yolt.clients.clientgroup.certificatemanagement.eidas;

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
@ValidEidasCertificateRequest
public class EIDASCertificateSigningRequestDTO extends CertificateSigningRequestDTO {
    public EIDASCertificateSigningRequestDTO(
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
