package com.yolt.clients.jira;

import com.yolt.clients.TestConfiguration;
import com.yolt.clients.client.ClientService;
import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.client.ipallowlist.AllowedIP;
import com.yolt.clients.client.ipallowlist.JiraAllowedIPData;
import com.yolt.clients.client.redirecturls.Action;
import com.yolt.clients.client.redirecturls.jira.RedirectURLChangelogJiraData;
import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogEntry;
import com.yolt.clients.jira.dto.CommentDTO;
import com.yolt.clients.jira.dto.FieldDTO;
import com.yolt.clients.jira.dto.IssueDTO;
import com.yolt.clients.jira.dto.IssueResponseDTO;
import com.yolt.securityutils.crypto.PasswordKey;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ClientService clientService;

    @Mock
    private ClientToken clientToken;

    @Mock
    private VaultKeys vaultKeys;

    @Captor
    private ArgumentCaptor<HttpEntity<IssueDTO>> entityArgumentCaptor;

    @Captor
    private ArgumentCaptor<HttpEntity<CommentDTO>> commentArgumentCaptor;

    private static JiraService jiraService;
    private static JiraService jiraServiceING;
    private static JiraService jiraServiceProduction;
    private static JiraService jiraServiceSandbox;
    private UUID clientId, clientGroupId;
    private ClientDTO client;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        clientGroupId = UUID.randomUUID();
        client = new ClientDTO(
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
                Collections.emptyList());

        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.rootUri(anyString())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.requestFactory(any(Supplier.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        when(vaultKeys.getPassword("clients-jira-token")).thenReturn(new PasswordKey("secret-token".toCharArray()));

        jiraService = new JiraService("https://jira.com", "npa@yolt.com", true, "squid", 3128, "test environment", vaultKeys, restTemplateBuilder, clientService);
        jiraServiceING = new JiraService("https://jira.com", "npa@yolt.com", true, "squid", 3128, "yfb-prd", vaultKeys, restTemplateBuilder, clientService);
        jiraServiceProduction = new JiraService("https://jira.com", "npa@yolt.com", true, "squid", 3128, "yfb-ext-prd", vaultKeys, restTemplateBuilder, clientService);
        jiraServiceSandbox = new JiraService("https://jira.com", "npa@yolt.com", true, "squid", 3128, "yfb-sandbox", vaultKeys, restTemplateBuilder, clientService);
    }

    @Test
    void creates_issue_add_ips() {
        var allowedIPId = UUID.randomUUID();
        var allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);
        var allowedIP2 = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.2/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);
        var allowedIP3 = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.3/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);

        var responseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self.com");
        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class))).thenReturn(ResponseEntity.ok(responseDTO));
        when(clientService.getClient(clientToken)).thenReturn(client);

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.CREATE)
                .withItemToBeAdded(allowedIP)
                .withItemToBeAdded(allowedIP2)
                .withItemToBeAdded(allowedIP3);

        String issueKey = jiraService.createIssue(clientToken, jiraAllowedIPData);
        assertThat(issueKey).isEqualTo("JIRA-123");

        FieldDTO request = entityArgumentCaptor.getValue().getBody().getFields();

        assertThat(request.getProject().getKey()).isEqualTo("YT");
        assertThat(request.getIssuetype().getName()).isEqualTo("Submit a request or incident");
        assertThat(request.getCustomfield_10010()).isEqualTo("yt/bbda6e6e-255e-48c4-9d34-f8ba05374247");
        assertThat(request.getCustomfield_10002()).isEqualTo(Set.of(1L));
        assertThat(request.getSummary()).isEqualTo("[test environment] Request for adding IPs from/to the allowlist");
        assertThat(request.getDescription()).isEqualToNormalizingNewlines("""
                client id: %s
                client name: client name
                                
                Add the following IPs to the allowlist:
                - 127.0.0.1/32
                - 127.0.0.2/32
                - 127.0.0.3/32
                    
                Remove the following IPs from the allowlist:
                <n/a>
                """.formatted(clientId));
    }

    @Test
    void create_issue_remove_ips() {
        UUID allowedIPId = UUID.randomUUID();
        AllowedIP allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);

        IssueResponseDTO responseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self.com");
        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class))).thenReturn(ResponseEntity.ok(responseDTO));
        when(clientService.getClient(clientToken)).thenReturn(client);

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.DELETE).withItemToBeRemoved(allowedIP);

        String issueKey = jiraService.createIssue(clientToken, jiraAllowedIPData);
        assertThat(issueKey).isEqualTo("JIRA-123");
        FieldDTO request = entityArgumentCaptor.getValue().getBody().getFields();

        assertThat(request.getProject().getKey()).isEqualTo("YT");
        assertThat(request.getIssuetype().getName()).isEqualTo("Submit a request or incident");
        assertThat(request.getCustomfield_10010()).isEqualTo("yt/bbda6e6e-255e-48c4-9d34-f8ba05374247");
        assertThat(request.getCustomfield_10002()).isEqualTo(Set.of(1L));
        assertThat(request.getSummary()).isEqualTo("[test environment] Request for removing IPs from/to the allowlist");
        assertThat(request.getDescription()).isEqualTo("""
                client id: %s
                client name: client name
                                
                Add the following IPs to the allowlist:
                <n/a>
                    
                Remove the following IPs from the allowlist:
                - 127.0.0.1/32
                """.formatted(clientId));
    }

    @Test
    void create_issue_null_response() {
        UUID allowedIPId = UUID.randomUUID();
        AllowedIP allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);
        when(clientService.getClient(clientToken)).thenReturn(client);

        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class))).thenReturn(ResponseEntity.of(Optional.empty()));

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.DELETE).withItemToBeRemoved(allowedIP);

        assertThrows(IllegalStateException.class, () -> jiraService.createIssue(clientToken, jiraAllowedIPData));
    }

    @Test
    void create_works_with_null_jira_id() {
        ClientDTO client = new ClientDTO(
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
                null,
                Collections.emptyList());

        UUID allowedIPId = UUID.randomUUID();
        AllowedIP allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);

        IssueResponseDTO responseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self.com");
        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class))).thenReturn(ResponseEntity.ok(responseDTO));
        when(clientService.getClient(clientToken)).thenReturn(client);

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.CREATE).withItemToBeAdded(allowedIP);

        String issueKey = jiraService.createIssue(clientToken, jiraAllowedIPData);
        assertThat(issueKey).isEqualTo("JIRA-123");
        FieldDTO request = entityArgumentCaptor.getValue().getBody().getFields();

        assertThat(request.getProject().getKey()).isEqualTo("YT");
        assertThat(request.getIssuetype().getName()).isEqualTo("Submit a request or incident");
        assertThat(request.getCustomfield_10010()).isEqualTo("yt/bbda6e6e-255e-48c4-9d34-f8ba05374247");
        assertThat(request.getCustomfield_10002()).isEqualTo(Collections.emptySet());
        assertThat(request.getSummary()).isEqualTo("[%s] Request for adding IPs from/to the allowlist".formatted("test environment"));
        assertThat(request.getDescription()).containsIgnoringCase("""
                Add the following IPs to the allowlist:
                - 127.0.0.1/32
                    
                Remove the following IPs from the allowlist:
                <n/a>
                """);
    }

    @Test
    void jiraServiceING_create_works_with_null_jira_id_() {
        ClientDTO client = new ClientDTO(
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
                null,
                Collections.emptyList());

        UUID allowedIPId = UUID.randomUUID();
        AllowedIP allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);

        IssueResponseDTO responseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self.com");
        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class))).thenReturn(ResponseEntity.ok(responseDTO));
        when(clientService.getClient(clientToken)).thenReturn(client);

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.CREATE).withItemToBeAdded(allowedIP);

        String issueKey = jiraServiceING.createIssue(clientToken, jiraAllowedIPData);
        assertThat(issueKey).isEqualTo("JIRA-123");
        FieldDTO request = entityArgumentCaptor.getValue().getBody().getFields();

        assertThat(request.getProject().getKey()).isEqualTo("YT");
        assertThat(request.getIssuetype().getName()).isEqualTo("Submit a request or incident");
        assertThat(request.getCustomfield_10010()).isEqualTo("yt/bbda6e6e-255e-48c4-9d34-f8ba05374247");
        assertThat(request.getCustomfield_10002()).isEqualTo(Collections.emptySet());
        assertThat(request.getSummary()).isEqualTo("[%s] Request for adding IPs from/to the allowlist".formatted("ING"));
        assertThat(request.getDescription()).containsIgnoringCase("""
                Add the following IPs to the allowlist:
                - 127.0.0.1/32
                    
                Remove the following IPs from the allowlist:
                <n/a>
                """);
    }

    @Test
    void jiraServiceProduction_create_works_with_null_jira_id() {
        ClientDTO client = new ClientDTO(
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
                null,
                Collections.emptyList());

        UUID allowedIPId = UUID.randomUUID();
        AllowedIP allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);

        IssueResponseDTO responseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self.com");
        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class))).thenReturn(ResponseEntity.ok(responseDTO));
        when(clientService.getClient(clientToken)).thenReturn(client);

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.CREATE).withItemToBeAdded(allowedIP);

        String issueKey = jiraServiceProduction.createIssue(clientToken, jiraAllowedIPData);
        assertThat(issueKey).isEqualTo("JIRA-123");
        FieldDTO request = entityArgumentCaptor.getValue().getBody().getFields();

        assertThat(request.getProject().getKey()).isEqualTo("YT");
        assertThat(request.getIssuetype().getName()).isEqualTo("Submit a request or incident");
        assertThat(request.getCustomfield_10010()).isEqualTo("yt/bbda6e6e-255e-48c4-9d34-f8ba05374247");
        assertThat(request.getCustomfield_10002()).isEqualTo(Collections.emptySet());
        assertThat(request.getSummary()).isEqualTo("[%s] Request for adding IPs from/to the allowlist".formatted("production"));
        assertThat(request.getDescription()).containsIgnoringCase("""
                Add the following IPs to the allowlist:
                - 127.0.0.1/32
                    
                Remove the following IPs from the allowlist:
                <n/a>
                """);
    }

    @Test
    void jiraServiceSandbox_create_works_with_null_jira_id() {
        ClientDTO client = new ClientDTO(
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
                null,
                Collections.emptyList());

        UUID allowedIPId = UUID.randomUUID();
        AllowedIP allowedIP = new AllowedIP(allowedIPId, clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null);

        IssueResponseDTO responseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self.com");
        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class))).thenReturn(ResponseEntity.ok(responseDTO));
        when(clientService.getClient(clientToken)).thenReturn(client);

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.CREATE).withItemToBeAdded(allowedIP);

        String issueKey = jiraServiceSandbox.createIssue(clientToken, jiraAllowedIPData);
        assertThat(issueKey).isEqualTo("JIRA-123");
        FieldDTO request = entityArgumentCaptor.getValue().getBody().getFields();

        assertThat(request.getProject().getKey()).isEqualTo("YT");
        assertThat(request.getIssuetype().getName()).isEqualTo("Submit a request or incident");
        assertThat(request.getCustomfield_10010()).isEqualTo("yt/bbda6e6e-255e-48c4-9d34-f8ba05374247");
        assertThat(request.getCustomfield_10002()).isEqualTo(Collections.emptySet());
        assertThat(request.getSummary()).isEqualTo("[%s] Request for adding IPs from/to the allowlist".formatted("sandbox"));
        assertThat(request.getDescription()).containsIgnoringCase("""
                Add the following IPs to the allowlist:
                - 127.0.0.1/32
                    
                Remove the following IPs from the allowlist:
                <n/a>
                """);
    }

    @Test
    void update_issue() {
        AllowedIP allowedIP = new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), "JIRA-123");

        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.UPDATE).withJiraTicketToBeEdited(allowedIP.getJiraTicket(), allowedIP);

        when(restTemplate.exchange(eq("/rest/api/2/issue/{issueIdOrKey}/comment"), eq(HttpMethod.POST), commentArgumentCaptor.capture(), eq(Void.class), eq("JIRA-123"))).thenReturn(ResponseEntity.of(Optional.empty()));

        jiraService.updateIssue(jiraAllowedIPData);

        assertThat(commentArgumentCaptor.getValue().getBody().getBody()).isEqualTo("""
                The following IPs have been handled:
                - 127.0.0.1/32
                """);
    }

    @Test
    void does_not_create_issue_if_there_is_nothing_to_add() {
        final JiraAllowedIPData jiraAllowedIPData = new JiraAllowedIPData(Action.CREATE);

        String issueKey = jiraService.createIssue(clientToken, jiraAllowedIPData);
        assertThat(issueKey).isNull();
        verify(restTemplate, never()).exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class));
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void givenDifferentActionsOnRedirectURLs_shouldCreateIssue(Action action) {
        final var now = LocalDateTime.now(TestConfiguration.FIXED_CLOCK);

        final var changelogEntry = switch (action) {
            case CREATE -> RedirectURLChangelogEntry.createEntry(clientId, now, "https://new-url.org", "Create comment");
            case UPDATE -> RedirectURLChangelogEntry.updateEntry(clientId, now, UUID.randomUUID(), "https://url.org", "https://new-url.org", "Update comment");
            case DELETE -> RedirectURLChangelogEntry.deleteEntry(clientId, now, UUID.randomUUID(), "https://url.org", "Delete comment");
        };

        final var responseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self.com");
        when(restTemplate.exchange(eq("/rest/api/2/issue/"), eq(HttpMethod.POST), entityArgumentCaptor.capture(), eq(IssueResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDTO));
        when(clientService.getClient(clientToken)).thenReturn(client);

        final var changelogJiraData = switch (action) {
            case CREATE -> new RedirectURLChangelogJiraData().withItemToBeAdded(changelogEntry);
            case UPDATE -> new RedirectURLChangelogJiraData().withItemToBeEdited(changelogEntry);
            case DELETE -> new RedirectURLChangelogJiraData().withItemToBeRemoved(changelogEntry);
        };

        String issueKey = jiraService.createIssue(clientToken, changelogJiraData);
        assertThat(issueKey).isEqualTo("JIRA-123");

        FieldDTO request = entityArgumentCaptor.getValue().getBody().getFields();

        assertThat(request.getProject().getKey()).isEqualTo("YT");
        assertThat(request.getIssuetype().getName()).isEqualTo("Submit a request or incident");
        assertThat(request.getCustomfield_10010()).isEqualTo("yt/bbda6e6e-255e-48c4-9d34-f8ba05374247");
        assertThat(request.getCustomfield_10002()).isEqualTo(Set.of(1L));
        assertThat(request.getSummary()).isEqualTo("[test environment] Request for updating the redirect URLs for licensed client.");

        final var description = """
                Add the following redirect URLs:
                %1$s

                Update the following redirect URLs:
                %2$s

                Delete the following redirect URLs:
                %3$s
                """;

        switch (action) {
            case CREATE -> assertThat(request.getDescription())
                    .containsIgnoringCase(description.formatted("- create url with id: %s, and url: %s (req id: %s)".formatted(changelogEntry.getRedirectURLId(), changelogEntry.getNewRedirectURL(), changelogEntry.getId()), "<n/a>", "<n/a>"));
            case UPDATE -> assertThat(request.getDescription())
                    .containsIgnoringCase(description.formatted("<n/a>", "- update url with id: %s, from: %s, to: %s (req id: %s)".formatted(changelogEntry.getRedirectURLId(), changelogEntry.getRedirectURL(), changelogEntry.getNewRedirectURL(), changelogEntry.getId()), "<n/a>"));
            case DELETE -> assertThat(request.getDescription())
                    .containsIgnoringCase(description.formatted("<n/a>", "<n/a>", "- delete url with id: %s, and url: %s (req id: %s)".formatted(changelogEntry.getRedirectURLId(), changelogEntry.getRedirectURL(), changelogEntry.getId())));
        }
    }
}
