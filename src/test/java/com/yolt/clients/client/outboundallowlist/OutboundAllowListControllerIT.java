package com.yolt.clients.client.outboundallowlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.outboundallowlist.dto.AllowedOutboundHostDTO;
import com.yolt.clients.client.outboundallowlist.dto.AllowedOutboundHostIdListDTO;
import com.yolt.clients.client.outboundallowlist.dto.NewAllowedOutboundHostsDTO;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.jira.Status;
import com.yolt.clients.jira.dto.IssueResponseDTO;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class OutboundAllowListControllerIT {
    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private AllowedOutboundHostRepository allowedOutboundHostRepository;
    @Autowired
    private Clock clock;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private ObjectMapper objectMapper;

    private UUID clientGroupId;
    private UUID clientId;
    private ClientGroup clientGroup;
    private Client client;
    private ClientToken clientToken;
    private String allowedOutboundHost;
    private UUID allowedOutboundHostId;

    @BeforeEach
    void setup() {
        clientGroupId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
        client = new Client(
                clientId,
                clientGroupId,
                "outbound client",
                null,
                false,
                false,
                "10.71",
                null,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                false,
                false,
                true,
                true,
                true,
                3L,
                Collections.emptySet()
        );
        clientGroup.getClients().add(client);
        allowedOutboundHost = "my.host.rest";
        allowedOutboundHostId = UUID.randomUUID();

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
    }

    @ParameterizedTest
    @CsvSource({
            "other,         '',         GET",   // list
            "other,         '',         POST",  // create
            "other,         '/apply', POST",  // markApplied
            "dev-portal,    '/apply', POST",  // markApplied
            "other,         '/delete', POST",   // delete
    })
    void test_invalid_client_token_for_all_uris(
            String issuedForClaim,
            String subPath,
            HttpMethod method
    ) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, issuedForClaim));
        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list" + subPath, method, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS9002");
    }

    @ParameterizedTest
    @ValueSource(strings = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL})
    void list_with_valid_client_token_should_go_ok(String issuedForClaim) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, issuedForClaim));
        clientGroupRepository.save(clientGroup);
        AllowedOutboundHost allowedOutboundHost = new AllowedOutboundHost(allowedOutboundHostId, clientId, OutboundAllowListControllerIT.this.allowedOutboundHost, Status.ADDED, LocalDateTime.now(clock));
        allowedOutboundHostRepository.save(allowedOutboundHost);

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedOutboundHostDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyInAnyOrder(new AllowedOutboundHostDTO(allowedOutboundHostId, OutboundAllowListControllerIT.this.allowedOutboundHost, LocalDateTime.now(clock), Status.ADDED, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL})
    void create_with_valid_client_token_should_go_ok(String issuedForClaim) throws Exception {
        UUID allowedOutboundHostId1 = UUID.randomUUID();
        UUID allowedOutboundHostId2 = UUID.randomUUID();
        UUID allowedOutboundHostId3 = UUID.randomUUID();
        UUID allowedOutboundHostId4 = UUID.randomUUID();

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, issuedForClaim));

        clientGroupRepository.save(clientGroup);
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId, clientId, allowedOutboundHost, Status.REMOVED, LocalDateTime.now(clock)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.ADDED, LocalDateTime.now(clock)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId3, clientId, "my.host.c.rest", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId4, clientId, "my.host.d.rest", Status.REMOVED, LocalDateTime.now(clock)));

        IssueResponseDTO issueResponseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self/YT-123");
        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/JIRA-999/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        NewAllowedOutboundHostsDTO newAllowedOutboundHostsDTO = new NewAllowedOutboundHostsDTO(Set.of("my.host.a.rest", "my.host.b.rest", "my.host.c.rest", "my.host.d.rest", "my.host.e.rest"));
        HttpEntity<NewAllowedOutboundHostsDTO> request = new HttpEntity<>(newAllowedOutboundHostsDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedOutboundHostDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UUID allowedOutboundHostId5 = response.getBody().stream()
                .map(AllowedOutboundHostDTO::getId)
                .filter(id -> !Set.of(allowedOutboundHostId1, allowedOutboundHostId2, allowedOutboundHostId3, allowedOutboundHostId4).contains(id))
                .findAny().orElseThrow(() -> new IllegalStateException("expected a new ID to be present"));

        assertThat(response.getBody()).extracting("id", "host", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedOutboundHostId1, "my.host.a.rest", LocalDateTime.now(clock), Status.PENDING_ADDITION, "JIRA-999"),
                Tuple.tuple(allowedOutboundHostId2, "my.host.b.rest", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedOutboundHostId3, "my.host.c.rest", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedOutboundHostId4, "my.host.d.rest", LocalDateTime.now(clock), Status.PENDING_ADDITION, "JIRA-123"),
                Tuple.tuple(allowedOutboundHostId5, "my.host.e.rest", LocalDateTime.now(clock), Status.PENDING_ADDITION, "JIRA-123")
        );

        assertThat(allowedOutboundHostRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedOutboundHost(allowedOutboundHostId, clientId, allowedOutboundHost, Status.REMOVED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId3, clientId, "my.host.c.rest", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId4, clientId, "my.host.d.rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-123"),
                new AllowedOutboundHost(allowedOutboundHostId5, clientId, "my.host.e.rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-123")
        );
    }

    @Test
    void create_with_too_many_pending_tasks_should_fail() {
        clientGroupRepository.save(clientGroup);
        // create 10 pending tasks
        for (int i = 0; i < 10; i++) {
            allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), clientId, "my.host.a" + i + ".rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-" + i));
            allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), clientId, "my.host.b" + i + ".rest", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-" + i));
        }

        LocalDateTime countAfter = LocalDateTime.now(clock).minusDays(1);
        long numberOfJiraTickets = allowedOutboundHostRepository.findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(clientId, countAfter)
                .stream()
                .map(AllowedOutboundHost::getJiraTicket)
                .distinct()
                .count();
        assertThat(numberOfJiraTickets).isEqualTo(10);

        NewAllowedOutboundHostsDTO newAllowedOutboundHostsDTO = new NewAllowedOutboundHostsDTO(Set.of(allowedOutboundHost, "my.host.a.rest", "my.host.b.rest", "my.host.c.rest", "my.host.d.rest"));
        HttpEntity<NewAllowedOutboundHostsDTO> request = new HttpEntity<>(newAllowedOutboundHostsDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS019");
    }


    @ParameterizedTest
    @ValueSource(strings = {"ab.com/de", "http://my.host.rest", "https://my.host.rest", "my.host.rest:8443", "1.256.0.0", "2001:db8:1234:1a00::"})
    void create_with_invalid_host_should_fail(String host) {
        clientGroupRepository.save(clientGroup);

        NewAllowedOutboundHostsDTO newAllowedOutboundHostsDTO = new NewAllowedOutboundHostsDTO(Set.of(host));
        HttpEntity<NewAllowedOutboundHostsDTO> request = new HttpEntity<>(newAllowedOutboundHostsDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");
    }

    @Test
    void create_with_too_many_host_should_fail() {
        clientGroupRepository.save(clientGroup);

        NewAllowedOutboundHostsDTO newAllowedOutboundHostsDTO = new NewAllowedOutboundHostsDTO(IntStream.range(1, 12).mapToObj(i -> "host" + i + ".tld").collect(Collectors.toUnmodifiableSet()));
        HttpEntity<NewAllowedOutboundHostsDTO> request = new HttpEntity<>(newAllowedOutboundHostsDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");
    }

    @Test
    void markApplied_with_valid_client_token_should_go_ok() {
        UUID allowedOutboundHostId1 = UUID.randomUUID();
        UUID allowedOutboundHostId2 = UUID.randomUUID();
        UUID allowedOutboundHostId3 = UUID.randomUUID();
        UUID allowedOutboundHostId4 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId, clientId, allowedOutboundHost, Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.ADDED, LocalDateTime.now(clock)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId3, clientId, "my.host.c.rest", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId4, clientId, "my.host.d.rest", Status.REMOVED, LocalDateTime.now(clock)));

        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/JIRA-999/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        var allowedOutboundHostIdListDTO = new AllowedOutboundHostIdListDTO(Set.of(
                allowedOutboundHostId1,
                allowedOutboundHostId2,
                allowedOutboundHostId3,
                allowedOutboundHostId4,
                UUID.randomUUID()));
        HttpEntity<AllowedOutboundHostIdListDTO> request = new HttpEntity<>(allowedOutboundHostIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedOutboundHostDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list/apply", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getBody()).extracting("id", "host", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedOutboundHostId1, "my.host.a.rest", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedOutboundHostId2, "my.host.b.rest", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedOutboundHostId3, "my.host.c.rest", LocalDateTime.now(clock), Status.REMOVED, null),
                Tuple.tuple(allowedOutboundHostId4, "my.host.d.rest", LocalDateTime.now(clock), Status.REMOVED, null)
        );

        assertThat(allowedOutboundHostRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedOutboundHost(allowedOutboundHostId, clientId, allowedOutboundHost, Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId3, clientId, "my.host.c.rest", Status.REMOVED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId4, clientId, "my.host.d.rest", Status.REMOVED, LocalDateTime.now(clock), null)
        );
    }

    @Test
    void delete_with_valid_client_token_should_go_ok() throws Exception {
        UUID allowedOutboundHostId1 = UUID.randomUUID();
        UUID allowedOutboundHostId2 = UUID.randomUUID();
        UUID allowedOutboundHostId3 = UUID.randomUUID();
        UUID allowedOutboundHostId4 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId, clientId, allowedOutboundHost, Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.ADDED, LocalDateTime.now(clock)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId3, clientId, "my.host.c.rest", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId4, clientId, "my.host.d.rest", Status.REMOVED, LocalDateTime.now(clock)));

        IssueResponseDTO issueResponseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self/YT-123");
        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/JIRA-999/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        var allowedOutboundHostIdListDTO = new AllowedOutboundHostIdListDTO(Set.of(
                allowedOutboundHostId1,
                allowedOutboundHostId2,
                allowedOutboundHostId3,
                allowedOutboundHostId4,
                UUID.randomUUID()));
        HttpEntity<AllowedOutboundHostIdListDTO> request = new HttpEntity<>(allowedOutboundHostIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedOutboundHostDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getBody()).extracting("id", "host", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedOutboundHostId1, "my.host.a.rest", LocalDateTime.now(clock), Status.REMOVED, null),
                Tuple.tuple(allowedOutboundHostId2, "my.host.b.rest", LocalDateTime.now(clock), Status.PENDING_REMOVAL, "JIRA-123"),
                Tuple.tuple(allowedOutboundHostId3, "my.host.c.rest", LocalDateTime.now(clock), Status.PENDING_REMOVAL, "JIRA-999"),
                Tuple.tuple(allowedOutboundHostId4, "my.host.d.rest", LocalDateTime.now(clock), Status.REMOVED, null)
        );

        assertThat(allowedOutboundHostRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedOutboundHost(allowedOutboundHostId, clientId, allowedOutboundHost, Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.REMOVED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-123"),
                new AllowedOutboundHost(allowedOutboundHostId3, clientId, "my.host.c.rest", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedOutboundHost(allowedOutboundHostId4, clientId, "my.host.d.rest", Status.REMOVED, LocalDateTime.now(clock), null)
        );
    }

    @Test
    void markDenied_with_valid_client_token_should_go_ok() {
        UUID allowedOutboundHostId1 = UUID.randomUUID();
        UUID allowedOutboundHostId2 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.DENIED, LocalDateTime.now(clock)));

        var allowedOutboundHostIdListDTO = new AllowedOutboundHostIdListDTO(Set.of(allowedOutboundHostId1, allowedOutboundHostId2));
        HttpEntity<AllowedOutboundHostIdListDTO> request = new HttpEntity<>(allowedOutboundHostIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedOutboundHostDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list/deny", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getBody()).extracting("id", "host", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedOutboundHostId1, "my.host.a.rest", LocalDateTime.now(clock), Status.DENIED, null),
                Tuple.tuple(allowedOutboundHostId2, "my.host.b.rest", LocalDateTime.now(clock), Status.DENIED, null)
        );

        assertThat(allowedOutboundHostRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", Status.DENIED, LocalDateTime.now(clock), null),
                new AllowedOutboundHost(allowedOutboundHostId2, clientId, "my.host.b.rest", Status.DENIED, LocalDateTime.now(clock), null)
        );
    }


    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"ADDED", "PENDING_REMOVAL", "REMOVED"})
    void markDenied_with_invalid_status_should_fail(Status status) {
        UUID allowedOutboundHostId1 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        allowedOutboundHostRepository.save(new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", status, LocalDateTime.now(clock), "JIRA-999"));

        var allowedOutboundHostIdListDTO = new AllowedOutboundHostIdListDTO(Set.of(allowedOutboundHostId1));
        HttpEntity<AllowedOutboundHostIdListDTO> request = new HttpEntity<>(allowedOutboundHostIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list/deny", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS029");

        assertThat(allowedOutboundHostRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedOutboundHost(allowedOutboundHostId1, clientId, "my.host.a.rest", status, LocalDateTime.now(clock), "JIRA-999")
        );
    }


    @Test
    void delete_with_too_many_pending_tasks_should_fail() {
        clientGroupRepository.save(clientGroup);
        // create 10 pending tasks
        for (int i = 0; i < 10; i++) {
            allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), clientId, "my.host.a" + i + ".rest", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-" + i));
            allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), clientId, "my.host.b" + i + ".rest", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-" + i));
        }

        LocalDateTime countAfter = LocalDateTime.now(clock).minusDays(1);
        long numberOfJiraTickets = allowedOutboundHostRepository.findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(clientId, countAfter)
                .stream()
                .map(AllowedOutboundHost::getJiraTicket)
                .distinct()
                .count();
        assertThat(numberOfJiraTickets).isEqualTo(10);

        var allowedOutboundHostIdListDTO = new AllowedOutboundHostIdListDTO(Set.of(UUID.randomUUID()));
        HttpEntity<AllowedOutboundHostIdListDTO> request = new HttpEntity<>(allowedOutboundHostIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/outbound-allow-list/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS019");
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}
