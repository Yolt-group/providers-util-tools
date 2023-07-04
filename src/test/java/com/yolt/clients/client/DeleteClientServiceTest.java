package com.yolt.clients.client;

import com.yolt.clients.client.admins.ClientAdminService;
import com.yolt.clients.client.admins.models.ClientAdminDTO;
import com.yolt.clients.client.creditoraccounts.AccountIdentifierSchemeEnum;
import com.yolt.clients.client.creditoraccounts.CreditorAccountDTO;
import com.yolt.clients.client.creditoraccounts.CreditorAccountService;
import com.yolt.clients.client.ipallowlist.IPAllowListService;
import com.yolt.clients.client.outboundallowlist.OutboundAllowListService;
import com.yolt.clients.client.redirecturls.RedirectURLChangelogService;
import com.yolt.clients.client.redirecturls.RedirectURLService;
import com.yolt.clients.client.redirecturls.dto.RedirectURLDTO;
import com.yolt.clients.client.requesttokenpublickeys.RequestTokenPublicKeyService;
import com.yolt.clients.client.requesttokenpublickeys.dto.RequestTokenPublicKeyDTO;
import com.yolt.clients.client.webhooks.WebhookService;
import com.yolt.clients.client.webhooks.dto.WebhookDTO;
import com.yolt.clients.events.ClientDeletedEvent;
import com.yolt.clients.jira.JiraService;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DeleteClientServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private ClientService clientService;
    @Mock
    private UsersClient usersClient;
    @Mock
    private ClientAdminService clientAdminService;
    @Mock
    private CreditorAccountService creditorAccountService;
    @Mock
    private RedirectURLService redirectURLService;
    @Mock
    private RedirectURLChangelogService redirectURLChangelogService;
    @Mock
    private RequestTokenPublicKeyService requestTokenPublicKeyService;
    @Mock
    private WebhookService webhookService;
    @Mock
    private OutboundAllowListService outboundAllowListService;
    @Mock
    private IPAllowListService ipAllowListService;
    @Mock
    private JiraService jiraService;
    @Mock
    private ClientDeletedEventProducer clientDeletedEventProducer;

    @InjectMocks
    private DeleteClientService deleteClientService;

    @Test
    void deleteClient() {
        UUID clientId = UUID.randomUUID();
        UUID clientGroupId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims();
        String serializedClientToken = "ct-" + clientId;
        claims.setStringClaim("client-id", clientId.toString());
        claims.setStringClaim("client-group-id", clientGroupId.toString());
        claims.setStringClaim("isf", "dev-portal");
        claims.setClaim("deleted", false);

        ClientToken clientToken = new ClientToken(serializedClientToken, claims);

        when(usersClient.getCount(clientToken)).thenReturn(0L);

        var admin = new ClientAdminDTO(UUID.randomUUID(), "portalUser1", "org", "p1@ma.il", UUID.randomUUID(), "invite1", "i1@ma.il", NOW.minusHours(25), NOW.minusHours(4), NOW.minusHours(1), ClientAdminDTO.InviteStatus.USED);
        var invite = new ClientAdminDTO(null, "portalUser2", "org", "p2@ma.il", UUID.randomUUID(), "invite2", "i2@ma.il", NOW.minusHours(25), NOW.minusHours(4), NOW.minusHours(1), ClientAdminDTO.InviteStatus.VALID);

        when(clientAdminService.getClientAdmins(clientToken)).thenReturn(List.of(admin, invite));

        var creditorAccount1 = new CreditorAccountDTO(UUID.randomUUID(), "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null);
        var creditorAccount2 = new CreditorAccountDTO(UUID.randomUUID(), "Creditor Account 2", "Account Number 2", AccountIdentifierSchemeEnum.SORTCODEACCOUNTNUMBER, "Secondary Identification");
        when(creditorAccountService.getCreditorAccounts(clientToken.getClientIdClaim())).thenReturn(List.of(creditorAccount1, creditorAccount2));

        RedirectURLDTO url1 = new RedirectURLDTO(UUID.randomUUID(), "https://url1");
        RedirectURLDTO url2 = new RedirectURLDTO(UUID.randomUUID(), "https://url2");
        when(redirectURLService.findAll(any())).thenReturn(List.of(url1, url2));

        RequestTokenPublicKeyDTO token1 = new RequestTokenPublicKeyDTO(clientId, "key1", "pub1", null, null);
        RequestTokenPublicKeyDTO token2 = new RequestTokenPublicKeyDTO(clientId, "key2", "pub2", null, null);
        when(requestTokenPublicKeyService.getRequestTokenPublicKeys(any())).thenReturn(List.of(token1, token2));

        WebhookDTO webhook1 = new WebhookDTO("https://url1", true);
        WebhookDTO webhook2 = new WebhookDTO("https://url2", false);
        when(webhookService.findAll(any())).thenReturn(List.of(webhook1, webhook2));

        deleteClientService.deleteClient(clientToken);

        verify(usersClient).getCount(clientToken);
        verify(clientAdminService).getClientAdmins(clientToken);
        verify(clientAdminService).removeAuthorization(clientToken, admin.getPortalUserId());
        verify(creditorAccountService).removeCreditorAccount(clientToken, creditorAccount1.getId());
        verify(creditorAccountService).removeCreditorAccount(clientToken, creditorAccount2.getId());
        verify(clientAdminService).removeAuthorization(clientToken, admin.getPortalUserId());
        verify(clientService).markClientDeleted(clientToken);
        verify(clientDeletedEventProducer).sendMessage(clientToken, new ClientDeletedEvent(clientId, clientGroupId));
        verify(redirectURLService).findAll(clientId);
        verify(redirectURLService).delete(clientToken, url1.getRedirectURLId());
        verify(redirectURLService).delete(clientToken, url2.getRedirectURLId());
        verify(redirectURLChangelogService).delete(clientToken);
        verify(requestTokenPublicKeyService).getRequestTokenPublicKeys(clientId);
        verify(requestTokenPublicKeyService).deleteRequestTokenPublicKey(clientToken, token1.getKeyId());
        verify(requestTokenPublicKeyService).deleteRequestTokenPublicKey(clientToken, token2.getKeyId());
        verify(webhookService).findAll(clientId);
        verify(webhookService).delete(clientToken, webhook1.getUrl());
        verify(webhookService).delete(clientToken, webhook2.getUrl());
        verify(outboundAllowListService).delete(clientToken);
        verify(ipAllowListService).delete(clientToken);
        verify(jiraService).createIssue(
                clientToken,
                """
                        Delete client: %s
                            - remove access to JIRA
                            - remove access to Slack
                            - clean-up the DN allow list
                        """.formatted(clientId),
                "Delete client: %s".formatted(clientId)
        );
    }

    @Test
    void deleteClient_withExistingUsers_shouldFail() {
        UUID clientId = UUID.randomUUID();
        UUID clientGroupId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims();
        String serializedClientToken = "ct-" + clientId;
        claims.setStringClaim("client-id", clientId.toString());
        claims.setStringClaim("client-group-id", clientGroupId.toString());
        claims.setStringClaim("isf", "dev-portal");
        claims.setClaim("deleted", false);

        ClientToken clientToken = new ClientToken(serializedClientToken, claims);

        when(usersClient.getCount(clientToken)).thenReturn(1L);

        assertThrows(ClientHasUsersException.class, () -> deleteClientService.deleteClient(clientToken));

        verify(usersClient).getCount(clientToken);
    }
}