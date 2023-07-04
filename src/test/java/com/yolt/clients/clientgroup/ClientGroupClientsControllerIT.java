package com.yolt.clients.clientgroup;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientEventProducer;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.client.dto.NewClientDTO;
import com.yolt.clients.events.ClientEvent;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.AbstractClientToken;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@IntegrationTest
class ClientGroupClientsControllerIT {

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private ClientEventProducer clientEventProducer;
    @Autowired
    private ClientGroupEventProducer clientGroupEventProducer;

    private UUID clientGroupId;
    private UUID clientId;
    private ClientGroup clientGroup;
    private Client client;

    private ClientGroupToken clientGroupToken;

    @BeforeEach
    public void setup() {
        clientGroupId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
        client = new Client(
                clientId,
                clientGroupId,
                "client name",
                "NL",
                false,
                true,
                "12.1",
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

        clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
    }

    @AfterEach
    void validateMocks() {
        verifyNoMoreInteractions(ignoreStubs(clientEventProducer, clientGroupEventProducer));
    }

    @Test
    void testGetClients() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientDTO[]> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.GET, requestEntity, ClientDTO[].class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()).containsExactlyInAnyOrder(new ClientDTO(
                client.getClientId(),
                client.getClientGroupId(),
                client.getName(),
                client.getKycCountryCode(),
                client.isKycPrivateIndividuals(),
                client.isKycEntities(),
                client.getSbiCode(),
                client.getGracePeriodInDays(),
                client.isDataEnrichmentMerchantRecognition(),
                client.isDataEnrichmentCategorization(),
                client.isDataEnrichmentCycleDetection(),
                client.isDataEnrichmentLabels(),
                client.isCam(),
                client.isPsd2Licensed(),
                client.isAis(),
                client.isPis(),
                client.isDeleted(),
                client.isConsentStarter(),
                client.isOneOffAis(),
                client.isRiskInsights(),
                1L,
                List.of()
        ));
    }

    @Test
    void testGetClientsGroupIdDoesNotExists() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientGroupToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.GET, requestEntity, ErrorDTO.class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS001", "client group not found"));
    }

    @Test
    void testGetClientsGroupIdDoesNotMatchToken() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientGroupToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.GET, requestEntity, ErrorDTO.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS002", "The client id and/or client group id validation failed."));
    }

    @ParameterizedTest
    @CsvSource({
            ",testName,NL,1,12.12.1",
            ",testName,NL,,12.12.1",
            ",testName,GB,5000,12121",
            "68b254ad-414b-4e3c-8d2a-b69515bdfc0f,testName,US,100,"
    })
    void testAddClient(UUID clientId, String clientName, String countryCode, Integer gracePeriod, String sbiCode) {
        clientGroupRepository.save(clientGroup);

        HttpEntity<NewClientDTO> requestEntity = new HttpEntity<>(
                new NewClientDTO(clientId,
                        clientName,
                        countryCode,
                        false,
                        true,
                        sbiCode,
                        gracePeriod,
                        false,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L),
                getHttpHeaders(clientGroupToken)
        );
        ResponseEntity<ClientDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.POST, requestEntity, ClientDTO.class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull().isEqualTo(new ClientDTO(
                response.getBody().getClientId(),
                clientGroupId,
                clientName,
                countryCode,
                false,
                true,
                sbiCode,
                gracePeriod,
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
                List.of()
        ));
        clientId = response.getBody().getClientId();
        Optional<Client> insertedClient = clientsRepository.findClientByClientGroupIdAndClientId(clientGroupId, clientId);
        assertThat(insertedClient).isPresent().hasValue(new Client(
                clientId,
                clientGroupId,
                clientName,
                countryCode,
                false,
                true,
                sbiCode,
                gracePeriod,
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
        ));
        client = insertedClient.get();
        verify(clientEventProducer).sendMessage(clientGroupToken, ClientEvent.addClientEvent(client));
    }

    @ParameterizedTest
    @CsvSource({
            "testName,NL,0,12.12.1,gracePeriodInDays",
            "testName,NL,5001,12.12.1,gracePeriodInDays",
            ",NL,10,12.12.1,name",
            "012345678901234567890123456789012345678901234567890123456789012345678901234567891,NL,10,12.12.1,name",
            "'',NL,10,12.12.1,name",
            "'   ',NL,10,12.12.1,name",
            "testName,,5000,12.12.1,'request body validation error'",
            "testName,NLD,5000,12.12.1,kycCountryCode",
            "testName,NL,5000,12.1,sbiCode",
    })
    void testAddClientInvalidInput(String clientName, String countryCode, Integer gracePeriod, String sbiCode, String offendingField) {
        clientGroupRepository.save(clientGroup);

        HttpEntity<NewClientDTO> requestEntity = new HttpEntity<>(
                new NewClientDTO(null,
                        clientName,
                        countryCode,
                        false,
                        true,
                        sbiCode,
                        gracePeriod,
                        false,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L),
                getHttpHeaders(clientGroupToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.POST, requestEntity, ErrorDTO.class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");
        assertThat(response.getBody().getMessage()).contains(offendingField);
    }

    @Test
    void testAddClientCountryCodeOptionalWhenNoKYC() {
        clientGroupRepository.save(clientGroup);

        String clientName = "client name";
        HttpEntity<NewClientDTO> requestEntity = new HttpEntity<>(
                new NewClientDTO(clientId,
                        clientName,
                        null,
                        false,
                        false,
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
                        true,
                        true,
                        false,
                        1L),
                getHttpHeaders(clientGroupToken)
        );
        ResponseEntity<ClientDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.POST, requestEntity, ClientDTO.class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull().isEqualTo(new ClientDTO(
                response.getBody().getClientId(),
                clientGroupId,
                clientName,
                null,
                false,
                false,
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
                false,
                1L,
                List.of()
        ));
        clientId = response.getBody().getClientId();
        Optional<Client> insertedClient = clientsRepository.findClientByClientGroupIdAndClientId(clientGroupId, clientId);
        assertThat(insertedClient).isPresent().hasValue(new Client(
                clientId,
                clientGroupId,
                clientName,
                null,
                false,
                false,
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
                false,
                1L,
                Collections.emptySet()
        ));
        client = insertedClient.get();
        verify(clientEventProducer).sendMessage(clientGroupToken, ClientEvent.addClientEvent(client));
    }

    @Test
    void testAddClientCountryCodeNotOptionalWhenKYC() {
        clientGroupRepository.save(clientGroup);

        String clientName = "client name";
        HttpEntity<NewClientDTO> requestEntity = new HttpEntity<>(
                new NewClientDTO(clientId,
                        clientName,
                        null,
                        true,
                        false,
                        "12.1",
                        null,
                        false,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L),
                getHttpHeaders(clientGroupToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.POST, requestEntity, ErrorDTO.class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CLS1008");
        assertThat(response.getBody().getMessage()).contains("request body validation error");
    }

    @Test
    void testAddClientClientGroupNotFound() {
        HttpEntity<NewClientDTO> requestEntity = new HttpEntity<>(
                new NewClientDTO(null,
                        "testName",
                        "NL",
                        false,
                        true,
                        "12.12.1",
                        null,
                        false,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L),
                getHttpHeaders(clientGroupToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.POST, requestEntity, ErrorDTO.class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS001", "client group not found"));
    }

    @Test
    void testAddClientClientGroupDoesNotMatchToken() {
        HttpEntity<NewClientDTO> requestEntity = new HttpEntity<>(
                new NewClientDTO(null,
                        "testName",
                        "NL",
                        false,
                        true,
                        "12.12.1",
                        null,
                        false,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L),
                getHttpHeaders(clientGroupToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/clients", HttpMethod.POST, requestEntity, ErrorDTO.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS002", "The client id and/or client group id validation failed."));
    }

    @Test
    void testAddClientButClientExists() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<NewClientDTO> requestEntity = new HttpEntity<>(
                new NewClientDTO(client.getClientId(),
                        "testName",
                        "NL",
                        false,
                        true,
                        "12.12.1",
                        null,
                        false,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L),
                getHttpHeaders(clientGroupToken)
        );
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/client-groups/{clientGroupId}/clients",
                HttpMethod.POST, requestEntity, ErrorDTO.class, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS004", "The client id is linked with an existing client."));
    }

    private HttpHeaders getHttpHeaders(AbstractClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}
