package com.yolt.clients.clientgroup.certificatemanagement.crypto.dto;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.SimpleDistinguishedNameElement;
import lombok.Value;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Value
public class CSRRequirementsDTO {
   CertificateUsageType type;
   Collection<ServiceType> serviceTypes;
   String signatureAlgorithm;
   List<SimpleDistinguishedNameElement> distinguishedNames;
   boolean eidasCertificate;
   Set<String> subjectAlternativeNames;
}
