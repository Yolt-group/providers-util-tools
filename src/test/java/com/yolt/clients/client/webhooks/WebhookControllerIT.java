package com.yolt.clients.client.webhooks;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.outboundallowlist.AllowedOutboundHost;
import com.yolt.clients.client.outboundallowlist.AllowedOutboundHostRepository;
import com.yolt.clients.client.webhooks.dto.WebhookDTO;
import com.yolt.clients.client.webhooks.dto.WebhookURLDTO;
import com.yolt.clients.client.webhooks.repository.Webhook;
import com.yolt.clients.client.webhooks.repository.WebhookRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.jira.Status;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@IntegrationTest
class WebhookControllerIT {

    private static final String DEV_PORTAL = "dev-portal";
    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private AllowedOutboundHostRepository allowedOutboundHostRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private WebhookConfigurationEventProducer webhookProducer;

    private UUID clientId;
    private UUID clientGroupId;
    private ClientGroup clientGroup;
    private Client client;
    private Webhook webhook;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        clientGroupId = UUID.randomUUID();
        clientGroup = new ClientGroup(clientGroupId, "clientGroupRedirectURL");
        client = new Client(
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
        webhook = new Webhook(clientId, "https://junit.test", true);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                webhookProducer
        );
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = {"GET", "POST", "DELETE"})
    void all_actions_with_other_client_token_should_fail(HttpMethod method) {
        var clientToken = createClientToken("other");

        var request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", method, request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS9002");
    }

    @ParameterizedTest
    @ValueSource(strings = {DEV_PORTAL, ASSISTANCE_PORTAL_YTS})
    void list_with_valid_client_token_should_succeed(String clientTokenIssuer) {
        var clientToken = createClientToken(clientTokenIssuer);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        webhookRepository.save(webhook);

        var request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<List<WebhookDTO>> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.GET, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()).contains(new WebhookDTO("https://junit.test", true));
    }

    @Test
    void upsert_with_valid_input_should_succeed() {
        ClientToken clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(),
                client.getClientId(),
                "junit.test",
                Status.ADDED,
                LocalDateTime.of(1985, 9, 5, 3, 7)));

        var webhookDTO = new WebhookDTO("https://junit.test", true);

        HttpEntity<WebhookDTO> request = new HttpEntity<>(webhookDTO, getHttpHeaders(clientToken));
        ResponseEntity<WebhookDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(webhookDTO);

        verify(webhookProducer).sendMessage(clientToken, webhookDTO, WebhookMessageType.WEBHOOK_CREATED);

        assertThat(webhookRepository.findAllByClientId(clientId))
                .contains(new Webhook(clientId, "https://junit.test", true));
    }

    @Test
    void upsert_without_allowed_outbound_host_should_fail() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        var webhookDTO = new WebhookDTO("https://junit.test", true);

        HttpEntity<WebhookDTO> request = new HttpEntity<>(webhookDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS030");
    }

    @Test
    void upsert_with_existing_webhook_should_succeed() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        webhookRepository.save(webhook);

        assertThat(webhookRepository.findAllByClientId(clientId))
                .doesNotContain(new Webhook(clientId, "https://junit.test", false))
                .contains(new Webhook(clientId, "https://junit.test", true));

        var webhookDTO = new WebhookDTO("https://junit.test", false);

        HttpEntity<WebhookDTO> request = new HttpEntity<>(webhookDTO, getHttpHeaders(clientToken));
        ResponseEntity<WebhookDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(webhookDTO);

        verify(webhookProducer).sendMessage(clientToken, webhookDTO, WebhookMessageType.WEBHOOK_UPDATED);

        assertThat(webhookRepository.findAllByClientId(clientId))
                .doesNotContain(new Webhook(clientId, "https://junit.test", true))
                .contains(new Webhook(clientId, "https://junit.test", false));
    }

    @Test
    void upsert_with_non_https_protocol_should_fail() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        var webhookDTO = new WebhookDTO("http://junit.test", true);

        HttpEntity<WebhookDTO> request = new HttpEntity<>(webhookDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");

        assertThat(webhookRepository.existsByClientIdAndWebhookURL(clientId, "https://junit.test")).isFalse();
    }

    @Test
    void upsert_with_missing_webhook_should_fail() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        var webhookDTO = new WebhookDTO(null, true);

        HttpEntity<WebhookDTO> request = new HttpEntity<>(webhookDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");

        assertThat(webhookRepository.existsByClientIdAndWebhookURL(clientId, "https://junit.test")).isFalse();
    }

    @Test
    void givenWebhookURLExceeding2000Chars_whenUpsert_shouldFail() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        var urlOver2000Chars = "https://" + StringUtils.repeat("url", 700) + ".org";
        var webhookDTO = new WebhookDTO(urlOver2000Chars, true);

        HttpEntity<WebhookDTO> request = new HttpEntity<>(webhookDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");

        assertThat(webhookRepository.existsByClientIdAndWebhookURL(clientId, "https://junit.test")).isFalse();
    }

    @Test
    void delete_with_valid_input_should_succeed() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        webhookRepository.save(webhook);

        var webhookUrlDTO = new WebhookURLDTO("https://junit.test");
        var webhookDTO = new WebhookDTO("https://junit.test", true);

        HttpEntity<WebhookURLDTO> request = new HttpEntity<>(webhookUrlDTO, getHttpHeaders(clientToken));
        ResponseEntity<Void> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.DELETE, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(webhookProducer).sendMessage(clientToken, webhookDTO, WebhookMessageType.WEBHOOK_DELETED);
        assertThat(webhookRepository.existsByClientIdAndWebhookURL(clientId, "https://junit.test")).isFalse();
    }

    @Test
    void delete_with_missing_webhook_should_fail() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        var webhookDTO = new WebhookURLDTO("https://junit.test");

        HttpEntity<WebhookURLDTO> request = new HttpEntity<>(webhookDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.DELETE,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS017");
    }

    @Test
    void givenWebhookURLExceeding2000Chars_whenDelete_shouldFail() {
        var clientToken = createClientToken(DEV_PORTAL);
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        var urlOver2000Chars = "https://" + StringUtils.repeat("url", 700) + ".org";
        var request = new HttpEntity<>(new WebhookURLDTO(urlOver2000Chars), getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/webhooks", HttpMethod.DELETE, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");

        assertThat(webhookRepository.existsByClientIdAndWebhookURL(clientId, "https://junit.test")).isFalse();
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        var headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }

    private ClientToken createClientToken(String issuedFor) {
        return testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, issuedFor));
    }
}
