package com.yolt.clients.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.client.creditoraccounts.AccountIdentifierSchemeEnum;
import com.yolt.clients.client.creditoraccounts.CreditorAccount;
import com.yolt.clients.client.creditoraccounts.CreditorAccountRepository;
import com.yolt.clients.client.ipallowlist.AllowedIP;
import com.yolt.clients.client.ipallowlist.IPAllowListRepository;
import com.yolt.clients.client.outboundallowlist.AllowedOutboundHost;
import com.yolt.clients.client.outboundallowlist.AllowedOutboundHostRepository;
import com.yolt.clients.client.redirecturls.Action;
import com.yolt.clients.client.redirecturls.ChangelogStatus;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogEntry;
import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.client.requesttokenpublickeys.RequestTokenPublicKeyRepository;
import com.yolt.clients.client.requesttokenpublickeys.model.RequestTokenPublicKey;
import com.yolt.clients.client.webhooks.repository.Webhook;
import com.yolt.clients.client.webhooks.repository.WebhookRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.jira.Status;
import com.yolt.clients.jira.dto.IssueResponseDTO;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class DeleteClientServiceIT {

    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private CreditorAccountRepository creditorAccountRepository;
    @Autowired
    private RedirectURLRepository redirectURLRepository;
    @Autowired
    private RedirectURLChangelogRepository redirectURLChangelogRepository;
    @Autowired
    private RequestTokenPublicKeyRepository requestTokenPublicKeyRepository;
    @Autowired
    private WebhookRepository webhookRepository;
    @Autowired
    private AllowedOutboundHostRepository allowedOutboundHostRepository;
    @Autowired
    private IPAllowListRepository ipAllowListRepository;

    private UUID clientId;
    private UUID clientGroupId;
    private ClientToken clientToken;


    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        clientGroupId = UUID.randomUUID();
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "assistance-portal-yts"));
    }

    @Test
    void deleteClient() throws Exception {
        ClientGroup clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(clientId, "test@yolt.eu", "test", "invitationCode", NOW, NOW.plusHours(24));
        Client client = new Client(clientId,
                clientGroupId,
                "Fancy Client",
                "NL",
                true,
                true,
                "12.1",
                4000,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                123L,
                Set.of(clientAdminInvitation));
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        creditorAccountRepository.saveAll(List.of(
                new CreditorAccount(UUID.randomUUID(), clientId, "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null),
                new CreditorAccount(UUID.randomUUID(), clientId, "Creditor Account 2", "Account Number 2", AccountIdentifierSchemeEnum.SORTCODEACCOUNTNUMBER, "Secondary Identification")
        ));

        wireMockServer.stubFor(
                WireMock.get("/users/users?count-by=CLIENT&client-id=" + clientId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("""
                                        {
                                            "count": 0
                                        }
                                        """))
        );

        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/client/details/" + clientId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("""
                                        [{
                                                "id": "47ea5256-6bc0-4f8f-834e-6b7a5379da38",
                                                "name": "admin name",
                                                "email": "em@il.com",
                                                "organisation": "org"
                                        }]
                                        """))
        );

        wireMockServer.stubFor(
                WireMock.delete("/dev-portal/internal-api/clients/" + clientId + "/admins/47ea5256-6bc0-4f8f-834e-6b7a5379da38")
                        .willReturn(aResponse().withStatus(200))
        );

        redirectURLRepository.save(new RedirectURL(clientId, UUID.randomUUID(), "https://url1"));

        redirectURLChangelogRepository.save(new RedirectURLChangelogEntry(UUID.randomUUID(), clientId, NOW, Action.UPDATE, UUID.randomUUID(), "oldURL", "newUrl", "comment", ChangelogStatus.DENIED, null));
        redirectURLChangelogRepository.save(new RedirectURLChangelogEntry(UUID.fromString("6b7450d7-fb94-4869-8e97-96d6c518ecf2"), clientId, NOW, Action.UPDATE, UUID.fromString("279cf30e-c6e4-4475-8743-811f8e016218"), "oldURL", "newUrl", "comment", ChangelogStatus.PENDING, "yt-701"));
        redirectURLChangelogRepository.save(new RedirectURLChangelogEntry(UUID.randomUUID(), clientId, NOW, Action.UPDATE, UUID.randomUUID(), "oldURL", "newUrl", "comment", ChangelogStatus.PROCESSED, null));

        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/yt-701/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        RequestTokenPublicKey requestToken = new RequestTokenPublicKey(clientId, UUID.randomUUID().toString(), "KEY", NOW);
        requestTokenPublicKeyRepository.save(requestToken);

        Webhook webhook = new Webhook(clientId, "https://webhook", true);
        webhookRepository.save(webhook);

        AllowedOutboundHost host1 = new AllowedOutboundHost(UUID.randomUUID(), clientId, "host1", Status.ADDED, NOW);
        AllowedOutboundHost host2 = new AllowedOutboundHost(UUID.randomUUID(), clientId, "host2", Status.PENDING_ADDITION, NOW, "yt-901");
        AllowedOutboundHost host3 = new AllowedOutboundHost(UUID.randomUUID(), clientId, "host3", Status.PENDING_REMOVAL, NOW, "yt-902");
        AllowedOutboundHost host4 = new AllowedOutboundHost(UUID.randomUUID(), clientId, "host4", Status.REMOVED, NOW);
        AllowedOutboundHost host5 = new AllowedOutboundHost(UUID.randomUUID(), clientId, "host5", Status.DENIED, NOW);

        allowedOutboundHostRepository.save(host1);
        allowedOutboundHostRepository.save(host2);
        allowedOutboundHostRepository.save(host3);
        allowedOutboundHostRepository.save(host4);
        allowedOutboundHostRepository.save(host5);

        IssueResponseDTO issueResponseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self/YT-123");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .withRequestBody(equalToJson("""
                                {"fields":{
                                    "project":{"key":"YT"},
                                    "summary":"[test] Request for adding/removing Outbound Hosts to allowlist",
                                    "description":"client id: %s\\nclient name: %s\\n\\nAdd the following outbound hosts to the allowlist:\\n<n/a>\\nremove the following outbound hosts from the allowlist:\\n- host1\\n",
                                    "issuetype":{"name":"Submit a request or incident"},
                                    "customfield_10002":[123],
                                    "customfield_10010":"yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                                }}
                                """.formatted(clientId, "Fancy Client")))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/yt-901/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        AllowedIP ip1 = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.1", Status.ADDED, NOW);
        AllowedIP ip2 = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.2", Status.PENDING_ADDITION, NOW, "yt-801");
        AllowedIP ip3 = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.3", Status.PENDING_REMOVAL, NOW, "yt-802");
        AllowedIP ip4 = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.4", Status.REMOVED, NOW);
        AllowedIP ip5 = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.5", Status.DENIED, NOW);

        ipAllowListRepository.save(ip1);
        ipAllowListRepository.save(ip2);
        ipAllowListRepository.save(ip3);
        ipAllowListRepository.save(ip4);
        ipAllowListRepository.save(ip5);

        IssueResponseDTO issueResponseDTO2 = new IssueResponseDTO("1", "JIRA-456", "https://self/YT-456");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .withRequestBody(equalToJson("""
                                {"fields":{
                                    "project":{"key":"YT"},
                                    "summary":"[test] Request for removing IPs from/to the allowlist",
                                    "description":"client id: %s\\nclient name: %s\\n\\nAdd the following IPs to the allowlist:\\n<n/a>\\n\\nRemove the following IPs from the allowlist:\\n- 127.0.0.1\\n",
                                    "issuetype":{"name":"Submit a request or incident"},
                                    "customfield_10002":[123],
                                    "customfield_10010":"yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                                }}
                                """.formatted(clientId, "Fancy Client")))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO2)))
        );

        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/yt-801/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        IssueResponseDTO issueResponseDTO3 = new IssueResponseDTO("1", "JIRA-789", "https://self/YT-789");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .withRequestBody(equalToJson("""
                                {"fields":{
                                    "project":{"key":"YT"},
                                    "summary":"[test] Delete client: %s",
                                    "description":"client id: %s\\nclient name: %s\\n\\nDelete client: %s\\n    - remove access to JIRA\\n    - remove access to Slack\\n    - clean-up the DN allow list\\n",
                                    "issuetype":{"name":"Submit a request or incident"},
                                    "customfield_10002":[123],
                                    "customfield_10010":"yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                                }}
                                """.formatted(clientId, clientId, "Fancy Client", clientId)))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO3)))
        );

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        var response = testRestTemplate.exchange(
                "/internal/clients/{clientId}",
                HttpMethod.DELETE,
                requestEntity,
                new ParameterizedTypeReference<>() {
                },
                clientId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().untilAsserted(() -> assertThat(clientsRepository.findClientByClientGroupIdAndClientId(clientGroupId, clientId).get().isDeleted()).isTrue());

        await().untilAsserted(() -> assertThat(creditorAccountRepository.getCreditorAccountsByClientId(clientId)).isEmpty());

        await().untilAsserted(() -> assertThat(redirectURLRepository.findAllByClientId(clientId)).isEmpty());
        await().untilAsserted(() -> assertThat(redirectURLChangelogRepository.findFirst20ByClientIdOrderByRequestDateDesc(clientId)).isEmpty());
        await().ignoreExceptions().untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/jira/rest/api/2/issue/yt-701/comment"))
                .withRequestBody(equalToJson("""
                        {
                            "body":"The following redirect URL change requests have been handled:\\n- update url with id: 279cf30e-c6e4-4475-8743-811f8e016218, from: oldURL, to: newUrl (req id: 6b7450d7-fb94-4869-8e97-96d6c518ecf2)\\n"
                        }
                        """))
        ));

        await().untilAsserted(() -> assertThat(requestTokenPublicKeyRepository.findAllByClientId(clientId)).isEmpty());

        await().untilAsserted(() -> assertThat(webhookRepository.findAllByClientId(clientId)).isEmpty());

        await().untilAsserted(() -> {
            assertThat(allowedOutboundHostRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).isEmpty();
        });

        await().ignoreExceptions().untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/jira/rest/api/2/issue/"))
                .withRequestBody(equalToJson("""
                        {"fields":{
                            "project":{"key":"YT"},
                            "summary":"[test] Request for adding/removing Outbound Hosts to allowlist",
                            "description":"client id: %s\\nclient name: %s\\n\\nAdd the following outbound hosts to the allowlist:\\n<n/a>\\nremove the following outbound hosts from the allowlist:\\n- host1\\n",
                            "issuetype":{"name":"Submit a request or incident"},
                            "customfield_10002":[123],
                            "customfield_10010":"yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                        }}
                        """.formatted(clientId, "Fancy Client")))
        ));

        await().ignoreExceptions().untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/jira/rest/api/2/issue/yt-901/comment"))
                .withRequestBody(equalToJson("""
                        {
                            "body":"The following hosts have been handled:\\n- host2\\n"
                        }
                        """))
        ));

        await().untilAsserted(() -> assertThat(ipAllowListRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).isEmpty());

        await().ignoreExceptions().untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/jira/rest/api/2/issue/"))
                .withRequestBody(equalToJson("""
                        {"fields":{
                            "project":{"key":"YT"},
                            "summary":"[test] Request for removing IPs from/to the allowlist",
                            "description":"client id: %s\\nclient name: %s\\n\\nAdd the following IPs to the allowlist:\\n<n/a>\\n\\nRemove the following IPs from the allowlist:\\n- 127.0.0.1\\n",
                            "issuetype":{"name":"Submit a request or incident"},
                            "customfield_10002":[123],
                            "customfield_10010":"yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                        }}
                        """.formatted(clientId, "Fancy Client")))
        ));

        await().ignoreExceptions().untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/jira/rest/api/2/issue/yt-801/comment"))
                .withRequestBody(equalToJson("""
                        {
                            "body":"The following IPs have been handled:\\n- 127.0.0.2\\n"
                        }
                        """))
        ));

        await().ignoreExceptions().untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/jira/rest/api/2/issue/"))
                .withRequestBody(equalToJson("""
                        {"fields":{
                            "project":{"key":"YT"},
                            "summary":"[test] Delete client: %s",
                            "description":"client id: %s\\nclient name: %s\\n\\nDelete client: %s\\n    - remove access to JIRA\\n    - remove access to Slack\\n    - clean-up the DN allow list\\n",
                            "issuetype":{"name":"Submit a request or incident"},
                            "customfield_10002":[123],
                            "customfield_10010":"yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                        }}
                        """.formatted(clientId, clientId, "Fancy Client", clientId)))
        ));
    }

    @Test
    void deleteClient_whileClientHasUsersShouldFail() throws Exception {
        ClientGroup clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(clientId, "test@yolt.eu", "test", "invitationCode", NOW, NOW.plusHours(24));
        Client client = new Client(clientId,
                clientGroupId,
                "client",
                "NL",
                true,
                true,
                "12.1",
                4000,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                123L,
                Set.of(clientAdminInvitation));
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        wireMockServer.stubFor(
                WireMock.get("/users/users?count-by=CLIENT&client-id=" + clientId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("""
                                        {
                                            "count": 1
                                        }
                                        """))
        );

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        var response = testRestTemplate.exchange(
                "/internal/clients/{clientId}",
                HttpMethod.DELETE,
                requestEntity,
                new ParameterizedTypeReference<ErrorDTO>() {
                },
                clientId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS036", "The client still has users."));

        await().untilAsserted(() -> assertThat(clientsRepository.findClientByClientGroupIdAndClientId(clientGroupId, clientId).get().isDeleted()).isFalse());
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}
