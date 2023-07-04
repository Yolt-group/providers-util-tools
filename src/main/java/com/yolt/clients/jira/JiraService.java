package com.yolt.clients.jira;

import com.yolt.clients.client.ClientService;
import com.yolt.clients.jira.dto.*;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JiraService {

    private static final String PROJECT_KEY = "YT";
    private static final String ISSUE_TYPE = "Submit a request or incident";
    private static final String REQUEST_TYPE = "yt/bbda6e6e-255e-48c4-9d34-f8ba05374247";

    private final RestTemplate restTemplate;
    private final ClientService clientService;
    private String apiToken;
    private final String jiraUser;
    private final boolean isEnabled;
    private final String environment;

    public JiraService(@Value("${yolt.jira.url}") String jiraUrl,
                       @Value("${yolt.jira.user}") String jiraUser,
                       @Value("${yolt.jira.enabled}") boolean isEnabled,
                       @Value("${isp.proxy.host}") String host,
                       @Value("${isp.proxy.port}") int port,
                       @Value("${environment}") String environment,
                       VaultKeys vaultKeys,
                       RestTemplateBuilder restTemplateBuilder,
                       ClientService clientService) {

        this.isEnabled = isEnabled;
        this.clientService = clientService;
        this.jiraUser = jiraUser;
        this.environment = userFriendlyEnvironmentName(environment);
        if (isEnabled) {
            this.apiToken = new String(vaultKeys.getPassword("clients-jira-token").getEncoded(), StandardCharsets.UTF_8);
        }

        this.restTemplate = restTemplateBuilder.rootUri(jiraUrl).requestFactory(() -> {
            var proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            var requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setProxy(proxy);
            return requestFactory;
        }).build();
    }

    private String userFriendlyEnvironmentName(String environment) {
        return switch (environment) {
            case "yfb-prd" -> "ING";
            case "yfb-ext-prd" -> "production";
            case "yfb-sandbox" -> "sandbox";
            default -> environment;
        };
    }

    public <T, U extends AbstractJiraData<T, ?>> String createIssue(ClientToken clientToken, AbstractJiraData<T, U> jiraData) {
        Set<T> toBeAdded = jiraData.getItemsToBeAdded();
        Set<T> toBeEdited = jiraData.getItemsToBeEdited();
        Set<T> toBeRemoved = jiraData.getItemsToBeRemoved();
        Function<T, String> mapper = jiraData.getMapperToDescription();

        if (toBeAdded.isEmpty() && toBeEdited.isEmpty() && toBeRemoved.isEmpty()) {
            log.warn("Nothing to add, edit or remove, not creating a jira ticket.");
            return null;
        }

        String toBeAddedList = formatItems(toBeAdded, mapper);
        String toBeEditedList = formatItems(toBeEdited, mapper);
        String toBeRemovedList = formatItems(toBeRemoved, mapper);

        return createIssue(clientToken, jiraData.getDescription(toBeAddedList, toBeEditedList, toBeRemovedList), jiraData.getSummary());
    }

    public <T, U extends AbstractJiraData<T, ?>> void updateIssue(AbstractJiraData<T, U> jiraData) {
        updateIssue(jiraData.getJiraTicketsToBeEdited(), jiraData.getMapperToDescription(), jiraData.getComment());
    }

    public String createIssue(ClientToken clientToken, String description, String summary) {
        if (!isEnabled) {
            log.warn("Jira connection is not enabled.");
            return null;
        }

        var client = clientService.getClient(clientToken);

        var jiraClientId = Optional.ofNullable(client.getJiraId())
                .map(Set::of)
                .orElseGet(() -> {
                    log.warn("No jira_id found for client {}", clientToken.getClientIdClaim());
                    return Collections.emptySet();
                });
        var projectDTO = new ProjectDTO(PROJECT_KEY);
        var issueTypeDTO = new IssueTypeDTO(ISSUE_TYPE);
        var environmentAndSummary = "[%s] %s".formatted(environment, summary);
        var clientDescription = """
                client id: %s
                client name: %s
                                
                %s""".formatted(client.getClientId(), client.getName(), description);
        var fieldDTO = new FieldDTO(projectDTO, environmentAndSummary, clientDescription, issueTypeDTO, jiraClientId, REQUEST_TYPE);
        var issueDTO = new IssueDTO(fieldDTO);
        var request = new HttpEntity<>(issueDTO, getHeaders());

        ResponseEntity<IssueResponseDTO> response;
        try {
            response = restTemplate.exchange("/rest/api/2/issue/", HttpMethod.POST, request, IssueResponseDTO.class);
        } catch (Exception e ) {
            log.error("Jira Integration is broken, can not create an issue", e);
            return "ticket_creation_failed";
        }
        IssueResponseDTO responseDTO = Optional.ofNullable(response.getBody()).orElseThrow(() -> new IllegalStateException("Expected an issue key in the response from jira, but got null."));


        log.info("Created issue with key {} and url {}.", responseDTO.getKey(), responseDTO.getSelf()); //NOSHERIFF
        return responseDTO.getKey();
    }

    private <T> String formatItems(Set<T> items, Function<T, String> mapper) {
        if (items.isEmpty()) return "<n/a>";

        return items.stream()
                .map(mapper)
                .map("- %s"::formatted)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private <T> void updateIssue(Map<String, Set<T>> jiraTicketsToBeEdited, Function<T, String> mapper, String hostComment) {
        if (isEnabled) {
            jiraTicketsToBeEdited.forEach((ticketKey, items) -> {
                String itemsHandled = formatItems(items, mapper);
                var comment = new CommentDTO(String.format(hostComment, itemsHandled));
                var request = new HttpEntity<>(comment, getHeaders());
                restTemplate.exchange("/rest/api/2/issue/{issueIdOrKey}/comment", HttpMethod.POST, request, Void.class, ticketKey);

                log.info("Added the comment {} to the jira ticket {}.", comment.getBody(), ticketKey); //NOSHERIFF
            });
        } else {
            log.warn("Jira connection is not enabled, no comment will be added.");
        }
    }

    private HttpHeaders getHeaders() {
        var authorizationHeader = String.format("%s:%s", jiraUser, apiToken);
        var httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Basic " + Base64.encodeBase64String(authorizationHeader.getBytes(StandardCharsets.UTF_8)));
        return httpHeaders;
    }

}
