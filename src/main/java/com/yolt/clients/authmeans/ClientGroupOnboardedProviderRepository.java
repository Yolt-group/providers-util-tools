package com.yolt.clients.authmeans;

import org.springframework.data.repository.CrudRepository;

/**
 * Onboarded providers on client group level (non-scraping).
 */
interface ClientGroupOnboardedProviderRepository extends CrudRepository<ClientGroupOnboardedProvider, ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId> {

}
