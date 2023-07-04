package com.yolt.clients.client.redirecturls.repository;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RedirectURLRepository extends CrudRepository<RedirectURL, UUID> {

    boolean existsByClientIdAndRedirectURL(UUID clientId, String redirectURL);

    boolean existsByClientId(UUID clientId);

    List<RedirectURL> findAllByClientId(UUID clientId);

    Optional<RedirectURL> findByClientIdAndRedirectURLId(UUID clientId, UUID redirectURLId);
}
