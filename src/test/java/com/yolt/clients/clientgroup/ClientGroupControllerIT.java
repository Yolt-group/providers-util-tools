package com.yolt.clients.clientgroup;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.TestConfiguration;
import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationCode;
import com.yolt.clients.clientgroup.dto.AdminInviteDTO;
import com.yolt.clients.clientgroup.dto.ClientGroupDTO;
import com.yolt.clients.clientgroup.dto.ClientGroupDetailsDTO;
import com.yolt.clients.clientgroup.dto.DomainDTO;
import com.yolt.clients.clientgroup.dto.NewClientGroupDTO;
import com.yolt.clients.clientgroup.dto.UpdateClientGroupDTO;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.model.EmailDomain;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ClientGroupControllerIT {
    private UUID clientGroupId;
    private ClientGroupToken clientGroupToken;

    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private static HttpHeaders getHttpHeaders(ClientGroupToken clientGroupToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized());
        return headers;
    }

    @BeforeEach
    void setUp() {
        clientGroupId = UUID.randomUUID();
        clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "assistance-portal-yts"));
    }

    @Test
    void clientGroupsCanBeFetched() {
        UUID clientGroupId1 = UUID.randomUUID();
        UUID clientGroupId2 = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        clientGroupRepository.save(new ClientGroup(clientGroupId1, "client group 1", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        clientGroupRepository.save(new ClientGroup(clientGroupId2, "client group 2",
                Set.of(new Client(clientId, clientGroupId2, "Test Client", "NL", false, true, "12.1", 120, false, false, false, false, true, true, true, true, false, true, true, true, 1L, Collections.emptySet())),
                Collections.emptySet(), Collections.emptySet()));

        HttpEntity<List<ClientGroupDTO>> entity = new HttpEntity<>(getHttpHeaders(clientGroupToken));
        ResponseEntity<List<ClientGroupDTO>> responseEntity = restTemplate
                .exchange("/internal/client-groups", HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).contains(new ClientGroupDTO(clientGroupId1, "client group 1"), new ClientGroupDTO(clientGroupId2, "client group 2"));
    }

    @Test
    void addClientGroup() {
        NewClientGroupDTO newClientGroup = new NewClientGroupDTO(null, "client group 1");
        HttpEntity<NewClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups", HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getName()).isEqualTo("client group 1");
    }

    @Test
    void addClientGroupWithFixedUUID() {
        NewClientGroupDTO newClientGroup = new NewClientGroupDTO(UUID.randomUUID(), "client group 1");
        HttpEntity<NewClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups", HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getName()).isEqualTo("client group 1");
    }

    @Test
    void addClientGroupWithExistingUUID() {
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));

        NewClientGroupDTO newClientGroup = new NewClientGroupDTO(clientGroupId, "client group 1");
        HttpEntity<NewClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));

        ResponseEntity<ErrorDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups", HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                });

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDTO("CLS005", "The client group id is linked with an existing client group."));
    }

    @Test
    void updateClientGroup() {
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        UpdateClientGroupDTO newClientGroup = new UpdateClientGroupDTO("new client group");
        HttpEntity<UpdateClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}", HttpMethod.PATCH, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(new ClientGroupDTO(clientGroupId, "new client group"));
    }

    @Test
    void updateClientGroupLargeName() {
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        UpdateClientGroupDTO newClientGroup = new UpdateClientGroupDTO("x".repeat(81));
        HttpEntity<UpdateClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}", HttpMethod.PATCH, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateClientGroupEmptyName() {
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        UpdateClientGroupDTO newClientGroup = new UpdateClientGroupDTO("");
        HttpEntity<UpdateClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}", HttpMethod.PATCH, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateClientGroupClientGroupNotFound() {
        UUID incorrectId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(incorrectId);

        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        UpdateClientGroupDTO newClientGroup = new UpdateClientGroupDTO("new client group");
        HttpEntity<UpdateClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups/" + incorrectId, HttpMethod.PATCH, entity, new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateClientGroupClientGroupTokenIdMismatch() {
        UUID incorrectId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(incorrectId);

        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        UpdateClientGroupDTO newClientGroup = new UpdateClientGroupDTO("new client group");
        HttpEntity<UpdateClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}", HttpMethod.PATCH, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getClientGroupDetails() {
        UUID clientId = UUID.randomUUID();
        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(clientId, "test@yolt.eu", "test", "getClientGroupDetailsInviteCode", LocalDateTime.now(TestConfiguration.FIXED_CLOCK), LocalDateTime.now(TestConfiguration.FIXED_CLOCK).plusHours(24));
        Client client = new Client(
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
                Set.of(clientAdminInvitation)
        );

        ClientDTO clientDTO = new ClientDTO(
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
                List.of(new AdminInviteDTO("test@yolt.eu", "test", LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null))
        );
        EmailDomain emailDomain = new EmailDomain(clientGroupId, "fake.com");

        clientGroupRepository.save(new ClientGroup(
                clientGroupId,
                "client group",
                Set.of(client),
                Set.of(emailDomain),
                Set.of(
                        new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", Set.of(
                                new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", LocalDateTime.now(TestConfiguration.FIXED_CLOCK), LocalDateTime.now(TestConfiguration.FIXED_CLOCK).plusDays(1), LocalDateTime.now(TestConfiguration.FIXED_CLOCK), null)
                        ))
                )
        ));

        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDetailsDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}", HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(new ClientGroupDetailsDTO(clientGroupId,
                "client group",
                List.of(clientDTO),
                List.of(new DomainDTO("fake.com")),
                List.of(new AdminInviteDTO("test@test.com", "test", LocalDateTime.of(2020, 4, 13, 0, 0, 0), LocalDateTime.of(2020, 4, 13, 0, 0, 0)))));
    }

    @Test
    void getClientGroupDetailsClientGroupIdClientGroupTokenMismatch() {
        UUID incorrectId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(incorrectId);

        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        UpdateClientGroupDTO newClientGroup = new UpdateClientGroupDTO("new client group");
        HttpEntity<UpdateClientGroupDTO> entity = new HttpEntity<>(newClientGroup, getHttpHeaders(clientGroupToken));
        ResponseEntity<ClientGroupDTO> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}", HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void addAllowedDomain() {
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Set.of(new EmailDomain(clientGroupId, "ing.com")), Collections.emptySet()));
        DomainDTO domainDTO = new DomainDTO("test.com");
        HttpEntity<DomainDTO> entity = new HttpEntity<>(domainDTO, getHttpHeaders(clientGroupToken));

        ResponseEntity<Void> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}/allowed-domains", HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Set<EmailDomain> emailDomains = clientGroupRepository.findById(clientGroupId).get().getEmailDomains();
        assertThat(emailDomains).contains(new EmailDomain(clientGroupId, "test.com"));
    }

    @Test
    void removeAllowedDomain() {
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Set.of(new EmailDomain(clientGroupId, "ing.com")), Collections.emptySet()));
        DomainDTO domainDTO = new DomainDTO("ing.com");
        HttpEntity<DomainDTO> entity = new HttpEntity<>(domainDTO, getHttpHeaders(clientGroupToken));

        ResponseEntity<Void> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}/allowed-domains", HttpMethod.DELETE, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Set<EmailDomain> emailDomains = clientGroupRepository.findById(clientGroupId).get().getEmailDomains();
        assertThat(emailDomains).isEmpty();
    }

    @Test
    void removeAllowedDomainNotFound() {
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Set.of(new EmailDomain(clientGroupId, "ing.com")), Collections.emptySet()));
        DomainDTO domainDTO = new DomainDTO("yolt.com");
        HttpEntity<DomainDTO> entity = new HttpEntity<>(domainDTO, getHttpHeaders(clientGroupToken));

        ResponseEntity<Void> responseEntity = restTemplate
                .exchange("/internal/client-groups/{clientGroupId}/allowed-domains", HttpMethod.DELETE, entity, new ParameterizedTypeReference<>() {
                }, clientGroupId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
