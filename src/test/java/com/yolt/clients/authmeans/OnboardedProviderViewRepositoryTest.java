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


@IntegrationTest
class OnboardedProviderViewRepositoryTest {

    // For setup
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;
    @Autowired
    RedirectURLRepository redirectUrlRepository;
    @Autowired
    ClientOnboardedProviderRepository clientOnboardedProviderRepository;
    @Autowired
    ClientGroupOnboardedProviderRepository clientGroupOnboardedProviderRepository;
    @Autowired
    ClientOnboardedScrapingProviderRepository clientOnboardedScrapingProviderRepository;

    // Under test
    @Autowired
    OnboardedProviderViewRepository onboardedProviderViewRepository;

    @Test
    public void testEverything() {
        // set up a ClientOnboardedProvider
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));
        final UUID redirectUrlId = UUID.randomUUID();
        redirectUrlRepository.save(new RedirectURL(clientId, redirectUrlId, "https://example.com"));
        clientOnboardedProviderRepository.save(ClientOnboardedProvider.builder()
                .clientOnboardedProviderId(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                        .clientId(clientId)
                        .redirectUrlId(redirectUrlId)
                        .serviceType(ServiceType.AIS)
                        .provider("TEST")
                        .build())
                .build()
        );

        // set up a ClientGroupOnboardedProvider
        var clientGroupId2 = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId2, "garbage-" + clientGroupId2));
        clientGroupOnboardedProviderRepository.save(ClientGroupOnboardedProvider.builder()
                .clientGroupOnboardedProviderId(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                        .clientGroupId(clientGroupId2)
                        .provider("TEST2")
                        .serviceType(ServiceType.AIS)
                        .build())
                .build()
        );
        var clientId2 = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId2, clientId2));
        final UUID redirectUrlId2 = UUID.randomUUID();
        redirectUrlRepository.save(new RedirectURL(clientId2, redirectUrlId2, "https://example.com"));

        // set up a ClientOnboardedScrapingProvider
        var clientGroupId3 = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId3, "garbage-" + clientGroupId3));
        var clientId3 = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId3, clientId3));
        clientOnboardedScrapingProviderRepository.save(ClientOnboardedScrapingProvider.builder()
                .clientOnboardedScrapingProviderId(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                        .clientId(clientId3)
                        .serviceType(ServiceType.AIS)
                        .provider("TEST3")
                        .build())
                .build()
        );

        // test ClientOnboardedProvider
        var expected = new OnboardedProviderView(clientId, "TEST", ServiceType.AIS, redirectUrlId);
        assertThat(onboardedProviderViewRepository.selectAll()).contains(expected);
        assertThat(onboardedProviderViewRepository.selectAllForClient(clientId)).contains(expected);
        assertThat(onboardedProviderViewRepository.selectAllForClient(UUID.randomUUID())).doesNotContain(expected);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(clientId, "TEST")).contains(expected);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(clientId, "covfefe")).doesNotContain(expected);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(UUID.randomUUID(), "TEST")).doesNotContain(expected);

        // test ClientGroupOnboardedProvider
        var expected2 = new OnboardedProviderView(clientId2, "TEST2", ServiceType.AIS, redirectUrlId2);
        assertThat(onboardedProviderViewRepository.selectAll()).contains(expected2);
        assertThat(onboardedProviderViewRepository.selectAllForClient(clientId2)).contains(expected2);
        assertThat(onboardedProviderViewRepository.selectAllForClient(UUID.randomUUID())).doesNotContain(expected2);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(clientId2, "TEST2")).contains(expected2);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(clientId2, "covfefe")).doesNotContain(expected2);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(UUID.randomUUID(), "TEST2")).doesNotContain(expected2);

        // test ClientOnboardedScrapingProvider
        var expected3 = new OnboardedProviderView(clientId3, "TEST3", ServiceType.AIS, null);
        assertThat(onboardedProviderViewRepository.selectAll()).contains(expected3);
        assertThat(onboardedProviderViewRepository.selectAllForClient(clientId3)).contains(expected3);
        assertThat(onboardedProviderViewRepository.selectAllForClient(UUID.randomUUID())).doesNotContain(expected2);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(clientId3, "TEST3")).contains(expected3);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(clientId3, "covfefe")).doesNotContain(expected3);
        assertThat(onboardedProviderViewRepository.selectAllForClientAndProvider(UUID.randomUUID(), "TEST3")).doesNotContain(expected3);
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
