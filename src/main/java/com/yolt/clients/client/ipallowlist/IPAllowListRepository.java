package com.yolt.clients.client.ipallowlist;

import com.yolt.clients.jira.Status;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IPAllowListRepository extends CrudRepository<AllowedIP, UUID> {
    Set<AllowedIP> findAllByClientIdOrderByLastUpdatedDesc(UUID clientId);

    boolean existsByClientIdAndStatus(UUID clientId, Status status);

    Optional<AllowedIP> findByClientIdAndId(UUID clientId, UUID id);

    Optional<AllowedIP> findByClientIdAndCidr(UUID clientId, String cidr);

    Set<AllowedIP> findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(UUID clientId, LocalDateTime after);

}
