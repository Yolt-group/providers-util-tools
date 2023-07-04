package com.yolt.clients.authmeans;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.ClientGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@IntegrationTest
class ClientGroupOnboardedProviderRepositoryTest {

    // For setup
    @Autowired
    ClientGroupRepository clientGroupRepository;

    // Under test
    @Autowired
    ClientGroupOnboardedProviderRepository clientGroupOnboardedProviderRepository;

    @Test
    void given_clientGroup_when_insertClientGroupOnboardedProvider_then_success() {
        // given a ClientGroup
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + UUID.randomUUID()));

        // Insert an onboarded provider.
        var key = ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                .clientGroupId(clientGroupId)
                .provider("TEST")
                .serviceType(ServiceType.AIS)
                .build();
        clientGroupOnboardedProviderRepository.save(ClientGroupOnboardedProvider.builder()
                .clientGroupOnboardedProviderId(key)
                .build()
        );

        // Check that it indeed exists.
        var createdEntity = clientGroupOnboardedProviderRepository.findById(key);
        assertThat(createdEntity).isPresent();
        createdEntity.ifPresent(o -> {
            assertThat(o.getClientGroupOnboardedProviderId()).isEqualTo(key);
            assertThat(o.getCreatedAt()).isNotNull();
        });

        // Delete the onboarded provider.
        clientGroupOnboardedProviderRepository.deleteById(key);
        // Check that it is gone.
        assertThat(clientGroupOnboardedProviderRepository.findById(key)).isEmpty();
    }

    @Test
    void given_nonExistingClientGroup_when_insertClientGroupOnboardedProvider_then_failure() {
        // test unsuccessful insert (fk failure)
        var key = ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                .clientGroupId(/* does not exist*/ UUID.randomUUID())
                .provider("TEST")
                .serviceType(ServiceType.AIS)
                .build();
        assertThatCode(() -> clientGroupOnboardedProviderRepository.save(ClientGroupOnboardedProvider.builder()
                .clientGroupOnboardedProviderId(key)
                .build()
        )).hasMessageContaining("client_group_onboarded_provider_client_group_id_fkey");
    }

}