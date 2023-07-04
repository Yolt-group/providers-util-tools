package com.yolt.clients.client;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.creditoraccounts.AccountIdentifierSchemeEnum;
import com.yolt.clients.client.creditoraccounts.CreditorAccount;
import com.yolt.clients.client.creditoraccounts.CreditorAccountDTO;
import com.yolt.clients.client.creditoraccounts.CreditorAccountRepository;
import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.client.dto.ClientOnboardingStatusDTO;
import com.yolt.clients.client.dto.UpdateClientDTO;
import com.yolt.clients.client.ipallowlist.AllowedIP;
import com.yolt.clients.client.ipallowlist.IPAllowListRepository;
import com.yolt.clients.client.mtlscertificates.ClientMTLSCertificate;
import com.yolt.clients.client.mtlscertificates.ClientMTLSCertificateRepository;
import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDN;
import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDNRepository;
import com.yolt.clients.client.outboundallowlist.AllowedOutboundHost;
import com.yolt.clients.client.outboundallowlist.AllowedOutboundHostRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.client.requesttokenpublickeys.RequestTokenPublicKeyRepository;
import com.yolt.clients.client.requesttokenpublickeys.model.RequestTokenPublicKey;
import com.yolt.clients.client.webhooks.repository.Webhook;
import com.yolt.clients.client.webhooks.repository.WebhookRepository;
import com.yolt.clients.clientgroup.ClientGroupEventProducer;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.events.ClientEvent;
import com.yolt.clients.events.ClientGroupEvent;
import com.yolt.clients.jira.Status;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static com.yolt.clients.client.dto.ClientOnboardingStatusDTO.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@IntegrationTest
class ClientControllerIT {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ClientMTLSCertificateRepository clientMTLSCertificateRepository;

    @Autowired
    private ClientMTLSCertificateDNRepository clientMTLSCertificateDNRepository;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private ClientEventProducer clientEventProducer;

    @Autowired
    private ClientGroupEventProducer clientGroupEventProducer;

    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private RedirectURLRepository redirectURLRepository;

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private IPAllowListRepository ipAllowListRepository;

    @Autowired
    private RequestTokenPublicKeyRepository requestTokenPublicKeyRepository;

    @Autowired
    private CreditorAccountRepository creditorAccountRepository;


    @Autowired
    private AllowedOutboundHostRepository allowedOutboundHostRepository;

    private ClientToken clientToken;

    private UUID clientGroupId;
    private UUID clientId;
    private ClientGroup clientGroup;
    private Client client;

    private UUID deletedClientId;
    private Client deletedClient;

    @BeforeEach
    void setup() {
        clientGroupId = UUID.randomUUID();
        clientGroup = new ClientGroup(clientGroupId, "clientGroupName");

        clientId = UUID.randomUUID();
        client = new Client(
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
                false,
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

        deletedClientId = UUID.randomUUID();
        deletedClient = new Client(
                deletedClientId,
                clientGroupId,
                "deleted client name",
                "NL",
                false,
                true,
                "10.71",
                null,
                false,
                true,
                false,
                true,
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                1L,
                Collections.emptySet()
        );

        setupClientToken("assistance-portal-yts");
    }

    private ClientMTLSCertificate createMTLSCertificate(UUID clientId) {
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        return new ClientMTLSCertificate(clientId, "fingerprint", BigInteger.ONE, "DN", "Issuer", yesterday, tomorrow, yesterday, yesterday, "certificate", now);
    }

    private ClientMTLSCertificateDN createDNCertificate(UUID clientId, Status status) {
        LocalDateTime yesterday = LocalDateTime.now(FIXED_CLOCK).minusDays(1);
        return new ClientMTLSCertificateDN(UUID.randomUUID(), clientId, "DN", "Issuer", "certificate chain", status, yesterday, yesterday, "jira ticket");
    }

    @Test
    void testGetAllActiveClientsOnly() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        clientsRepository.save(deletedClient);

        ResponseEntity<List<ClientDTO>> response = testRestTemplate.exchange(
                "/internal/clients", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(response.getBody()).contains(new ClientDTO(
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
                false,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                1L,
                Collections.emptyList()
        ));
    }

    @Test
    void testGetAllDeletedClientsOnly() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        clientsRepository.save(deletedClient);

        ResponseEntity<List<ClientDTO>> response = testRestTemplate.exchange(
                "/internal/clients?deleted=true", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(response.getBody()).contains(new ClientDTO(
                deletedClientId,
                clientGroupId,
                "deleted client name",
                "NL",
                false,
                true,
                "10.71",
                null,
                false,
                true,
                false,
                true,
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                1L,
                Collections.emptyList()
        ));
    }

    @Test
    void testGetClientById() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ClientDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ClientDTO(
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
                false,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                1L,
                Collections.emptyList()
        ));
    }

    @Test
    void testGetClientClientNotFound() {
        clientGroupRepository.save(clientGroup);

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}", HttpMethod.GET, requestEntity, ErrorDTO.class, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS003", "client not found"));
    }

    @Test
    void testGetClientClientIdDoesNotMatchToken() {
        clientGroupRepository.save(clientGroup);

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}", HttpMethod.GET, requestEntity, ErrorDTO.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS002", "The client id and/or client group id validation failed."));
    }

    @ParameterizedTest
    @CsvSource({
            "testName,1",
            "'other name',",
            "testName,2500",
            "testName,100"
    })
    void testUpdateClient(String clientName, Integer gracePeriod) {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<UpdateClientDTO> requestEntity = new HttpEntity<>(
                new UpdateClientDTO(
                        clientName,
                        "NL",
                        true,
                        false,
                        "10.71",
                        gracePeriod,
                        true,
                        false,
                        true,
                        false,
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        1L
                ),
                getHttpHeaders(clientToken)
        );
        ResponseEntity<ClientDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}", HttpMethod.PUT, requestEntity, ClientDTO.class, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ClientDTO(
                clientId,
                clientGroupId,
                clientName,
                "NL",
                true,
                false,
                "10.71",
                gracePeriod,
                true,
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptyList()
        ));
        Optional<Client> insertedClient = clientsRepository.findClientByClientGroupIdAndClientId(clientGroupId, clientId);
        assertThat(insertedClient).isPresent().hasValue(new Client(
                clientId,
                clientGroupId,
                clientName,
                "NL",
                true,
                false,
                "10.71",
                gracePeriod,
                true,
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptySet()
        ));
        client = insertedClient.get();
        verify(clientEventProducer).sendMessage(clientToken, ClientEvent.updateClientEvent(client, Collections.emptyList()));
    }

    @Test
    void testUpdateClientWithCreditorAccounts() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        UUID creditorAccountId1 = UUID.randomUUID();
        UUID creditorAccountId2 = UUID.randomUUID();
        creditorAccountRepository.saveAll(List.of(
                new CreditorAccount(creditorAccountId1, clientId, "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null),
                new CreditorAccount(creditorAccountId2, clientId, "Creditor Account 2", "Account Number 2", AccountIdentifierSchemeEnum.SORTCODEACCOUNTNUMBER, "Secondary Identification")
        ));
        List<CreditorAccountDTO> creditorAccounts = List.of(
                new CreditorAccountDTO(creditorAccountId1, "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null),
                new CreditorAccountDTO(creditorAccountId2, "Creditor Account 2", "Account Number 2", AccountIdentifierSchemeEnum.SORTCODEACCOUNTNUMBER, "Secondary Identification")
        );

        HttpEntity<UpdateClientDTO> requestEntity = new HttpEntity<>(
                new UpdateClientDTO(
                        "clientName",
                        "NL",
                        true,
                        false,
                        "10.71",
                        1,
                        true,
                        false,
                        true,
                        false,
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        1L
                ),
                getHttpHeaders(clientToken)
        );
        ResponseEntity<ClientDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}", HttpMethod.PUT, requestEntity, ClientDTO.class, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ClientDTO(
                clientId,
                clientGroupId,
                "clientName",
                "NL",
                true,
                false,
                "10.71",
                1,
                true,
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptyList()
        ));
        Optional<Client> insertedClient = clientsRepository.findClientByClientGroupIdAndClientId(clientGroupId, clientId);
        assertThat(insertedClient).isPresent().hasValue(new Client(
                clientId,
                clientGroupId,
                "clientName",
                "NL",
                true,
                false,
                "10.71",
                1,
                true,
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptySet()
        ));
        client = insertedClient.get();
        verify(clientEventProducer).sendMessage(clientToken, ClientEvent.updateClientEvent(client, creditorAccounts));
    }

    @ParameterizedTest
    @CsvSource({
            "testName,0,gracePeriodInDays",
            "testName,5001,gracePeriodInDays",
            ",10,name",
            "012345678901234567890123456789012345678901234567890123456789012345678901234567891,10,name",
            "'',10,name",
            "'   ',10,name"
    })
    void testUpdateClientBadInput(String clientName, Integer gracePeriod, String offendingField) {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<UpdateClientDTO> requestEntity = new HttpEntity<>(
                new UpdateClientDTO(
                        clientName,
                        "NL",
                        true,
                        false,
                        "10.71",
                        gracePeriod,
                        true,
                        false,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L
                ),
                getHttpHeaders(clientToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}", HttpMethod.PUT, requestEntity, ErrorDTO.class, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");
        assertThat(response.getBody().getMessage()).contains(offendingField);
    }

    @Test
    void testUpdateClientClientDoesNotExists() {
        clientGroupRepository.save(clientGroup);

        HttpEntity<UpdateClientDTO> requestEntity = new HttpEntity<>(
                new UpdateClientDTO(
                        "clientName",
                        "NL",
                        true,
                        false,
                        "10.71",
                        null,
                        true,
                        false,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L
                ),
                getHttpHeaders(clientToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}", HttpMethod.PUT, requestEntity, ErrorDTO.class, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS003", "client not found"));
    }

    @Test
    void testUpdateClientClientIdDoesNotMatchToken() {
        clientGroupRepository.save(clientGroup);

        HttpEntity<UpdateClientDTO> requestEntity = new HttpEntity<>(
                new UpdateClientDTO(
                        "clientName",
                        "NL",
                        true,
                        false,
                        "10.71",
                        null,
                        true,
                        false,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L
                ),
                getHttpHeaders(clientToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}", HttpMethod.PUT, requestEntity, ErrorDTO.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS002", "The client id and/or client group id validation failed."));
    }

    private void setupClientToken(String isf) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, isf));
    }

    @Test
    void getOnboardingStatus_nothing_configured_for_this_client() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        UUID otherClientId = UUID.randomUUID();
        Client otherClient = new Client(
                otherClientId,
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
                false,
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

        clientsRepository.save(otherClient);
        redirectURLRepository.save(new RedirectURL(otherClientId, UUID.randomUUID(), "https://test-redirect"));
        webhookRepository.save(new Webhook(otherClientId, "https://test-webhook", false));
        ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), otherClientId, "127.0.0.1/32", Status.ADDED, LocalDateTime.now(FIXED_CLOCK)));
        requestTokenPublicKeyRepository.save(new RequestTokenPublicKey(otherClientId, "keyId", "public key", LocalDateTime.now(FIXED_CLOCK)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), otherClientId, "host", Status.ADDED, LocalDateTime.now(FIXED_CLOCK)));

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ClientOnboardingStatusDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/onboarding-status", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(ClientOnboardingStatusDTO.builder()
                .distinguishedNameConfigured(NOT_CONFIGURED)
                .mutualTlsCertificatesConfigured(NOT_CONFIGURED)
                .redirectUrlConfigured(NOT_CONFIGURED)
                .webhookUrlConfigured(NOT_CONFIGURED)
                .ipAllowListConfigured(NOT_CONFIGURED)
                .requestTokenConfigured(NOT_CONFIGURED)
                .webhookDomainAllowListConfigured(NOT_CONFIGURED)
                .build());
    }

    @Test
    void getOnboardingStatus_all_configured() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        clientMTLSCertificateRepository.save(createMTLSCertificate(clientId));
        clientMTLSCertificateDNRepository.save(createDNCertificate(clientId, Status.ADDED));
        redirectURLRepository.save(new RedirectURL(clientId, UUID.randomUUID(), "https://test-redirect"));
        webhookRepository.save(new Webhook(clientId, "https://test-webhook", false));
        ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.1/32", Status.ADDED, LocalDateTime.now(FIXED_CLOCK)));
        requestTokenPublicKeyRepository.save(new RequestTokenPublicKey(clientId, "keyId", "public key", LocalDateTime.now(FIXED_CLOCK)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), clientId, "host", Status.ADDED, LocalDateTime.now(FIXED_CLOCK)));

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ClientOnboardingStatusDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/onboarding-status", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(ClientOnboardingStatusDTO.builder()
                .distinguishedNameConfigured(CONFIGURED)
                .mutualTlsCertificatesConfigured(CONFIGURED)
                .redirectUrlConfigured(CONFIGURED)
                .webhookUrlConfigured(CONFIGURED)
                .ipAllowListConfigured(CONFIGURED)
                .requestTokenConfigured(CONFIGURED)
                .webhookDomainAllowListConfigured(CONFIGURED)
                .build());
    }

    @Test
    void getOnboardingStatus_ip_allow_list_pending_allows_host_pending() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        clientMTLSCertificateRepository.save(createMTLSCertificate(clientId));
        clientMTLSCertificateDNRepository.save(createDNCertificate(clientId, Status.PENDING_ADDITION));
        redirectURLRepository.save(new RedirectURL(clientId, UUID.randomUUID(), "https://test-redirect"));
        webhookRepository.save(new Webhook(clientId, "https://test-webhook", false));
        ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.1/32", Status.PENDING_ADDITION, LocalDateTime.now(FIXED_CLOCK)));
        requestTokenPublicKeyRepository.save(new RequestTokenPublicKey(clientId, "keyId", "public key", LocalDateTime.now(FIXED_CLOCK)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), clientId, "host", Status.PENDING_ADDITION, LocalDateTime.now(FIXED_CLOCK)));

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ClientOnboardingStatusDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/onboarding-status", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(ClientOnboardingStatusDTO.builder()
                .distinguishedNameConfigured(PENDING)
                .mutualTlsCertificatesConfigured(CONFIGURED)
                .redirectUrlConfigured(CONFIGURED)
                .webhookUrlConfigured(CONFIGURED)
                .ipAllowListConfigured(PENDING)
                .requestTokenConfigured(CONFIGURED)
                .webhookDomainAllowListConfigured(PENDING)
                .build());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"PENDING_ADDITION", "ADDED"}, mode = EnumSource.Mode.EXCLUDE)
    void getOnboardingStatus_ip_allow_list_and_allows_hosts_not_configured_state(Status status) {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        clientMTLSCertificateRepository.save(createMTLSCertificate(clientId));
        clientMTLSCertificateDNRepository.save(createDNCertificate(clientId, Status.PENDING_REMOVAL));
        redirectURLRepository.save(new RedirectURL(clientId, UUID.randomUUID(), "https://test-redirect"));
        webhookRepository.save(new Webhook(clientId, "https://test-webhook", false));
        ipAllowListRepository.save(new AllowedIP(UUID.randomUUID(), clientId, "127.0.0.1/32", status, LocalDateTime.now(FIXED_CLOCK)));
        requestTokenPublicKeyRepository.save(new RequestTokenPublicKey(clientId, "keyId", "public key", LocalDateTime.now(FIXED_CLOCK)));
        allowedOutboundHostRepository.save(new AllowedOutboundHost(UUID.randomUUID(), clientId, "host", status, LocalDateTime.now(FIXED_CLOCK)));

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ClientOnboardingStatusDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/onboarding-status", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(ClientOnboardingStatusDTO.builder()
                .distinguishedNameConfigured(CONFIGURED)
                .mutualTlsCertificatesConfigured(CONFIGURED)
                .redirectUrlConfigured(CONFIGURED)
                .webhookUrlConfigured(CONFIGURED)
                .ipAllowListConfigured(NOT_CONFIGURED)
                .requestTokenConfigured(CONFIGURED)
                .webhookDomainAllowListConfigured(NOT_CONFIGURED)
                .build());
    }

    @Test
    void testSyncClients() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        ResponseEntity<Void> response = testRestTemplate.postForEntity("/internal/clients/batch/sync", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(clientGroupEventProducer, timeout(1000)).sendMessage(new ClientGroupEvent(ClientGroupEvent.Action.SYNC, clientGroup.getId(), clientGroup.getName()));
        verify(clientEventProducer, timeout(1000)).sendMessage(ClientEvent.syncClientEvent(client, Collections.emptyList()));
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}
