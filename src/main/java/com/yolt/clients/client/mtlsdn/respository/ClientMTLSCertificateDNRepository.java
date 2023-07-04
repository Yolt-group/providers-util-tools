package com.yolt.clients.client.mtlsdn.respository;

import com.yolt.clients.jira.Status;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ClientMTLSCertificateDNRepository extends CrudRepository<ClientMTLSCertificateDN, UUID> {
    boolean existsByClientIdAndStatusIn(UUID clientId, Set<Status> statuses);

    Optional<ClientMTLSCertificateDN> findByClientIdAndId(UUID clientId, UUID id);

    List<ClientMTLSCertificateDN> findAllByClientId(UUID clientId);

    List<ClientMTLSCertificateDN> findAllByClientIdAndUpdatedAtAfterAndJiraTicketNotNull(UUID clientId, LocalDateTime after);

    Optional<ClientMTLSCertificateDN> findByClientIdAndSubjectDNAndIssuerDN(UUID clientId, String subjectDN, String issuerDN);
}
