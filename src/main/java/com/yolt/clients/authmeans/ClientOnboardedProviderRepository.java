package com.yolt.clients.authmeans;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * Onboarded providers on client group level (non-scraping).
 */
public interface ClientOnboardedProviderRepository extends CrudRepository<ClientOnboardedProvider, ClientOnboardedProvider.ClientOnboardedProviderId> {

    @Modifying
    void deleteByClientOnboardedProviderId_ClientIdAndClientOnboardedProviderId_RedirectUrlId(UUID clientId, UUID redirectURLId);
}
