package com.yolt.clients.clientgroup.certificatemanagement.repository;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends CrudRepository<Certificate, Certificate.CertificateId> {
    boolean existsByClientGroupIdAndName(UUID clientGroupId, String name);

    List<Certificate> findCertificatesByClientGroupIdAndCertificateType(UUID clientGroupId, CertificateType certificateType);

    Optional<Certificate> findCertificateByClientGroupIdAndCertificateTypeAndKid(UUID clientGroupId, CertificateType certificateType, String kid);

    Optional<Certificate> findCertificateByClientGroupIdAndKid(UUID clientGroupId, String kid);

    List<Certificate> findCertificatesByClientGroupIdAndCertificateUsageType(UUID clientGroupId, CertificateUsageType usageType);

    long countByClientGroupId(UUID clientGroupId);

}
