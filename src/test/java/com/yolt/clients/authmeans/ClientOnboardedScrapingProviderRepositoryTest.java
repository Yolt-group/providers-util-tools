package com.yolt.clients.authmeans;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@IntegrationTest
class ClientOnboardedScrapingProviderRepositoryTest {

    // For setup
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;

    // Under test
    @Autowired
    ClientOnboardedScrapingProviderRepository clientOnboardedScrapingProviderRepository;

    @Test
    void given_client_when_insertClientOnboardedScrapingProvider_then_success() {
        // given a Client
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));

        // Insert a ClientOnboardedScrapingProvider.
        var key = ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                .clientId(clientId)
                .serviceType(ServiceType.AIS)
                .provider("TEST")
                .build();
        clientOnboardedScrapingProviderRepository.save(ClientOnboardedScrapingProvider.builder()
                .clientOnboardedScrapingProviderId(key)
                .build()
        );

        // Check that it indeed exists.
        var createdEntity = clientOnboardedScrapingProviderRepository.findById(key);
        assertThat(createdEntity).isPresent();
        createdEntity.ifPresent(o -> {
            assertThat(o.getClientOnboardedScrapingProviderId()).isEqualTo(key);
            assertThat(o.getCreatedAt()).isNotNull();
        });

        // Delete the ClientOnboardedProvider.
        clientOnboardedScrapingProviderRepository.deleteById(key);
        // Check that it is gone.
        assertThat(clientOnboardedScrapingProviderRepository.findById(key)).isEmpty();
    }

    @Test
    void given_nonExistingClient_when_insertClientOnboardedScrapingProvider_then_failure() {
        // test unsuccessful insert (fk failure)
        var key = ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                .clientId(/* does not exist */ UUID.randomUUID())
                .serviceType(ServiceType.AIS)
                .provider("TEST")
                .build();
        assertThatCode(() -> clientOnboardedScrapingProviderRepository.save(ClientOnboardedScrapingProvider.builder()
                .clientOnboardedScrapingProviderId(key)
                .build()
        )).hasMessageContaining("client_onboarded_scraping_provider_client_id_fkey");
    }


    @NotNull
    private Client makeClient(UUID clientGroupId, UUID clientId) {
        return new Client(
                clientId,
                clientGroupId,
                "garbage-" + clientId,
                "NL",
                false,
                false,
                "10.71",
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptySet()
        );
    }

}
