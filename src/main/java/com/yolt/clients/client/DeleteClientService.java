package com.yolt.clients.client;

import com.yolt.clients.client.admins.ClientAdminService;
import com.yolt.clients.client.admins.models.ClientAdminDTO;
import com.yolt.clients.client.creditoraccounts.CreditorAccountService;
import com.yolt.clients.client.ipallowlist.IPAllowListService;
import com.yolt.clients.client.outboundallowlist.OutboundAllowListService;
import com.yolt.clients.client.redirecturls.RedirectURLChangelogService;
import com.yolt.clients.client.redirecturls.RedirectURLService;
import com.yolt.clients.client.requesttokenpublickeys.RequestTokenPublicKeyService;
import com.yolt.clients.client.webhooks.WebhookService;
import com.yolt.clients.events.ClientDeletedEvent;
import com.yolt.clients.jira.JiraService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteClientService {

    private final ClientService clientService;
    private final UsersClient usersClient;
    private final ClientAdminService clientAdminService;
    private final CreditorAccountService creditorAccountService;

    private final RedirectURLService redirectURLService;
    private final RedirectURLChangelogService redirectURLChangelogService;
    private final RequestTokenPublicKeyService requestTokenPublicKeyService;
    private final WebhookService webhookService;

    private final OutboundAllowListService outboundAllowListService;
    private final IPAllowListService ipAllowListService;
    private final JiraService jiraService;

    private final ClientDeletedEventProducer clientDeletedEventProducer;

    public void deleteClient(@Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientToken clientToken) {
        var clientId = clientToken.getClientIdClaim();
        var clientGroupId = clientToken.getClientGroupIdClaim();

        var usersCount = usersClient.getCount(clientToken);
        if (usersCount > 0) {
            throw new ClientHasUsersException(clientId, usersCount);
        }

        // remove client admins
        clientAdminService.getClientAdmins(clientToken).stream()
                .filter(ClientAdminDTO::hasPortalUserId)
                .forEach(admin -> clientAdminService.removeAuthorization(clientToken, admin.getPortalUserId()));

        // remove creditor accounts
        creditorAccountService.getCreditorAccounts(clientToken.getClientIdClaim())
                .forEach(creditorAccount -> creditorAccountService.removeCreditorAccount(clientToken, creditorAccount.getId()));

        // remove public keys
        requestTokenPublicKeyService.getRequestTokenPublicKeys(clientId)
                .forEach(key -> requestTokenPublicKeyService.deleteRequestTokenPublicKey(clientToken, key.getKeyId()));

        // mark client deleted
        clientService.markClientDeleted(clientToken);

        // let everyone know we deleted the client
        clientDeletedEventProducer.sendMessage(clientToken, new ClientDeletedEvent(clientId, clientGroupId));

        // remove redirect urls
        redirectURLService.findAll(clientId)
                .forEach(url -> redirectURLService.delete(clientToken, url.getRedirectURLId()));
        redirectURLChangelogService.delete(clientToken);

        // remove webhooks
        webhookService.findAll(clientId)
                .forEach(webhook -> webhookService.delete(clientToken, webhook.getUrl()));

        // create tasks to clean up outbound domains
        outboundAllowListService.delete(clientToken);

        // create tasks to clean up inbound IPs
        ipAllowListService.delete(clientToken);

        // TODO add cleanup job for DN allow list

        // create tasks to clean up other systems
        jiraService.createIssue(
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

    @Value
    public static class DeleteClientEvent {
        UUID clientId;
    }
}
