package com.yolt.clients.client.redirecturls;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.authmeans.ClientOnboardingsTestUtility;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@IntegrationTest
public class RedirectURLServiceWithNoMockitoTest {

    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;
    @Autowired
    ClientOnboardingsTestUtility clientOnboardingsTestUtility;
    @Autowired
    RedirectURLService redirectURLService;
    @Autowired
    TestClientTokens testClientTokens;

    @Test
    public void given_existingClient_when_addRedirectUrlId_then_OK() {
        // setup
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));

        // when
        var redirectURLId = UUID.randomUUID();
        var token = testClientTokens.createClientToken(clientGroupId, clientId);
        var dto = redirectURLService.create(token, redirectURLId, "https://example.com");

        // then success
        assertThat(dto.getRedirectURLId()).isEqualTo(redirectURLId);
    }

    @Test
    public void given_existingRedirectUrlWithAssociatedBankOnboarding_when_deleteRedirectUrlId_then_KO() {
        // setup
        // add group
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        // add client
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));
        // add redirectURL
        var token = testClientTokens.createClientToken(clientGroupId, clientId);
        UUID redirectUrlId = UUID.randomUUID();
        redirectURLService.create(token, redirectUrlId, "https://example.com");

        // add onboarding & check that it exists
        clientOnboardingsTestUtility.addOnboardedAISProviderForClient(clientId, redirectUrlId, "garbage");

        // try to delete the redirectUrl, which should fail since there is now an onboarding that uses this url
        assertThatCode(() -> redirectURLService.delete(token, redirectUrlId))
                .hasCauseInstanceOf(ConstraintViolationException.class);
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
