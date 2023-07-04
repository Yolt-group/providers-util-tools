package com.yolt.clients.client.creditoraccounts;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientEventProducer;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.events.ClientEvent;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.model.EmailDomain;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.logging.SemaEventLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@IntegrationTest
class CreditorAccountControllerIT {

    private Appender<ILoggingEvent> mockAppender;
    private ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private CreditorAccountRepository creditorAccountRepository;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientEventProducer clientEventProducer;

    private UUID clientId;
    private UUID clientGroupId;
    private ClientToken clientToken;
    private Client client;

    @BeforeEach
    public void setup() {
        clientId = UUID.randomUUID();
        clientGroupId = UUID.randomUUID();
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        client = new Client(clientId,
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
                1L,
                new HashSet<>());
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        mockAppender = mock(Appender.class);
        captorLoggingEvent = ArgumentCaptor.forClass(ILoggingEvent.class);
        final Logger logger = (Logger) LoggerFactory.getLogger(SemaEventLogger.class);
        logger.setLevel(Level.ALL);
        logger.addAppender(mockAppender);
    }

    @AfterEach
    public void afterEach() {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.detachAppender(mockAppender);
    }

    @Test
    void shouldGetCreditorAccountsForClient() {
        // Given
        UUID creditorAccountId1 = UUID.randomUUID();
        UUID creditorAccountId2 = UUID.randomUUID();

        creditorAccountRepository.saveAll(List.of(
                new CreditorAccount(creditorAccountId1, clientId, "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null),
                new CreditorAccount(creditorAccountId2, clientId, "Creditor Account 2", "Account Number 2", AccountIdentifierSchemeEnum.SORTCODEACCOUNTNUMBER, "Secondary Identification")
        ));

        // When
        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<List<CreditorAccountDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/creditor-accounts", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new CreditorAccountDTO(creditorAccountId1, "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null),
                new CreditorAccountDTO(creditorAccountId2, "Creditor Account 2", "Account Number 2", AccountIdentifierSchemeEnum.SORTCODEACCOUNTNUMBER, "Secondary Identification")
        );
    }

    @Test
    void shouldAddCreditorAccountsForClient() {
        // When
        HttpEntity<CreateCreditorAccountDTO> requestEntity = new HttpEntity<>(new CreateCreditorAccountDTO(null, "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null), getHttpHeaders(clientToken));
        ResponseEntity<CreditorAccountDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/creditor-accounts", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody())
                .hasFieldOrPropertyWithValue("accountHolderName", "Creditor Account 1")
                .hasFieldOrPropertyWithValue("accountNumber", "Account Number 1")
                .hasFieldOrPropertyWithValue("accountIdentifierScheme", AccountIdentifierSchemeEnum.IBAN)
                .hasAllNullFieldsOrPropertiesExcept("id", "accountHolderName", "accountNumber", "accountIdentifierScheme");
        List<CreditorAccount> addedCreditorAccounts = creditorAccountRepository.getCreditorAccountsByClientId(clientId);
        assertThat(addedCreditorAccounts).hasSize(1);
        CreditorAccount addedCreditorAccount = addedCreditorAccounts.get(0);
        assertThat(addedCreditorAccount.getClientId()).isEqualTo(clientId);
        assertThat(addedCreditorAccount.getAccountHolderName()).isEqualTo("Creditor Account 1");
        assertThat(addedCreditorAccount.getAccountNumber()).isEqualTo("Account Number 1");

        CreditorAccountDTO creditorAccountDTO = new CreditorAccountDTO(
                addedCreditorAccount.getId(),
                addedCreditorAccount.getAccountHolderName(),
                addedCreditorAccount.getAccountNumber(),
                addedCreditorAccount.getAccountIdentifierScheme(),
                addedCreditorAccount.getSecondaryIdentification()
        );

        verify(clientEventProducer).sendMessage(clientToken, ClientEvent.updateClientEvent(client, List.of(creditorAccountDTO)));

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        List<ILoggingEvent> values = captorLoggingEvent.getAllValues();
        ILoggingEvent semaLog = values.get(0);
        assertThat(semaLog.getMessage()).isEqualTo("Creditor account was added for client");
        assertThat(semaLog.getMarker().toString()).contains("log_type=SEMA");
        assertThat(semaLog.getMarker().toString()).contains("sema_type=com.yolt.clients.events.sema.CreditorAccountSemaEvent");
        assertThat(semaLog.getMarker().toString()).contains("clientId="+clientId);
        assertThat(semaLog.getMarker().toString()).contains("accountHolderName=Creditor Account 1");
        assertThat(semaLog.getMarker().toString()).contains("accountNumber=Account Number 1");
        assertThat(semaLog.getMarker().toString()).contains("accountIdentifierScheme=IBAN");
        assertThat(semaLog.getMarker().toString()).contains("secondaryIdentification=null");

    }

    @Test
    void shouldDeleteCreditorAccountForClient() {
        // Given
        UUID creditorAccountId1 = UUID.randomUUID();
        UUID creditorAccountId2 = UUID.randomUUID();

        creditorAccountRepository.saveAll(List.of(
                new CreditorAccount(creditorAccountId1, clientId, "Creditor Account 1", "Account Number 1", AccountIdentifierSchemeEnum.IBAN, null),
                new CreditorAccount(creditorAccountId2, clientId, "Creditor Account 2", "Account Number 2", AccountIdentifierSchemeEnum.SORTCODEACCOUNTNUMBER, "Secondary Identification")
        ));

        // When
        HttpEntity<RemoveCreditorAccountDTO> requestEntity = new HttpEntity<>(new RemoveCreditorAccountDTO(creditorAccountId1), getHttpHeaders(clientToken));
        ResponseEntity<List<CreditorAccountDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/creditor-accounts", HttpMethod.DELETE, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        List<CreditorAccount> addedCreditorAccounts = creditorAccountRepository.getCreditorAccountsByClientId(clientId);
        assertThat(addedCreditorAccounts).hasSize(1);
        CreditorAccount remainingCreditorAccount = addedCreditorAccounts.get(0);
        assertThat(remainingCreditorAccount.getId()).isEqualTo(creditorAccountId2);
        assertThat(remainingCreditorAccount.getClientId()).isEqualTo(clientId);
        assertThat(remainingCreditorAccount.getAccountHolderName()).isEqualTo("Creditor Account 2");
        assertThat(remainingCreditorAccount.getAccountNumber()).isEqualTo("Account Number 2");

        CreditorAccountDTO creditorAccountDTO = new CreditorAccountDTO(
                remainingCreditorAccount.getId(),
                remainingCreditorAccount.getAccountHolderName(),
                remainingCreditorAccount.getAccountNumber(),
                remainingCreditorAccount.getAccountIdentifierScheme(),
                remainingCreditorAccount.getSecondaryIdentification()
        );

        verify(clientEventProducer).sendMessage(clientToken, ClientEvent.updateClientEvent(client, List.of(creditorAccountDTO)));

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        List<ILoggingEvent> values = captorLoggingEvent.getAllValues();
        ILoggingEvent semaLog = values.get(0);
        assertThat(semaLog.getMessage()).isEqualTo("Creditor account was removed from client");
        assertThat(semaLog.getMarker().toString()).contains("log_type=SEMA");
        assertThat(semaLog.getMarker().toString()).contains("sema_type=com.yolt.clients.events.sema.CreditorAccountSemaEvent");
        assertThat(semaLog.getMarker().toString()).contains("clientId="+clientId);
        assertThat(semaLog.getMarker().toString()).contains("accountHolderName=Creditor Account 1");
        assertThat(semaLog.getMarker().toString()).contains("accountNumber=Account Number 1");
        assertThat(semaLog.getMarker().toString()).contains("accountIdentifierScheme=IBAN");
        assertThat(semaLog.getMarker().toString()).contains("secondaryIdentification=null");
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}