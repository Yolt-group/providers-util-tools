package com.yolt.clients.client.redirecturls.repository;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RedirectURLChangelogRepository extends CrudRepository<RedirectURLChangelogEntry, UUID> {

    List<RedirectURLChangelogEntry> findFirst20ByClientIdOrderByRequestDateDesc(UUID clientId);

    List<RedirectURLChangelogEntry> findAllByClientId(UUID clientId);

    Optional<RedirectURLChangelogEntry> findByClientIdAndId(UUID clientId, UUID id);

}
