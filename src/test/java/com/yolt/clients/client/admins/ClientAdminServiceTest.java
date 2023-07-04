package com.yolt.clients.client.admins;

import com.yolt.clients.admins.dto.ConnectionDTO;
import com.yolt.clients.admins.dto.NewInviteDTO;
import com.yolt.clients.admins.portalusersservice.DevPortalUserService;
import com.yolt.clients.admins.portalusersservice.PortalUser;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.admins.models.ClientAdminDTO;
import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.client.admins.models.ClientAdminInvitationCode;
import com.yolt.clients.client.admins.models.ClientAdminInvitationWithLastCode;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.email.EmailService;
import com.yolt.clients.exceptions.ClientGroupNotFoundException;
import com.yolt.clients.exceptions.ClientNotFoundException;
import com.yolt.clients.exceptions.InvalidDomainException;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.model.EmailDomain;
import lombok.Value;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAdminServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    private ClientAdminService clientAdminService;

    @Mock
    private ClientAdminInvitationRepository clientAdminInvitationRepository;
    @Mock
    private ClientGroupRepository clientGroupRepository;
    @Mock
    private ClientsRepository clientRepository;
    @Mock
    private DevPortalUserService devPortalUserService;
    @Mock
    private EmailService emailService;

    private int expirationHours;
    private int maxInvites;

    @Captor
    private ArgumentCaptor<ClientAdminInvitation> clientAdminInvitationArgumentCaptor;

    @Mock
    private ClientToken clientToken;
    private UUID clientId;
    private UUID clientGroupId;
    private Client client;
    private ClientGroup clientGroup;

    @BeforeEach
    void setUp() {
        expirationHours = 24;
        maxInvites = 5;
        int resendTimeout = 5;
        clientAdminService = new ClientAdminService(
                clientAdminInvitationRepository,
                clientGroupRepository,
                clientRepository,
                devPortalUserService,
                emailService,
                FIXED_CLOCK,
                expirationHours,
                maxInvites,
                resendTimeout
        );
        clientId = UUID.randomUUID();
        clientGroupId = UUID.randomUUID();
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
                Set.of());
        clientGroup = new ClientGroup(clientGroupId, "client group", Set.of(client), Set.of(new EmailDomain(clientGroupId, "provider.com")), Collections.emptySet());
    }

    @Test
    void getClientAdmins() {
        UUID inviteId0 = UUID.randomUUID();
        UUID inviteId1 = UUID.randomUUID();
        UUID inviteId2 = UUID.randomUUID();
        UUID inviteId3 = UUID.randomUUID();
        UUID inviteId4 = UUID.randomUUID();
        UUID inviteId5 = UUID.randomUUID();
        UUID inviteId6 = UUID.randomUUID();
        UUID inviteId7 = UUID.randomUUID();
        UUID inviteId8 = UUID.randomUUID();
        UUID inviteId9 = UUID.randomUUID();
        UUID inviteId10 = UUID.randomUUID();
        UUID portalUserId1 = UUID.randomUUID();
        UUID portalUserId2 = UUID.randomUUID();
        UUID portalUserId3 = UUID.randomUUID();
        UUID portalUserId4 = UUID.randomUUID();
        UUID portalUserId5 = UUID.randomUUID();
        UUID portalUserId6 = UUID.randomUUID();

        Set<ClientAdminInvitationWithLastCode> latestInvites = Set.of(
                new ClientAdminInvitationWithLastCodeImpl(inviteId0, clientId, "i0@ma.il", "invite 0", null, null, null, null, 0),
                new ClientAdminInvitationWithLastCodeImpl(inviteId1, clientId, "i1@ma.il", "invite 1", NOW.minusHours(12), NOW.plusHours(12), null, null, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId2, clientId, "i2@ma.il", "invite 2", NOW.minusHours(13), NOW.plusHours(11), null, null, 2),
                new ClientAdminInvitationWithLastCodeImpl(inviteId3, clientId, "i3@ma.il", "invite 3", NOW.minusHours(24), NOW, null, null, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId4, clientId, "i4@ma.il", "invite 4", NOW.minusHours(25), NOW.minusHours(1), null, null, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId5, clientId, "i5@ma.il", "invite 5", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(1), null, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId6, clientId, "i6@ma.il", "invite 6", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(2), null, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId7, clientId, "i7@ma.il", "invite 7", NOW.minusHours(26), NOW.minusHours(1), NOW.minusHours(1), portalUserId1, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId8, clientId, "i8@ma.il", "invite 8", NOW.minusHours(27), NOW.minusHours(1), NOW.minusHours(2), portalUserId2, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId9, clientId, "i9@ma.il", "invite 9", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(3), portalUserId3, 1),
                new ClientAdminInvitationWithLastCodeImpl(inviteId10, clientId, "i10@ma.il", "invite 10", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(4), portalUserId4, 1)
        );

        Set<PortalUser> clientAdmins = Set.of(
                new PortalUser(portalUserId3, "portalUser3", "p3@ma.il", "org3", inviteId9),
                new PortalUser(portalUserId4, "portalUser4", "p4@ma.il", "org4", inviteId10),
                new PortalUser(portalUserId5, "portalUser5", "p5@ma.il", "org5", null),
                new PortalUser(portalUserId6, "portalUser6", "p6@ma.il", "org6", null)
        );
        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientAdminInvitationRepository.findAllByClientIdWithLastInvite(clientId)).thenReturn(latestInvites);
        when(devPortalUserService.getClientAdmins(clientToken)).thenReturn(clientAdmins);

        List<ClientAdminDTO> result = clientAdminService.getClientAdmins(clientToken);

        assertThat(result).containsExactly(
                // portal users without invite, sorted on name
                new ClientAdminDTO(portalUserId5, "portalUser5", "org5", "p5@ma.il", null, null, null, null, null, null, null),
                new ClientAdminDTO(portalUserId6, "portalUser6", "org6", "p6@ma.il", null, null, null, null, null, null, null),

                // portal users with invite, longest user first
                new ClientAdminDTO(portalUserId4, "portalUser4", "org4", "p4@ma.il", inviteId10, "invite 10", "i10@ma.il", NOW.minusHours(25), NOW.minusHours(4), NOW.minusHours(1), ClientAdminDTO.InviteStatus.USED),
                new ClientAdminDTO(portalUserId3, "portalUser3", "org3", "p3@ma.il", inviteId9, "invite 9", "i9@ma.il", NOW.minusHours(25), NOW.minusHours(3), NOW.minusHours(1), ClientAdminDTO.InviteStatus.USED),

                // invite without any actual codes
                new ClientAdminDTO(null, null, null, null, inviteId0, "invite 0", "i0@ma.il", null, null, null, ClientAdminDTO.InviteStatus.EXPIRED),

                // Non used invites, oldest first
                new ClientAdminDTO(null, null, null, null, inviteId4, "invite 4", "i4@ma.il", NOW.minusHours(25), null, NOW.minusHours(1), ClientAdminDTO.InviteStatus.EXPIRED),
                new ClientAdminDTO(null, null, null, null, inviteId3, "invite 3", "i3@ma.il", NOW.minusHours(24), null, NOW, ClientAdminDTO.InviteStatus.VALID),
                new ClientAdminDTO(null, null, null, null, inviteId2, "invite 2", "i2@ma.il", NOW.minusHours(13), null, NOW.plusHours(11), ClientAdminDTO.InviteStatus.RESENT),
                new ClientAdminDTO(null, null, null, null, inviteId1, "invite 1", "i1@ma.il", NOW.minusHours(12), null, NOW.plusHours(12), ClientAdminDTO.InviteStatus.VALID),

                // used invites, without a portal user longest used first
                new ClientAdminDTO(null, null, null, null, inviteId8, "invite 8", "i8@ma.il", NOW.minusHours(27), NOW.minusHours(2), NOW.minusHours(1), ClientAdminDTO.InviteStatus.USED),
                new ClientAdminDTO(null, null, null, null, inviteId6, "invite 6", "i6@ma.il", NOW.minusHours(25), NOW.minusHours(2), NOW.minusHours(1), ClientAdminDTO.InviteStatus.USED),
                new ClientAdminDTO(null, null, null, null, inviteId7, "invite 7", "i7@ma.il", NOW.minusHours(26), NOW.minusHours(1), NOW.minusHours(1), ClientAdminDTO.InviteStatus.USED),
                new ClientAdminDTO(null, null, null, null, inviteId5, "invite 5", "i5@ma.il", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(1), ClientAdminDTO.InviteStatus.USED)
        );
    }

    @Test
    void inviteClientAdmin() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(devPortalUserService.isAdminForClient(clientToken, mail)).thenReturn(false);
        when(clientAdminInvitationRepository.countClientAdminInvitationByClientId(clientId)).thenReturn(4);
        when(clientAdminInvitationRepository.existsByClientIdAndEmail(clientId, mail)).thenReturn(false);
        doAnswer(returnsFirstArg()).when(clientAdminInvitationRepository).save(any());

        clientAdminService.inviteClientAdmin(clientToken, newInvite);

        verify(clientAdminInvitationRepository).save(clientAdminInvitationArgumentCaptor.capture());
        ClientAdminInvitation invitation = clientAdminInvitationArgumentCaptor.getValue();
        assertThat(invitation.getCodes()).hasSize(1);
        assertThat(invitation.getEmail()).isEqualTo(mail);
        assertThat(invitation.getName()).isEqualTo(name);

        ClientAdminInvitationCode invitationCode = invitation.getCodes().stream().findAny().get();
        assertThat(invitationCode.getCode()).isNotBlank();
        assertThat(invitationCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(invitationCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(invitationCode.getUsedAt()).isNull();
        assertThat(invitationCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser(mail, name, invitationCode.getCode(), "client", NOW.plusHours(expirationHours), false);
    }

    @Test
    void inviteClientAdmin_invalid_domain() {
        String mail = "mail@invalid.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));

        InvalidDomainException ex = assertThrows(InvalidDomainException.class, () -> clientAdminService.inviteClientAdmin(clientToken, newInvite));
        assertThat(ex).hasMessage("The domain invalid.com is not in the allowed list of domains for client group id " + clientGroupId);
    }

    @Test
    void inviteClientAdmin_too_many_invites() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientAdminInvitationRepository.countClientAdminInvitationByClientId(clientId)).thenReturn(5);

        TooManyClientAdminInvitesException ex = assertThrows(TooManyClientAdminInvitesException.class, () -> clientAdminService.inviteClientAdmin(clientToken, newInvite));
        assertThat(ex).hasMessage("There are too many (more than %d) invites for client %s".formatted(maxInvites, clientId));
    }

    @Test
    void inviteClientAdmin_email_known_for_invite() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientAdminInvitationRepository.countClientAdminInvitationByClientId(clientId)).thenReturn(3);
        when(clientAdminInvitationRepository.existsByClientIdAndEmail(clientId, mail)).thenReturn(true);

        ClientAdminEmailInUseException ex = assertThrows(ClientAdminEmailInUseException.class, () -> clientAdminService.inviteClientAdmin(clientToken, newInvite));
        assertThat(ex).hasMessage("could not invite %s for client %s, reason: already invited".formatted(mail, clientId));
    }

    @Test
    void inviteClientAdmin_email_known_as_admin() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientAdminInvitationRepository.countClientAdminInvitationByClientId(clientId)).thenReturn(0);
        when(clientAdminInvitationRepository.existsByClientIdAndEmail(clientId, mail)).thenReturn(false);
        when(devPortalUserService.isAdminForClient(clientToken, mail)).thenReturn(true);

        ClientAdminEmailInUseException ex = assertThrows(ClientAdminEmailInUseException.class, () -> clientAdminService.inviteClientAdmin(clientToken, newInvite));
        assertThat(ex).hasMessage("could not invite %s for client %s, reason: already an admin".formatted(mail, clientId));
    }

    @Test
    void inviteClientAdmin_client_not_found() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);
        clientGroup = new ClientGroup(clientGroupId, "client group", Set.of(), Set.of(new EmailDomain(clientGroupId, "provider.com")), Collections.emptySet());

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));

        ClientNotFoundException ex = assertThrows(ClientNotFoundException.class, () -> clientAdminService.inviteClientAdmin(clientToken, newInvite));
        assertThat(ex).hasMessage("could not find the client with clientId %s".formatted(clientId));
    }

    @Test
    void inviteClientAdmin_client_group_not_found() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.empty());

        ClientGroupNotFoundException ex = assertThrows(ClientGroupNotFoundException.class, () -> clientAdminService.inviteClientAdmin(clientToken, newInvite));
        assertThat(ex).hasMessage("No client group found for client group id %s".formatted(clientGroupId));
    }

    @Test
    void resendClientInvite_no_existing_codes() {
        UUID invitationId = UUID.randomUUID();
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>());

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientAdminService.resendClientInvite(clientToken, invitationId);

        ClientAdminInvitationCode newCode = new ArrayList<>(existingInvitation.getCodes()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client", NOW.plusHours(expirationHours), false);
    }

    @Test
    void resendClientInvite_expired() {
        UUID invitationId = UUID.randomUUID();
        ClientAdminInvitationCode oldCode = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(25), NOW.minusHours(1), null, null);
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(oldCode)));

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientAdminService.resendClientInvite(clientToken, invitationId);

        ClientAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client", NOW.plusHours(expirationHours), false);
    }

    @Test
    void resendClientInvite_valid() {
        UUID invitationId = UUID.randomUUID();
        ClientAdminInvitationCode oldCode = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(oldCode)));

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientAdminService.resendClientInvite(clientToken, invitationId);

        ClientAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client", NOW.plusHours(expirationHours), false);
    }

    @Test
    void resendClientInvite_resent() {
        UUID invitationId = UUID.randomUUID();
        ClientAdminInvitationCode oldCode1 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode oldCode2 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientAdminService.resendClientInvite(clientToken, invitationId);

        ClientAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode1) && !code.equals(oldCode2)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client", NOW.plusHours(expirationHours), false);
    }

    @Test
    void resendClientInvite_notResentBecauseTimeoutIsNotExceeded() {
        UUID invitationId = UUID.randomUUID();
        ClientAdminInvitationCode oldCode1 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode oldCode2 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusMinutes(3), NOW.plusHours(2), null, null);
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));

        clientAdminService.resendClientInvite(clientToken, invitationId);

        verify(emailService, times(0)).sendInvitationForUser(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void resendClientInvite_used_no_portal_user_id() {
        UUID invitationId = UUID.randomUUID();
        ClientAdminInvitationCode oldCode1 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode oldCode2 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), NOW, null);
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientAdminService.resendClientInvite(clientToken, invitationId);

        ClientAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode1) && !code.equals(oldCode2)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client", NOW.plusHours(expirationHours), false);
    }

    @Test
    void resendClientInvite_used_portal_user_not_linked_anymore() {
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode oldCode1 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode oldCode2 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), NOW, portalUserId);
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(devPortalUserService.isAdminForClient(clientId, portalUserId)).thenReturn(false);
        when(clientAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientAdminService.resendClientInvite(clientToken, invitationId);

        ClientAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode1) && !code.equals(oldCode2)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client", NOW.plusHours(expirationHours), false);
    }

    @Test
    void resendClientInvite_used_portal_user_still_linked() {
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode oldCode1 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode oldCode2 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), NOW, portalUserId);
        ClientAdminInvitation existingInvitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(devPortalUserService.isAdminForClient(clientId, portalUserId)).thenReturn(true);

        ClientAdminInviteUsedException ex = assertThrows(ClientAdminInviteUsedException.class, () -> clientAdminService.resendClientInvite(clientToken, invitationId));
        assertThat(ex).hasMessage("the client admin invite %s for client %s is used and the portal user is still linked".formatted(invitationId, clientId));
    }

    @Test
    void resendClientInvite_invite_does_not_exists() {
        UUID invitationId = UUID.randomUUID();
        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)).thenReturn(Optional.empty());
        ClientAdminInvitationNotFoundException ex = assertThrows(ClientAdminInvitationNotFoundException.class, () -> clientAdminService.resendClientInvite(clientToken, invitationId));
        assertThat(ex).hasMessage("Could not find invitation %s for client %s".formatted(invitationId, clientId));
    }

    @Test
    void resendClientInvite_client_does_not_exists() {
        UUID invitationId = UUID.randomUUID();
        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());
        ClientNotFoundException ex = assertThrows(ClientNotFoundException.class, () -> clientAdminService.resendClientInvite(clientToken, invitationId));
        assertThat(ex).hasMessage("could not find the client with clientId %s".formatted(clientId));
    }

    @Test
    void connect_valid() {
        reset(clientToken);
        String code = "abcdefghhijkl";
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode invitationCode = new ClientAdminInvitationCode(code, NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientAdminInvitation invitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode)));

        when(clientAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));
        when(clientAdminInvitationRepository.save(invitation)).thenReturn(invitation);
        when(clientRepository.findByClientIdAndDeletedIsFalse(clientId)).thenReturn(Optional.of(client));

        Optional<ConnectionDTO> result = clientAdminService.connect(code, portalUserId);

        assertThat(result).contains(new ConnectionDTO(invitation.getId(), null, clientId));
        assertThat(invitationCode.getUsedAt()).isEqualTo(NOW);
        assertThat(invitationCode.getUsedBy()).isEqualTo(portalUserId);
        verify(clientAdminInvitationRepository).save(invitation);
    }

    @Test
    void connect_valid_deleted_client() {
        reset(clientToken);
        String code = "abcdefghhijkl";
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode invitationCode = new ClientAdminInvitationCode(code, NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientAdminInvitation invitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode)));

        when(clientAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));
        when(clientRepository.findByClientIdAndDeletedIsFalse(clientId)).thenReturn(Optional.empty());

        ClientNotFoundException ex = assertThrows(ClientNotFoundException.class, () -> clientAdminService.connect(code, portalUserId));
        assertThat(ex).hasMessage("could not find the client with clientId %s".formatted(clientId));
    }

    @Test
    void connect_resent() {
        reset(clientToken);
        String code = "abcdefghhijkl";
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode invitationCode1 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode invitationCode2 = new ClientAdminInvitationCode(code, NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientAdminInvitation invitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode1, invitationCode2)));

        when(clientAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));
        when(clientAdminInvitationRepository.save(invitation)).thenReturn(invitation);
        when(clientRepository.findByClientIdAndDeletedIsFalse(clientId)).thenReturn(Optional.of(client));

        Optional<ConnectionDTO> result = clientAdminService.connect(code, portalUserId);

        assertThat(result).contains(new ConnectionDTO(invitation.getId(), null, clientId));
        assertThat(invitationCode1.getUsedAt()).isNull();
        assertThat(invitationCode1.getUsedBy()).isNull();
        assertThat(invitationCode2.getUsedAt()).isEqualTo(NOW);
        assertThat(invitationCode2.getUsedBy()).isEqualTo(portalUserId);
        verify(clientAdminInvitationRepository).save(invitation);
    }


    @Test
    void connect_resent_deleted_client() {
        reset(clientToken);
        String code = "abcdefghhijkl";
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode invitationCode1 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode invitationCode2 = new ClientAdminInvitationCode(code, NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientAdminInvitation invitation = new ClientAdminInvitation(UUID.randomUUID(), clientId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode1, invitationCode2)));

        when(clientAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));
        when(clientRepository.findByClientIdAndDeletedIsFalse(clientId)).thenReturn(Optional.empty());

        ClientNotFoundException ex = assertThrows(ClientNotFoundException.class, () -> clientAdminService.connect(code, portalUserId));
        assertThat(ex).hasMessage("could not find the client with clientId %s".formatted(clientId));
    }

    @Test
    void connect_old_code() {
        reset(clientToken);
        String code = "abcdefghhijkl";
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode invitationCode1 = new ClientAdminInvitationCode(code, NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientAdminInvitationCode invitationCode2 = new ClientAdminInvitationCode("getClientDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientAdminInvitation invitation = new ClientAdminInvitation(invitationId, clientId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode1, invitationCode2)));

        when(clientAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));

        ClientAdminInviteInInvalidStateException ex = assertThrows(ClientAdminInviteInInvalidStateException.class, () -> clientAdminService.connect(code, portalUserId));

        assertThat(ex).hasMessage("Could not link user %s with client %s using invite %s, the supplied code does not belong to the last invite".formatted(portalUserId, clientId, invitationId));
        assertThat(invitationCode1.getUsedAt()).isNull();
        assertThat(invitationCode1.getUsedBy()).isNull();
        assertThat(invitationCode2.getUsedAt()).isNull();
        assertThat(invitationCode2.getUsedBy()).isNull();
        verify(clientAdminInvitationRepository, times(0)).save(invitation);
    }

    @Test
    void connect_expired() {
        reset(clientToken);
        String code = "abcdefghhijkl";
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientAdminInvitationCode invitationCode = new ClientAdminInvitationCode(code, NOW.minusHours(23), NOW.minusHours(1), null, null);
        ClientAdminInvitation invitation = new ClientAdminInvitation(invitationId, clientId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode)));

        when(clientAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));

        ClientAdminInviteInInvalidStateException ex = assertThrows(ClientAdminInviteInInvalidStateException.class, () -> clientAdminService.connect(code, portalUserId));

        assertThat(ex).hasMessage("Could not link user %s with client %s using invite %s, invite in wrong state %s".formatted(portalUserId, clientId, invitationId, ClientAdminDTO.InviteStatus.EXPIRED));
        assertThat(invitationCode.getUsedAt()).isNull();
        assertThat(invitationCode.getUsedBy()).isNull();
        verify(clientAdminInvitationRepository, times(0)).save(invitation);
    }

    @Value
    private static class ClientAdminInvitationWithLastCodeImpl implements ClientAdminInvitationWithLastCode {
        UUID id;
        UUID clientId;
        String email;
        String name;
        LocalDateTime generatedAt;
        LocalDateTime expiresAt;
        LocalDateTime usedAt;
        UUID usedBy;
        int numberOfCodes;

        @Override
        public String getIdText() {
            return id.toString();
        }

        @Override
        public String getClientIdText() {
            return clientId.toString();
        }

        @Override
        public String getUsedByText() {
            return usedBy.toString();
        }
    }
}
