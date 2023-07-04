package com.yolt.clients.client.admins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.admins.dto.NewInviteDTO;
import com.yolt.clients.admins.dto.PortalUserIdDTO;
import com.yolt.clients.admins.portalusersservice.PortalUser;
import com.yolt.clients.client.admins.models.ClientAdminDTO;
import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.client.admins.models.ClientAdminInvitationCode;
import com.yolt.clients.client.admins.models.ClientAdminInvitationWithLastCode;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.model.EmailDomain;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@IntegrationTest
class ClientAdminControllerIT {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private ClientAdminInvitationRepository clientAdminInvitationRepository;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private SesClient sesClient;
    @Autowired
    private Clock clock;

    @Captor
    private ArgumentCaptor<SendEmailRequest> sendEmailRequestArgumentCaptor;

    private LocalDateTime now;
    private UUID clientId;
    private UUID clientGroupId;
    private ClientToken clientToken;

    @BeforeEach
    public void setup() {
        now = LocalDateTime.now(clock);
        clientId = UUID.randomUUID();
        clientGroupId = UUID.randomUUID();
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
    }

    @Test
    void getClientAdmins() throws Exception {
        UUID inviteId0 = UUID.randomUUID();
        UUID inviteId1 = UUID.randomUUID();
        UUID inviteId2 = UUID.randomUUID();
        UUID inviteId3 = UUID.randomUUID();
        UUID portalUserId1 = UUID.randomUUID();
        UUID portalUserId2 = UUID.randomUUID();
        UUID portalUserId3 = UUID.randomUUID();
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
                new HashSet<>());
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        clientAdminInvitationRepository.saveAll(Set.of(
                new ClientAdminInvitation(inviteId0, clientId, "i0@ma.il", "invite 0", Set.of()),
                new ClientAdminInvitation(inviteId1, clientId, "i1@ma.il", "invite 1", Set.of(new ClientAdminInvitationCode("code1", now.minusHours(12), now.plusHours(12), null, null))),
                new ClientAdminInvitation(inviteId2, clientId, "i2@ma.il", "invite 2", Set.of(new ClientAdminInvitationCode("code2", now.minusHours(12), now.plusHours(12), now.minusHours(1), portalUserId1))),
                new ClientAdminInvitation(inviteId3, clientId, "i3@ma.il", "invite 3", Set.of(new ClientAdminInvitationCode("code3", now.minusHours(12), now.plusHours(12), now.minusHours(1), portalUserId2)))
        ));

        Set<PortalUser> clientAdmins = Set.of(
                new PortalUser(portalUserId2, "portalUser2", "p2@ma.il", "org2", inviteId3),
                new PortalUser(portalUserId3, "portalUser3", "p3@ma.il", "org3", null)
        );

        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/client/details/" + clientId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(OBJECT_MAPPER.writeValueAsBytes(clientAdmins)))
        );

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<List<ClientAdminDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/admins", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new ClientAdminDTO(portalUserId3, "portalUser3", "org3", "p3@ma.il", null, null, null, null, null, null, null),
                new ClientAdminDTO(portalUserId2, "portalUser2", "org2", "p2@ma.il", inviteId3, "invite 3", "i3@ma.il", now.minusHours(12), now.minusHours(1), now.plusHours(12), ClientAdminDTO.InviteStatus.USED),
                new ClientAdminDTO(null, null, null, null, inviteId2, "invite 2", "i2@ma.il", now.minusHours(12), now.minusHours(1), now.plusHours(12), ClientAdminDTO.InviteStatus.USED),
                new ClientAdminDTO(null, null, null, null, inviteId0, "invite 0", "i0@ma.il", null, null, null, ClientAdminDTO.InviteStatus.EXPIRED),
                new ClientAdminDTO(null, null, null, null, inviteId1, "invite 1", "i1@ma.il", now.minusHours(12), null, now.plusHours(12), ClientAdminDTO.InviteStatus.VALID)
        );
    }

    @Test
    void createClientInvite() throws Exception {
        String email = "mail@yolt.com";
        String name = "Name " + UUID.randomUUID();
        NewInviteDTO newInviteDTO = new NewInviteDTO(email, name);

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
                new HashSet<>());
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        Set<PortalUser> clientAdmins = Set.of();
        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/client/details/" + clientId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(OBJECT_MAPPER.writeValueAsBytes(clientAdmins)))
        );

        HttpEntity<NewInviteDTO> requestEntity = new HttpEntity<>(newInviteDTO, getHttpHeaders(clientToken));
        ResponseEntity<Void> response = testRestTemplate.exchange("/internal/clients/{clientId}/admins", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Set<ClientAdminInvitationWithLastCode> invites = clientAdminInvitationRepository.findAllByClientIdWithLastInvite(clientId);
        assertThat(invites).hasSizeGreaterThanOrEqualTo(1);
        Optional<ClientAdminInvitation> invitation = invites.stream()
                .filter(inv -> inv.getName().equals(name))
                .findAny()
                .map(ClientAdminInvitationWithLastCode::getId)
                .flatMap(clientAdminInvitationRepository::findById);

        assertThat(invitation).isNotEmpty();
        assertThat(invitation.get().getCodes()).hasSize(1);

        verify(sesClient).sendEmail(sendEmailRequestArgumentCaptor.capture());
        SendEmailRequest sendMailRequest = sendEmailRequestArgumentCaptor.getValue();
        assertThat(sendMailRequest.destination().toAddresses()).contains(email);
        assertThat(sendMailRequest.source()).isEqualTo("no-reply-clients@yolt.com");
        assertThat(sendMailRequest.message().subject().data()).isEqualTo("Invitation for Client Admin");
    }

    @Test
    void createClientInvite_too_many_invites() throws Exception {
        String email = "mail@yolt.com";
        String name = "Name " + UUID.randomUUID();
        NewInviteDTO newInviteDTO = new NewInviteDTO(email, name);

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
                new HashSet<>());
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        ClientAdminInvitation invite1 = new ClientAdminInvitation(clientId, "test1@yolt.com", "test 1", "inviteCode1", now, now.plusHours(24));
        clientAdminInvitationRepository.save(invite1);
        ClientAdminInvitation invite2 = new ClientAdminInvitation(clientId, "test2@yolt.com", "test 2", "inviteCode2", now, now.plusHours(24));
        clientAdminInvitationRepository.save(invite2);

        Set<PortalUser> clientAdmins = Set.of();
        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/client/details/" + clientId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(OBJECT_MAPPER.writeValueAsBytes(clientAdmins)))
        );

        HttpEntity<NewInviteDTO> requestEntity = new HttpEntity<>(newInviteDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/admins", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("CLS027");

        Set<ClientAdminInvitationWithLastCode> invites = clientAdminInvitationRepository.findAllByClientIdWithLastInvite(clientId);
        assertThat(invites).hasSize(2);
    }

    @Test
    void resendClientInvite() {
        UUID invitationId = UUID.randomUUID();
        String email = invitationId + "-mail@yolt.com";
        String name = "Name " + invitationId;

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
                new HashSet<>());
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        clientAdminInvitationRepository.save(new ClientAdminInvitation(invitationId, clientId, email, name, Set.of(new ClientAdminInvitationCode("someRandomCode", now.minusHours(25), now.minusHours(1)))));

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<Void> response = testRestTemplate.exchange("/internal/clients/{clientId}/admins/{invitationId}/resend", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientId, invitationId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Set<ClientAdminInvitationWithLastCode> invites = clientAdminInvitationRepository.findAllByClientIdWithLastInvite(clientId);
        assertThat(invites).hasSizeGreaterThanOrEqualTo(1);
        Optional<ClientAdminInvitation> invitation = invites.stream()
                .filter(inv -> inv.getName().equals(name))
                .findAny()
                .map(ClientAdminInvitationWithLastCode::getId)
                .flatMap(clientAdminInvitationRepository::findById);

        assertThat(invitation).isNotEmpty();
        assertThat(invitation.get().getCodes()).hasSize(2);

        verify(sesClient).sendEmail(sendEmailRequestArgumentCaptor.capture());
        SendEmailRequest sendMailRequest = sendEmailRequestArgumentCaptor.getValue();
        assertThat(sendMailRequest.destination().toAddresses()).contains(email);
        assertThat(sendMailRequest.source()).isEqualTo("no-reply-clients@yolt.com");
        assertThat(sendMailRequest.message().subject().data()).isEqualTo("Invitation for Client Admin");
    }

    @Test
    void removeClientAuthorization() {
        UUID portalUserId = UUID.randomUUID();
        wireMockServer.stubFor(
                WireMock.delete("/dev-portal/internal-api/clients/" + clientId + "/admins/" + portalUserId)
                        .willReturn(aResponse().withStatus(202))
        );

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
                new HashSet<>());
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(client), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        HttpEntity<PortalUserIdDTO> requestEntity = new HttpEntity<>(new PortalUserIdDTO(portalUserId), getHttpHeaders(clientToken));
        ResponseEntity<Void> response = testRestTemplate.exchange("/internal/clients/{clientId}/admins/remove-authorization", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}
