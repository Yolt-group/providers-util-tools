package com.yolt.clients.client.outboundallowlist;

import com.yolt.clients.jira.Status;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AllowedOutboundHostRepository extends CrudRepository<AllowedOutboundHost, UUID> {
    Set<AllowedOutboundHost> findAllByClientIdOrderByLastUpdatedDesc(UUID clientId);

    Optional<AllowedOutboundHost> findByClientIdAndId(UUID clientId, UUID id);

    Optional<AllowedOutboundHost> findByClientIdAndHost(UUID clientId, String host);

    boolean existsByClientIdAndHostAndStatus(UUID clientId, String host, Status status);

    Set<AllowedOutboundHost> findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(UUID clientId, LocalDateTime after);

    boolean existsByClientIdAndStatus(UUID clientIdClaim, Status status);
}
