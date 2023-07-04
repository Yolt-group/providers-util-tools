package com.yolt.clients.client.ipallowlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ipallowlist.dto.AllowedIPDTO;
import com.yolt.clients.client.ipallowlist.dto.AllowedIPIdListDTO;
import com.yolt.clients.client.ipallowlist.dto.NewAllowedIPsDTO;
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
class IPAllowListControllerIT {

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private IPAllowListRepository ipAllowListRepository;
    @Autowired
    private Clock clock;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private ObjectMapper objectMapper;

    private UUID clientGroupId;
    private UUID clientId;
    private ClientGroup clientGroup;
    private UUID allowedIPId;
    private ClientToken clientToken;

    @BeforeEach
    void setup() {
        clientGroupId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
        var client = new Client(
                clientId,
                clientGroupId,
                "client name",
                "NL",
                false,
                true,
                "10.71",
                null,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                1L,
                Collections.emptySet()
        );
        clientGroup.getClients().add(client);
        allowedIPId = UUID.randomUUID();

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
    }

    @ParameterizedTest
    @CsvSource({
            "other,     GET, ''",           // list
            "other,     POST, ''",          // create
            "other,     POST, '/apply'",    // markApplied
            "dev-portal,POST, '/apply'",    // markApplied
            "other,     POST, '/delete'",   // delete
    })
    void test_invalid_client_token_for_all_uris(
            String issuedForClaim,
            HttpMethod method,
            String subPath
    ) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, issuedForClaim));
        var request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list" + subPath, method, request,
                new ParameterizedTypeReference<>() {
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
        AllowedIP allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.ADDED, LocalDateTime.now(clock));
        ipAllowListRepository.save(allowedIP);

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedIPDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyInAnyOrder(new AllowedIPDTO(allowedIPId, "127.0.0.1/32", LocalDateTime.now(clock), Status.ADDED, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL})
    void create_with_valid_client_token_should_go_ok(String issuedForClaim) throws Exception {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, issuedForClaim));

        UUID allowedIPId1 = UUID.randomUUID();
        UUID allowedIPId2 = UUID.randomUUID();
        UUID allowedIPId3 = UUID.randomUUID();
        UUID allowedIPId4 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        ipAllowListRepository.save(new AllowedIP(allowedIPId, clientId, "127.0.0.0/24", Status.REMOVED, LocalDateTime.now(clock)));
        ipAllowListRepository.save(new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.ADDED, LocalDateTime.now(clock)));
        ipAllowListRepository.save(new AllowedIP(allowedIPId3, clientId, "127.0.0.3/32", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId4, clientId, "127.0.0.4/32", Status.REMOVED, LocalDateTime.now(clock)));

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

        NewAllowedIPsDTO newAllowedIPsDTO = new NewAllowedIPsDTO(Set.of("127.0.0.1/32", "127.0.0.2/32", "127.0.0.3/32", "127.0.0.4/32", "127.0.0.5/32"));
        HttpEntity<NewAllowedIPsDTO> request = new HttpEntity<>(newAllowedIPsDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedIPDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UUID allowedIPId5 = response.getBody().stream()
                .map(AllowedIPDTO::getId)
                .filter(id -> !Set.of(allowedIPId1, allowedIPId2, allowedIPId3, allowedIPId4).contains(id))
                .findAny().orElseThrow(() -> new IllegalStateException("expected a new ID to be present"));

        assertThat(response.getBody()).extracting("id", "cidr", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedIPId1, "127.0.0.1/32", LocalDateTime.now(clock), Status.PENDING_ADDITION, "JIRA-999"),
                Tuple.tuple(allowedIPId2, "127.0.0.2/32", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedIPId3, "127.0.0.3/32", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedIPId4, "127.0.0.4/32", LocalDateTime.now(clock), Status.PENDING_ADDITION, "JIRA-123"),
                Tuple.tuple(allowedIPId5, "127.0.0.5/32", LocalDateTime.now(clock), Status.PENDING_ADDITION, "JIRA-123")
        );

        assertThat(ipAllowListRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedIP(allowedIPId, clientId, "127.0.0.0/24", Status.REMOVED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId3, clientId, "127.0.0.3/32", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId4, clientId, "127.0.0.4/32", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-123"),
                new AllowedIP(allowedIPId5, clientId, "127.0.0.5/32", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-123")
        );
    }

    @Test
    void create_with_too_many_pending_tasks_should_fail() {
        clientGroupRepository.save(clientGroup);
        // create 10 pending tasks
        for (int i = 0; i < 10; i++) {
            ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), clientId, "127.0.0." + i + "/24", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-" + i));
            ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), clientId, "127.0.1." + i + "/24", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-" + i));
        }

        LocalDateTime countAfter = LocalDateTime.now(clock).minusDays(1);
        long numberOfJiraTickets = ipAllowListRepository.findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(clientId, countAfter)
                .stream()
                .map(AllowedIP::getJiraTicket)
                .distinct()
                .count();
        assertThat(numberOfJiraTickets).isEqualTo(10);

        NewAllowedIPsDTO newAllowedIPsDTO = new NewAllowedIPsDTO(Set.of("127.0.0.1/32", "127.0.0.2/32", "127.0.0.3/32", "127.0.0.4/32", "127.0.0.5/32"));
        HttpEntity<NewAllowedIPsDTO> request = new HttpEntity<>(newAllowedIPsDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS019");
    }


    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1/23", "127.0.0.1/33", "bad/32", "256.0.0.0/24", "1.256.0.0/24", "2001:db8:1234:1a00::/64"})
    void create_with_invalid_ipv4_cidr_should_fail(String cidr) {
        clientGroupRepository.save(clientGroup);

        NewAllowedIPsDTO newAllowedIPsDTO = new NewAllowedIPsDTO(Set.of(cidr));
        HttpEntity<NewAllowedIPsDTO> request = new HttpEntity<>(newAllowedIPsDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");
    }

    @Test
    void create_with_too_many_cidr_should_fail() {
        clientGroupRepository.save(clientGroup);

        NewAllowedIPsDTO newAllowedIPsDTO = new NewAllowedIPsDTO(IntStream.range(1, 12).mapToObj(i -> "10." + i + ".0.0/27").collect(Collectors.toUnmodifiableSet()));
        HttpEntity<NewAllowedIPsDTO> request = new HttpEntity<>(newAllowedIPsDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");
    }


    @Test
    void markApplied_with_valid_client_token_should_go_ok() {
        UUID allowedIPId1 = UUID.randomUUID();
        UUID allowedIPId2 = UUID.randomUUID();
        UUID allowedIPId3 = UUID.randomUUID();
        UUID allowedIPId4 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        ipAllowListRepository.save(new AllowedIP(allowedIPId, clientId, "127.0.0.0/24", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.ADDED, LocalDateTime.now(clock)));
        ipAllowListRepository.save(new AllowedIP(allowedIPId3, clientId, "127.0.0.3/32", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId4, clientId, "127.0.0.4/32", Status.REMOVED, LocalDateTime.now(clock)));

        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/JIRA-999/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        AllowedIPIdListDTO allowedIPIdListDTO = new AllowedIPIdListDTO(Set.of(allowedIPId1, allowedIPId2, allowedIPId3, allowedIPId4, UUID.randomUUID()));
        HttpEntity<AllowedIPIdListDTO> request = new HttpEntity<>(allowedIPIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedIPDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list/apply", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getBody()).extracting("id", "cidr", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedIPId1, "127.0.0.1/32", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedIPId2, "127.0.0.2/32", LocalDateTime.now(clock), Status.ADDED, null),
                Tuple.tuple(allowedIPId3, "127.0.0.3/32", LocalDateTime.now(clock), Status.REMOVED, null),
                Tuple.tuple(allowedIPId4, "127.0.0.4/32", LocalDateTime.now(clock), Status.REMOVED, null)
        );

        assertThat(ipAllowListRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedIP(allowedIPId, clientId, "127.0.0.0/24", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.ADDED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId3, clientId, "127.0.0.3/32", Status.REMOVED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId4, clientId, "127.0.0.4/32", Status.REMOVED, LocalDateTime.now(clock), null)
        );
    }

    @Test
    void delete_with_valid_client_token_should_go_ok() throws Exception {
        UUID allowedIPId1 = UUID.randomUUID();
        UUID allowedIPId2 = UUID.randomUUID();
        UUID allowedIPId3 = UUID.randomUUID();
        UUID allowedIPId4 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        ipAllowListRepository.save(new AllowedIP(allowedIPId, clientId, "127.0.0.0/24", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.ADDED, LocalDateTime.now(clock)));
        ipAllowListRepository.save(new AllowedIP(allowedIPId3, clientId, "127.0.0.3/32", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId4, clientId, "127.0.0.4/32", Status.REMOVED, LocalDateTime.now(clock)));

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

        AllowedIPIdListDTO allowedIPIdListDTO = new AllowedIPIdListDTO(Set.of(allowedIPId1, allowedIPId2, allowedIPId3, allowedIPId4, UUID.randomUUID()));
        HttpEntity<AllowedIPIdListDTO> request = new HttpEntity<>(allowedIPIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedIPDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getBody()).extracting("id", "cidr", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedIPId1, "127.0.0.1/32", LocalDateTime.now(clock), Status.REMOVED, null),
                Tuple.tuple(allowedIPId2, "127.0.0.2/32", LocalDateTime.now(clock), Status.PENDING_REMOVAL, "JIRA-123"),
                Tuple.tuple(allowedIPId3, "127.0.0.3/32", LocalDateTime.now(clock), Status.PENDING_REMOVAL, "JIRA-999"),
                Tuple.tuple(allowedIPId4, "127.0.0.4/32", LocalDateTime.now(clock), Status.REMOVED, null)
        );

        assertThat(ipAllowListRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedIP(allowedIPId, clientId, "127.0.0.0/24", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.REMOVED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-123"),
                new AllowedIP(allowedIPId3, clientId, "127.0.0.3/32", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-999"),
                new AllowedIP(allowedIPId4, clientId, "127.0.0.4/32", Status.REMOVED, LocalDateTime.now(clock), null)
        );
    }

    @Test
    void markDenied_with_valid_client_token_should_go_ok() {
        UUID allowedIPId1 = UUID.randomUUID();
        UUID allowedIPId2 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        ipAllowListRepository.save(new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-999"));
        ipAllowListRepository.save(new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.DENIED, LocalDateTime.now(clock)));

        AllowedIPIdListDTO allowedIPIdListDTO = new AllowedIPIdListDTO(Set.of(allowedIPId1, allowedIPId2));
        HttpEntity<AllowedIPIdListDTO> request = new HttpEntity<>(allowedIPIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<AllowedIPDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list/deny", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getBody()).extracting("id", "cidr", "lastUpdated", "status", "jiraTicket").containsExactlyInAnyOrder(
                Tuple.tuple(allowedIPId1, "127.0.0.1/32", LocalDateTime.now(clock), Status.DENIED, null),
                Tuple.tuple(allowedIPId2, "127.0.0.2/32", LocalDateTime.now(clock), Status.DENIED, null)
        );

        assertThat(ipAllowListRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", Status.DENIED, LocalDateTime.now(clock), null),
                new AllowedIP(allowedIPId2, clientId, "127.0.0.2/32", Status.DENIED, LocalDateTime.now(clock), null)
        );
    }


    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"ADDED", "PENDING_REMOVAL", "REMOVED"})
    void markDenied_with_invalid_status_should_fail(Status status) {
        UUID allowedIPId1 = UUID.randomUUID();

        clientGroupRepository.save(clientGroup);
        ipAllowListRepository.save(new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", status, LocalDateTime.now(clock), "JIRA-999"));

        AllowedIPIdListDTO allowedIPIdListDTO = new AllowedIPIdListDTO(Set.of(allowedIPId1));
        HttpEntity<AllowedIPIdListDTO> request = new HttpEntity<>(allowedIPIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list/deny", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS020");

        assertThat(ipAllowListRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId)).containsExactlyInAnyOrder(
                new AllowedIP(allowedIPId1, clientId, "127.0.0.1/32", status, LocalDateTime.now(clock), "JIRA-999")
        );
    }


    @Test
    void delete_with_too_many_pending_tasks_should_fail() {
        clientGroupRepository.save(clientGroup);
        // create 10 pending tasks
        for (int i = 0; i < 10; i++) {
            ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), clientId, "127.0.0." + i + "/24", Status.PENDING_ADDITION, LocalDateTime.now(clock), "JIRA-" + i));
            ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), clientId, "127.0.1." + i + "/24", Status.PENDING_REMOVAL, LocalDateTime.now(clock), "JIRA-" + i));
        }

        LocalDateTime countAfter = LocalDateTime.now(clock).minusDays(1);
        long numberOfJiraTickets = ipAllowListRepository.findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(clientId, countAfter)
                .stream()
                .map(AllowedIP::getJiraTicket)
                .distinct()
                .count();
        assertThat(numberOfJiraTickets).isEqualTo(10);

        AllowedIPIdListDTO allowedIPIdListDTO = new AllowedIPIdListDTO(Set.of(UUID.randomUUID()));
        HttpEntity<AllowedIPIdListDTO> request = new HttpEntity<>(allowedIPIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/ip-allow-list/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
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
