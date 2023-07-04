package com.yolt.clients.admins;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.TestConfiguration;
import com.yolt.clients.admins.dto.ConnectionDTO;
import com.yolt.clients.admins.dto.InvitationCodeDTO;
import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.client.admins.models.ClientAdminInvitationCode;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationCode;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ConnectControllerIT {

    private static final LocalDateTime NOW = LocalDateTime.now(TestConfiguration.FIXED_CLOCK);
    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private WireMockServer wireMockServer;

    private UUID clientGroupId, clientId;
    private String invitationCode;

    @BeforeEach
    void setUp() {
        clientGroupId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        invitationCode = new String(Base64.getEncoder().encode(bytes));
    }

    @Test
    void connectClientGroupAdmin() {
        UUID portalUserId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Set.of(new ClientGroupAdminInvitation(clientGroupId, "test@test.com", "test", invitationCode, NOW, NOW.plusHours(24)))));
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, portalUserId);

        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/clientgroups/" + clientGroupId + "/is-admin/" + portalUserId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"admin\":\"false\"}"))
        );

        ResponseEntity<ConnectionDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getClientGroupId()).isEqualTo(clientGroupId);

        ClientGroup clientGroup = clientGroupRepository.findById(clientGroupId).get();
        ClientGroupAdminInvitation invitation = clientGroup.getClientGroupAdminInvitations().iterator().next();
        assertThat(invitation.getCodes().iterator().next().getUsedAt()).isEqualTo(LocalDateTime.of(2020, 4, 13, 0, 0, 0));
    }

    @Test
    void connectClientGroupAdmin_already_admin() {
        UUID portalUserId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Collections.emptySet(), Set.of(new ClientGroupAdminInvitation(clientGroupId, "test@test.com", "test", invitationCode, NOW, NOW.plusHours(24)))));
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, portalUserId);

        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/clientgroups/" + clientGroupId + "/is-admin/" + portalUserId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"admin\":\"true\"}"))
        );

        ResponseEntity<ErrorDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getCode()).isEqualTo("CLS026");
    }

    @Test
    void connectClientGroupAdminActivationCodeUsed() {
        clientGroupRepository.save(new ClientGroup(
                clientGroupId,
                "client group",
                Collections.emptySet(),
                Collections.emptySet(),
                Set.of(
                        new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", Set.of(
                                new ClientGroupAdminInvitationCode(invitationCode, NOW, NOW.plusDays(1), NOW, null)
                        ))
                )
        ));
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, null);

        ResponseEntity<ErrorDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getCode()).isEqualTo("CLS021");
    }

    @Test
    void connectClientGroupAdminActivationCodeTooOld() {
        clientGroupRepository.save(new ClientGroup(
                clientGroupId,
                "client group",
                Collections.emptySet(),
                Collections.emptySet(),
                Set.of(
                        new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", Set.of(
                                new ClientGroupAdminInvitationCode(invitationCode, NOW.minusHours(25), NOW.minusHours(1), null, null)
                        ))
                )
        ));
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, null);

        ResponseEntity<ErrorDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getCode()).isEqualTo("CLS021");
    }

    @Test
    void connectClientGroupAdminActivationCodeNotFound() {
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, null);

        ResponseEntity<ConnectionDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void connectClientAdmin() {
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(clientId, "test@yolt.eu", "test", invitationCode, NOW, NOW.plusHours(24));
        Client client = new Client(clientId,
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
                Set.of(clientAdminInvitation));
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Collections.emptySet(), Collections.emptySet()));
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, portalUserId);

        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/clients/" + clientId + "/is-admin/" + portalUserId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"admin\":\"false\"}"))
        );

        ResponseEntity<ConnectionDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getClientId()).isEqualTo(clientId);
    }

    @Test
    void connectClientAdmin_is_already_an_admin() {
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(clientId, "test@yolt.eu", "test", invitationCode, NOW, NOW.plusHours(24));
        Client client = new Client(clientId,
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
                Set.of(clientAdminInvitation));
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Collections.emptySet(), Collections.emptySet()));
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, portalUserId);

        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/clients/" + clientId + "/is-admin/" + portalUserId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"admin\":\"true\"}"))
        );

        ResponseEntity<ErrorDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getCode()).isEqualTo("CLS025");
    }

    @Test
    void connectClientAdminCodeUsed() {
        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@yolt.eu", "test", Set.of());
        Client client = new Client(clientId,
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
                Set.of(clientAdminInvitation));
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Collections.emptySet(), Collections.emptySet()));
        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, null);

        ResponseEntity<ConnectionDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void connectClientAdminActivationCodeTooOld() {
        LocalDateTime localDateTime = NOW.minusHours(25);

        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@yolt.eu", "test", Set.of(new ClientAdminInvitationCode(invitationCode, localDateTime, localDateTime.plusHours(24))));
        Client client = new Client(clientId,
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
                Set.of(clientAdminInvitation));

        clientGroupRepository.save(new ClientGroup(
                clientGroupId,
                "client group",
                Set.of(client),
                Collections.emptySet(),
                Set.of()
        ));

        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO(invitationCode, null);

        ResponseEntity<ErrorDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getCode()).isEqualTo("CLS021");
    }

    @Test
    void connectClientAdminActivationCodeNotFound() {
        ClientAdminInvitation clientAdminInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@yolt.eu", "test", Set.of(new ClientAdminInvitationCode(invitationCode, NOW, NOW.plusHours(24))));
        Client client = new Client(clientId,
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
                Set.of(clientAdminInvitation));

        clientGroupRepository.save(new ClientGroup(
                clientGroupId,
                "client group",
                Set.of(client),
                Collections.emptySet(),
                Set.of(
                        new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", Set.of(
                                new ClientGroupAdminInvitationCode(invitationCode, NOW.minusHours(25), NOW.minusHours(1), null, null)
                        ))
                )
        ));

        InvitationCodeDTO invitationCodeDTO = new InvitationCodeDTO("4", null);

        ResponseEntity<ConnectionDTO> responseEntity = restTemplate
                .exchange("/internal/connect", HttpMethod.POST, new HttpEntity<>(invitationCodeDTO), new ParameterizedTypeReference<>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
