package com.yolt.clients.client.redirecturls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.dto.NewAddRequestDTO;
import com.yolt.clients.client.redirecturls.dto.NewDeleteRequestDTO;
import com.yolt.clients.client.redirecturls.dto.NewUpdateRequestDTO;
import com.yolt.clients.client.redirecturls.dto.RedirectURLChangeRequestListDTO;
import com.yolt.clients.client.redirecturls.dto.RedirectURLChangelogDTO;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogEntry;
import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.jira.dto.IssueResponseDTO;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class RedirectURLChangelogControllerIT {

    private static final String DEV_PORTAL = "dev-portal";

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";

    private final static Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1_627_465_201L), ZoneOffset.UTC); // Wednesday, 28 July 2021 11:40:01 GMT+02:00 DST

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private RedirectURLRepository urlRepository;

    @Autowired
    private RedirectURLChangelogRepository changeRequestRepository;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clientGroupId;
    private UUID clientId;
    private UUID redirectURLId;
    private String redirectURL;
    private ClientToken clientToken;

    @BeforeEach
    void setup() {
        clientId = UUID.randomUUID();
        clientGroupId = UUID.randomUUID();
        redirectURLId = UUID.randomUUID();
        redirectURL = "https://junit.test";

        var clientGroup = new ClientGroup(clientGroupId, "clientGroupRedirectURL");
        clientGroupRepository.save(clientGroup);

        var client = new Client(
                clientId,
                clientGroupId,
                "client Redirect URL",
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
        clientsRepository.save(client);

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, DEV_PORTAL));
    }

    @ParameterizedTest
    @CsvSource({
            "GET,''",                   // list
            "POST,'/add-request'",      // create add request
            "POST,'/update-request'",   // create update request
            "POST,'/delete-request'",   // create delete request
    })
    void givenInvalidClientToken_shouldFail(HttpMethod method, String subPath) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "not-dev-portal"));
        var request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog" + subPath, method, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS9002");
    }

    @Test
    void testGetLatestChangelogEntries() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var urlId2 = UUID.randomUUID();
        var url2 = "https://some.other.url";
        changeRequestRepository.save(new RedirectURLChangelogEntry(id1, clientId, now(CLOCK), Action.CREATE, redirectURLId, null, "https://new-url.com", "Adding new URL.", ChangelogStatus.PENDING, "JIRA-111"));
        changeRequestRepository.save(new RedirectURLChangelogEntry(id2, clientId, now(CLOCK), Action.DELETE, urlId2, redirectURL, url2, "URL deleted.", ChangelogStatus.PROCESSED, "JIRA-123"));

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<List<RedirectURLChangelogDTO>> response = testRestTemplate.exchange(
                "/internal/clients/" + clientId + "/redirect-urls-changelog",
                HttpMethod.GET, request, new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).contains(new RedirectURLChangelogDTO(id1, now(CLOCK), Action.CREATE, redirectURLId, null, "https://new-url.com", ChangelogStatus.PENDING, "JIRA-111"));
        assertThat(response.getBody()).contains(new RedirectURLChangelogDTO(id2, now(CLOCK), Action.DELETE, urlId2, redirectURL, url2, ChangelogStatus.PROCESSED, "JIRA-123"));
    }

    @Test
    void givenMoreThan20RecordsInDB_whenFetchLatest20_thenReturnsLatest20ChangelogEntriesOrderByRequestDateDesc() {
        for (int i = 0; i < 25; i++) {
            var entry = new RedirectURLChangelogEntry(
                    UUID.randomUUID(), clientId, now(CLOCK).plusDays(i), Action.CREATE,
                    UUID.randomUUID(), null, "https://new-url-%s.com".formatted(i), "Comment %s".formatted(i), ChangelogStatus.PENDING, "JIRA-%s".formatted(i));
            changeRequestRepository.save(entry);
            System.out.println(entry);
        }

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<List<RedirectURLChangelogDTO>> response = testRestTemplate.exchange(
                "/internal/clients/" + clientId + "/redirect-urls-changelog",
                HttpMethod.GET, request, new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(20);
        for (int i=0; i < 19; i++) {
            var younger = Objects.requireNonNull(response.getBody()).get(i);
            var older = Objects.requireNonNull(response.getBody()).get(i + 1);
            assertThat(younger.getRequestDate().isAfter(older.getRequestDate())).isTrue();
        }
        assertThat(Objects.requireNonNull(response.getBody()).get(19).getRequestDate()).isEqualTo(now(CLOCK).plusDays(5));
    }

    @Test
    void givenValidPayload_whenCreateAddRequest_shouldSucceed() throws Exception {
        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        var requestDTO = new NewAddRequestDTO(redirectURL, "Some comment.");
        var request = new HttpEntity<>(requestDTO, getHttpHeaders(clientToken));
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/add-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getRequestDate()).isNotNull();
        assertThat(response.getBody().getAction()).isEqualTo(Action.CREATE);
        assertThat(response.getBody().getRedirectURLId()).isNotNull();
        assertThat(response.getBody().getRedirectURL()).isNull();
        assertThat(response.getBody().getNewRedirectURL()).isEqualTo(redirectURL);
        assertThat(response.getBody().getStatus()).isEqualTo(ChangelogStatus.PENDING);
        assertThat(response.getBody().getJiraTicket()).isEqualTo("JIRA-111");
    }

    @Test
    void givenExistingRedirectURL_whenCreateAddRequest_shouldReturn400() throws Exception {
        urlRepository.save(new RedirectURL(clientId, redirectURLId, redirectURL));

        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        var requestDTO = new NewAddRequestDTO(redirectURL, "Some comment.");
        var request = new HttpEntity<>(requestDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/add-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void givenNonLowercaseRedirectURLButSimilarToExistingOne_whenCreateAddRequest_shouldReturn400() throws Exception {
        urlRepository.save(new RedirectURL(clientId, redirectURLId, "https://some-url.com"));

        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        var requestDTO = new NewAddRequestDTO("https://Some-URL.com", "Some comment.");
        var request = new HttpEntity<>(requestDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/add-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void givenValidPayload_whenCreateUpdateRequest_shouldSucceed() throws Exception {
        urlRepository.save(new RedirectURL(clientId, redirectURLId, redirectURL));

        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        var requestDTO = new NewUpdateRequestDTO(redirectURLId, "https://new-url.org", "Some new comment.");
        var request = new HttpEntity<>(requestDTO, getHttpHeaders(clientToken));
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/update-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getRequestDate()).isNotNull();
        assertThat(response.getBody().getAction()).isEqualTo(Action.UPDATE);
        assertThat(response.getBody().getRedirectURLId()).isEqualTo(redirectURLId);
        assertThat(response.getBody().getRedirectURL()).isEqualTo(redirectURL);
        assertThat(response.getBody().getNewRedirectURL()).isEqualTo("https://new-url.org");
        assertThat(response.getBody().getStatus()).isEqualTo(ChangelogStatus.PENDING);
        assertThat(response.getBody().getJiraTicket()).isEqualTo("JIRA-111");
    }

    @Test
    void givenNonExistingRedirectURL_whenCreateUpdateRequest_shouldReturn404() throws Exception {
        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        var requestDTO = new NewUpdateRequestDTO(redirectURLId, "https://new-url.org", "Some comment.");
        var request = new HttpEntity<>(requestDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/update-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS033", "The redirect URL not found."));
    }

    @Test
    void givenDuplicatedRedirectURL_whenCreateUpdateRequest_shouldReturn400() throws Exception {
        urlRepository.save(new RedirectURL(clientId, redirectURLId, redirectURL));

        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        var requestDTO = new NewUpdateRequestDTO(redirectURLId, redirectURL, "Update with the same URL.");
        var request = new HttpEntity<>(requestDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/update-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void givenNonLowercaseRedirectURLButSimilarToExistingOne_whenCreateUpdateRequest_shouldReturn400() throws Exception {
        urlRepository.save(new RedirectURL(clientId, redirectURLId, "http://some-url.com/With-CustomPage"));

        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        var requestDTO = new NewUpdateRequestDTO(redirectURLId, "http://Some-URL.com/With-CustomPage", "Update with the same URL but non-lowercase.");
        var request = new HttpEntity<>(requestDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/update-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void givenValidPayload_whenCreateDeleteRequest_shouldSucceed() throws Exception {
        urlRepository.save(new RedirectURL(clientId, redirectURLId, redirectURL));

        var issueResponseDTO = new IssueResponseDTO("1", "JIRA-111", "https://self/JIRA-111");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/JIRA-111/comment")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        var request = new HttpEntity<>(new NewDeleteRequestDTO(redirectURLId, "A comment about deletion."), getHttpHeaders(clientToken));
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/delete-request", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getRequestDate()).isNotNull();
        assertThat(response.getBody().getAction()).isEqualTo(Action.DELETE);
        assertThat(response.getBody().getRedirectURLId()).isEqualTo(redirectURLId);
        assertThat(response.getBody().getRedirectURL()).isEqualTo(redirectURL);
        assertThat(response.getBody().getNewRedirectURL()).isNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ChangelogStatus.PENDING);
        assertThat(response.getBody().getJiraTicket()).isEqualTo("JIRA-111");
    }

    /* User input verification */

    @Test
    void givenInvalidPayload_whenCreateAddRequest_shouldFail() {
        assertAddPayload(new NewAddRequestDTO(null, "Comment."));

        var commentOver500Chars = StringUtils.repeat("Comment 123", 50);
        assertAddPayload(new NewAddRequestDTO(redirectURL, commentOver500Chars));

        var urlOver2000Chars = "https://" + StringUtils.repeat("url", 700) + ".org";
        assertAddPayload(new NewAddRequestDTO(urlOver2000Chars, "Comment."));
    }

    private void assertAddPayload(NewAddRequestDTO payload) {
        var request = new HttpEntity<>(payload, getHttpHeaders(clientToken));
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/add-request", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    void givenInvalidPayload_whenCreateUpdateRequest_shouldFail() {
        assertUpdatePayload(new NewUpdateRequestDTO(null, "https://new-url.org", "Comment."));

        assertUpdatePayload(new NewUpdateRequestDTO(redirectURLId, null, "Comment."));

        var commentOver500Chars = StringUtils.repeat("Comment 123", 50);
        assertUpdatePayload(new NewUpdateRequestDTO(redirectURLId, "https://new-url.org", commentOver500Chars));

        var urlOver2000Chars = "https://" + StringUtils.repeat("url", 700) + ".org";
        assertUpdatePayload(new NewUpdateRequestDTO(redirectURLId, urlOver2000Chars, "Comment."));
    }

    private void assertUpdatePayload(NewUpdateRequestDTO payload) {
        var request = new HttpEntity<>(payload, getHttpHeaders(clientToken));
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/update-request", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    void givenInvalidPayload_whenCreateDeleteRequest_shouldFail() {
        assertDeletePayload(new NewDeleteRequestDTO(null, "Comment."));

        var commentOver500Chars = StringUtils.repeat("Comment 123", 50);
        assertDeletePayload(new NewDeleteRequestDTO(redirectURLId, commentOver500Chars));
    }

    @Test
    void markApplied_with_valid_client_token_should_go_ok() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        changeRequestRepository.save(new RedirectURLChangelogEntry(id1, clientId, now(CLOCK), Action.CREATE, redirectURLId, null, "https://new-url.com", "Adding new URL.", ChangelogStatus.PENDING, "JIRA-111"));
        changeRequestRepository.save(new RedirectURLChangelogEntry(id2, clientId, now(CLOCK), Action.DELETE, redirectURLId, redirectURL, "https://old-url.com", "URL deleted.", ChangelogStatus.PROCESSED, "JIRA-123"));
        changeRequestRepository.save(new RedirectURLChangelogEntry(id3, clientId, now(CLOCK), Action.DELETE, redirectURLId, redirectURL, "https://current-url.com", "URL deleted.", ChangelogStatus.DENIED, "JIRA-123"));

        RedirectURLChangeRequestListDTO payload = new RedirectURLChangeRequestListDTO(
                Set.of(id1, id2, id3)
        );
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
        var request = new HttpEntity<>(payload, getHttpHeaders(clientToken));
        ResponseEntity<List<RedirectURLChangelogDTO>> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/apply-request", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new RedirectURLChangelogDTO(id1, now(CLOCK), Action.CREATE, redirectURLId, null, "https://new-url.com", ChangelogStatus.PROCESSED, "JIRA-111"),
                new RedirectURLChangelogDTO(id2, now(CLOCK), Action.DELETE, redirectURLId, "https://junit.test", "https://old-url.com", ChangelogStatus.PROCESSED, "JIRA-123"),
                new RedirectURLChangelogDTO(id3, now(CLOCK), Action.DELETE, redirectURLId, "https://junit.test", "https://current-url.com", ChangelogStatus.DENIED, "JIRA-123")
        );

    }

    @Test
    void markDenied_with_valid_client_token_should_go_ok() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        changeRequestRepository.save(new RedirectURLChangelogEntry(id1, clientId, now(CLOCK), Action.CREATE, redirectURLId, null, "https://new-url.com", "Adding new URL.", ChangelogStatus.PENDING, "JIRA-111"));
        changeRequestRepository.save(new RedirectURLChangelogEntry(id2, clientId, now(CLOCK), Action.DELETE, redirectURLId, redirectURL, "https://old-url.com", "URL deleted.", ChangelogStatus.PROCESSED, "JIRA-123"));
        changeRequestRepository.save(new RedirectURLChangelogEntry(id3, clientId, now(CLOCK), Action.DELETE, redirectURLId, redirectURL, "https://current-url.com", "URL deleted.", ChangelogStatus.DENIED, "JIRA-123"));

        RedirectURLChangeRequestListDTO payload = new RedirectURLChangeRequestListDTO(
                Set.of(id1, id2, id3)
        );
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
        var request = new HttpEntity<>(payload, getHttpHeaders(clientToken));
        ResponseEntity<List<RedirectURLChangelogDTO>> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/deny-request", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new RedirectURLChangelogDTO(id1, now(CLOCK), Action.CREATE, redirectURLId, null, "https://new-url.com", ChangelogStatus.DENIED, "JIRA-111"),
                new RedirectURLChangelogDTO(id2, now(CLOCK), Action.DELETE, redirectURLId, "https://junit.test", "https://old-url.com", ChangelogStatus.PROCESSED, "JIRA-123"),
                new RedirectURLChangelogDTO(id3, now(CLOCK), Action.DELETE, redirectURLId, "https://junit.test", "https://current-url.com", ChangelogStatus.DENIED, "JIRA-123")
        );

    }

    private void assertDeletePayload(NewDeleteRequestDTO payload) {
        var request = new HttpEntity<>(payload, getHttpHeaders(clientToken));
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls-changelog/delete-request", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpHeaders getHttpHeaders(final ClientToken clientToken) {
        var headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}
