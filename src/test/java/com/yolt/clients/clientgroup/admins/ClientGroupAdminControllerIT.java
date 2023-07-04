package com.yolt.clients.clientgroup.admins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.admins.dto.NewInviteDTO;
import com.yolt.clients.admins.dto.PortalUserIdDTO;
import com.yolt.clients.admins.portalusersservice.PortalUser;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminDTO;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationCode;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationWithLastCode;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.model.EmailDomain;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@IntegrationTest
class ClientGroupAdminControllerIT {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private ClientGroupAdminInvitationRepository clientGroupAdminInvitationRepository;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private SesClient sesClient;
    @Autowired
    private Clock clock;

    @Captor
    private ArgumentCaptor<SendEmailRequest> sendEmailRequestArgumentCaptor;


    private LocalDateTime now;
    private UUID clientGroupId;
    private ClientGroupToken clientGroupToken;

    @BeforeEach
    public void setup() {
        now = LocalDateTime.now(clock);
        clientGroupId = UUID.randomUUID();
        clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
    }

    @Test
    void getClientGroupAdmins() throws Exception {
        UUID inviteId0 = UUID.randomUUID();
        UUID inviteId1 = UUID.randomUUID();
        UUID inviteId2 = UUID.randomUUID();
        UUID inviteId3 = UUID.randomUUID();
        UUID portalUserId1 = UUID.randomUUID();
        UUID portalUserId2 = UUID.randomUUID();
        UUID portalUserId3 = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        clientGroupAdminInvitationRepository.saveAll(Set.of(
                new ClientGroupAdminInvitation(inviteId0, clientGroupId, "i0@ma.il", "invite 0", Set.of()),
                new ClientGroupAdminInvitation(inviteId1, clientGroupId, "i1@ma.il", "invite 1", Set.of(new ClientGroupAdminInvitationCode("code1", now.minusHours(12), now.plusHours(12), null, null))),
                new ClientGroupAdminInvitation(inviteId2, clientGroupId, "i2@ma.il", "invite 2", Set.of(new ClientGroupAdminInvitationCode("code2", now.minusHours(12), now.plusHours(12), now.minusHours(1), portalUserId1))),
                new ClientGroupAdminInvitation(inviteId3, clientGroupId, "i3@ma.il", "invite 3", Set.of(new ClientGroupAdminInvitationCode("code3", now.minusHours(12), now.plusHours(12), now.minusHours(1), portalUserId2)))
        ));

        Set<PortalUser> clientAdmins = Set.of(
                new PortalUser(portalUserId2, "portalUser2", "p2@ma.il", "org2", inviteId3),
                new PortalUser(portalUserId3, "portalUser3", "p3@ma.il", "org3", null)
        );

        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/clientgroups/details/" + clientGroupId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(OBJECT_MAPPER.writeValueAsBytes(clientAdmins)))
        );

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientGroupToken));
        ResponseEntity<List<ClientGroupAdminDTO>> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/admins", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}, clientGroupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new ClientGroupAdminDTO(portalUserId3, "portalUser3", "org3", "p3@ma.il", null, null, null, null, null, null, null),
                new ClientGroupAdminDTO(portalUserId2, "portalUser2", "org2", "p2@ma.il", inviteId3, "invite 3", "i3@ma.il", now.minusHours(12), now.minusHours(1), now.plusHours(12), ClientGroupAdminDTO.InviteStatus.USED),
                new ClientGroupAdminDTO(null, null, null, null, inviteId2, "invite 2", "i2@ma.il", now.minusHours(12), now.minusHours(1), now.plusHours(12), ClientGroupAdminDTO.InviteStatus.USED),
                new ClientGroupAdminDTO(null, null, null, null, inviteId0, "invite 0", "i0@ma.il", null, null, null, ClientGroupAdminDTO.InviteStatus.EXPIRED),
                new ClientGroupAdminDTO(null, null, null, null, inviteId1, "invite 1", "i1@ma.il", now.minusHours(12), null, now.plusHours(12), ClientGroupAdminDTO.InviteStatus.VALID)
        );
    }

    @Test
    void createClientGroupInvite() throws Exception {
        String email = "mail@yolt.com";
        String name = "Name " + UUID.randomUUID();
        NewInviteDTO newInviteDTO = new NewInviteDTO(email, name);

        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        Set<PortalUser> clientAdmins = Set.of();
        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/clientgroups/details/" + clientGroupId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(OBJECT_MAPPER.writeValueAsBytes(clientAdmins)))
        );

        HttpEntity<NewInviteDTO> requestEntity = new HttpEntity<>(newInviteDTO, getHttpHeaders(clientGroupToken));
        ResponseEntity<Void> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/admins", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientGroupId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Set<ClientGroupAdminInvitationWithLastCode> invites = clientGroupAdminInvitationRepository.findAllByClientGroupIdWithLastInvite(clientGroupId);
        assertThat(invites).hasSizeGreaterThanOrEqualTo(1);
        Optional<ClientGroupAdminInvitation> invitation = invites.stream()
                .filter(inv -> inv.getName().equals(name))
                .findAny()
                .map(ClientGroupAdminInvitationWithLastCode::getId)
                .flatMap(clientGroupAdminInvitationRepository::findById);

        assertThat(invitation).isNotEmpty();
        assertThat(invitation.get().getCodes()).hasSize(1);

        verify(sesClient).sendEmail(sendEmailRequestArgumentCaptor.capture());
        SendEmailRequest sendMailRequest = sendEmailRequestArgumentCaptor.getValue();
        assertThat(sendMailRequest.destination().toAddresses()).contains(email);
        assertThat(sendMailRequest.source()).isEqualTo("no-reply-clients@yolt.com");
        assertThat(sendMailRequest.message().subject().data()).isEqualTo("Invitation for Client Group Admin");
    }

    @Test
    void resendClientGroupInvite() throws Exception {
        UUID invitationId = UUID.randomUUID();
        String email = invitationId + "-mail@yolt.com";
        String name = "Name " + invitationId;

        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Set.of(), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        clientGroupAdminInvitationRepository.save(new ClientGroupAdminInvitation(invitationId, clientGroupId, email, name, Set.of(new ClientGroupAdminInvitationCode("someRandomCode", now.minusHours(25), now.minusHours(1)))));

        Set<PortalUser> clientAdmins = Set.of();
        wireMockServer.stubFor(
                WireMock.get("/dev-portal/internal-api/clientgroups/details/" + clientGroupId)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(OBJECT_MAPPER.writeValueAsBytes(clientAdmins)))
        );

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, getHttpHeaders(clientGroupToken));
        ResponseEntity<Void> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/admins/{invitationId}/resend", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientGroupId, invitationId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Set<ClientGroupAdminInvitationWithLastCode> invites = clientGroupAdminInvitationRepository.findAllByClientGroupIdWithLastInvite(clientGroupId);
        assertThat(invites).hasSizeGreaterThanOrEqualTo(1);
        Optional<ClientGroupAdminInvitation> invitation = invites.stream()
                .filter(inv -> inv.getName().equals(name))
                .findAny()
                .map(ClientGroupAdminInvitationWithLastCode::getId)
                .flatMap(clientGroupAdminInvitationRepository::findById);

        assertThat(invitation).isNotEmpty();
        assertThat(invitation.get().getCodes()).hasSize(2);

        verify(sesClient).sendEmail(sendEmailRequestArgumentCaptor.capture());
        SendEmailRequest sendMailRequest = sendEmailRequestArgumentCaptor.getValue();
        assertThat(sendMailRequest.destination().toAddresses()).contains(email);
        assertThat(sendMailRequest.source()).isEqualTo("no-reply-clients@yolt.com");
        assertThat(sendMailRequest.message().subject().data()).isEqualTo("Invitation for Client Group Admin");
    }

    @Test
    void removeClientGroupAuthorization() {
        UUID portalUserId = UUID.randomUUID();
        wireMockServer.stubFor(
                WireMock.delete("/dev-portal/internal-api/clientgroups/" + clientGroupId + "/admins/" + portalUserId)
                        .willReturn(aResponse().withStatus(202))
        );

        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group", Collections.emptySet(), Set.of(new EmailDomain(clientGroupId, "yolt.com")), Collections.emptySet()));

        HttpEntity<PortalUserIdDTO> requestEntity = new HttpEntity<>(new PortalUserIdDTO(portalUserId), getHttpHeaders(clientGroupToken));
        ResponseEntity<Void> response = testRestTemplate.exchange("/internal/client-groups/{clientGroupId}/admins/remove-authorization", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}, clientGroupId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private HttpHeaders getHttpHeaders(ClientGroupToken clientGroupToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized());
        return headers;
    }
}