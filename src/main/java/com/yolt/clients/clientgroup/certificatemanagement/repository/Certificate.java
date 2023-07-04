package com.yolt.clients.clientgroup.certificatemanagement.repository;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.providerdomain.ServiceType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "client_group_certificates")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
@IdClass(Certificate.CertificateId.class)
public class Certificate {
    @Id
    @Column(name = "certificate_type")
    @Enumerated(EnumType.STRING)
    private CertificateType certificateType;

    @Id
    @Column(name = "key_id")
    private String kid;

    @Id
    @Column(name = "client_group_id")
    private UUID clientGroupId;

    @Column(name = "name")
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_group_certificates_service_types", joinColumns = {@JoinColumn(name = "certificate_type"), @JoinColumn(name = "client_group_id"), @JoinColumn(name = "key_id")})
    @Column(name = "service_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<ServiceType> serviceTypes;

    @Column(name = "certificate_usage_type")
    @Enumerated(EnumType.STRING)
    private CertificateUsageType certificateUsageType;

    @Column(name = "key_algorithm")
    private String keyAlgorithm;

    @Column(name = "signature_algorithm")
    private String signatureAlgorithm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_group_certificates_subject_alternative_names", joinColumns = {@JoinColumn(name = "certificate_type"), @JoinColumn(name = "client_group_id"), @JoinColumn(name = "key_id")})
    @Column(name = "subject_alternative_name", nullable = false)
    private Set<String> subjectAlternativeNames;

    @Column(name = "certificate_signing_request")
    private String certificateSigningRequest;

    @Column(name = "signed_certificate_chain")
    private String signedCertificateChain;

    @Column(name = "provider_key")
    private String providerKey;

    public Certificate(
            CertificateType certificateType,
            String kid,
            UUID clientGroupId,
            String name,
            Set<ServiceType> serviceTypes,
            CertificateUsageType certificateUsageType,
            String keyAlgorithm,
            String signatureAlgorithm,
            Set<String> subjectAlternativeNames,
            String certificateSigningRequest,
            String signedCertificateChain
    ) {
        this.certificateType = certificateType;
        this.kid = kid;
        this.clientGroupId = clientGroupId;
        this.name = name;
        this.serviceTypes = serviceTypes;
        this.certificateUsageType = certificateUsageType;
        this.keyAlgorithm = keyAlgorithm;
        this.signatureAlgorithm = signatureAlgorithm;
        this.subjectAlternativeNames = subjectAlternativeNames;
        this.certificateSigningRequest = certificateSigningRequest;
        this.signedCertificateChain = signedCertificateChain;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CertificateId implements Serializable {
        private CertificateType certificateType;
        private String kid;
        private UUID clientGroupId;
    }
}
