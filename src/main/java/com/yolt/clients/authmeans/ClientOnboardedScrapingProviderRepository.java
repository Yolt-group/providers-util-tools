package com.yolt.clients.authmeans;

import org.springframework.data.repository.CrudRepository;

/**
 * Onboarded scraping providers on client level.
 */
interface ClientOnboardedScrapingProviderRepository extends CrudRepository<ClientOnboardedScrapingProvider, ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId> {

}
