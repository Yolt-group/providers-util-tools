package com.yolt.clients.authmeans;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
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
class ClientOnboardedProviderRepositoryTest {

    // For setup
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;
    @Autowired
    RedirectURLRepository redirectUrlRepository;

    // Under test
    @Autowired
    ClientOnboardedProviderRepository clientOnboardedProviderRepository;

    @Test
    void given_clientAndRedirectUrl_when_insertClientOnboardedProvider_then_success() {
        // given a Client ..
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));
        // .. and a redirectUrl
        final UUID redirectUrlId = UUID.randomUUID();
        redirectUrlRepository.save(new RedirectURL(clientId, redirectUrlId, "https://example.com"));

        // Insert a ClientOnboardedProvider.
        var key = ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                .clientId(clientId)
                .redirectUrlId(redirectUrlId)
                .serviceType(ServiceType.AIS)
                .provider("TEST")
                .build();
        clientOnboardedProviderRepository.save(ClientOnboardedProvider.builder()
                .clientOnboardedProviderId(key)
                .build()
        );

        // Check that it indeed exists.
        var createdEntity = clientOnboardedProviderRepository.findById(key);
        assertThat(createdEntity).isPresent();
        createdEntity.ifPresent(o -> {
            assertThat(o.getClientOnboardedProviderId()).isEqualTo(key);
            assertThat(o.getCreatedAt()).isNotNull();
        });

        // Delete the ClientOnboardedProvider.
        clientOnboardedProviderRepository.deleteById(key);
        // Check that it is gone.
        assertThat(clientOnboardedProviderRepository.findById(key)).isEmpty();
    }

    @Test
    void given_nonExistingClient_when_insertClientOnboardedProvider_then_failure() {
        // test unsuccessful insert (fk failure)
        var key = ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                .clientId(/* does not exist */ UUID.randomUUID())
                .redirectUrlId(/* does not exist */ UUID.randomUUID())
                .serviceType(ServiceType.AIS)
                .provider("TEST")
                .build();
        assertThatCode(() -> clientOnboardedProviderRepository.save(ClientOnboardedProvider.builder()
                .clientOnboardedProviderId(key)
                .build()
        )).hasMessageContaining("client_onboarded_provider_client_id_");
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
