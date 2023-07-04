package com.yolt.clients.clientgroup.admins;

import com.yolt.clients.admins.dto.ConnectionDTO;
import com.yolt.clients.admins.dto.NewInviteDTO;
import com.yolt.clients.admins.portalusersservice.PortalUser;
import com.yolt.clients.admins.portalusersservice.DevPortalUserService;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminDTO;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationCode;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationWithLastCode;
import com.yolt.clients.email.EmailService;
import com.yolt.clients.exceptions.ClientGroupNotFoundException;
import com.yolt.clients.exceptions.InvalidDomainException;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.model.EmailDomain;
import lombok.Value;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientGroupAdminServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    private ClientGroupAdminService clientGroupAdminService;

    @Mock
    private ClientGroupAdminInvitationRepository clientGroupAdminInvitationRepository;
    @Mock
    private ClientGroupRepository clientGroupRepository;
    @Mock
    private DevPortalUserService devPortalUserService;
    @Mock
    private EmailService emailService;
    private int expirationHours = 24;

    @Captor
    private ArgumentCaptor<ClientGroupAdminInvitation> clientGroupAdminInvitationArgumentCaptor;

    @Mock
    private ClientGroupToken clientGroupToken;
    private UUID clientGroupId;
    private ClientGroup clientGroup;

    @BeforeEach
    void setUp() {
        expirationHours = 24;
        clientGroupAdminService = new ClientGroupAdminService(
                clientGroupAdminInvitationRepository,
                clientGroupRepository,
                devPortalUserService,
                emailService,
                FIXED_CLOCK,
                expirationHours
        );
        clientGroupId = UUID.randomUUID();
        when(clientGroupToken.getClientGroupIdClaim()).thenReturn(clientGroupId);
        clientGroup = new ClientGroup(clientGroupId, "client group", Set.of(), Set.of(new EmailDomain(clientGroupId, "provider.com")), Collections.emptySet());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getClientGroupAdmins() {
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

        Set<ClientGroupAdminInvitationWithLastCode> latestInvites = Set.of(
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId0, clientGroupId, "i0@ma.il", "invite 0", null, null, null, null, 0),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId1, clientGroupId, "i1@ma.il", "invite 1", NOW.minusHours(12), NOW.plusHours(12), null, null, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId2, clientGroupId, "i2@ma.il", "invite 2", NOW.minusHours(13), NOW.plusHours(11), null, null, 2),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId3, clientGroupId, "i3@ma.il", "invite 3", NOW.minusHours(24), NOW, null, null, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId4, clientGroupId, "i4@ma.il", "invite 4", NOW.minusHours(25), NOW.minusHours(1), null, null, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId5, clientGroupId, "i5@ma.il", "invite 5", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(1), null, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId6, clientGroupId, "i6@ma.il", "invite 6", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(2), null, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId7, clientGroupId, "i7@ma.il", "invite 7", NOW.minusHours(26), NOW.minusHours(2), NOW.minusHours(1), portalUserId1, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId8, clientGroupId, "i8@ma.il", "invite 8", NOW.minusHours(27), NOW.minusHours(3), NOW.minusHours(2), portalUserId2, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId9, clientGroupId, "i9@ma.il", "invite 9", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(3), portalUserId3, 1),
                new ClientGroupAdminInvitationWithLastCodeImpl(inviteId10, clientGroupId, "i10@ma.il", "invite 10", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(4), portalUserId4, 1)
        );

        Set<PortalUser> clientGroupAdmins = Set.of(
                new PortalUser(portalUserId3, "portalUser3", "p3@ma.il", "org3", inviteId9),
                new PortalUser(portalUserId4, "portalUser4", "p4@ma.il", "org4", inviteId10),
                new PortalUser(portalUserId5, "portalUser5", "p5@ma.il", "org5", null),
                new PortalUser(portalUserId6, "portalUser6", "p6@ma.il", "org6", null)
        );

        when(clientGroupAdminInvitationRepository.findAllByClientGroupIdWithLastInvite(clientGroupId)).thenReturn(latestInvites);
        when(devPortalUserService.getClientGroupAdmins(clientGroupToken)).thenReturn(clientGroupAdmins);

        List<ClientGroupAdminDTO> result = clientGroupAdminService.getClientGroupAdmins(clientGroupToken);

        assertThat(result).containsExactly(
                // portal users without invite, sorted on name
                new ClientGroupAdminDTO(portalUserId5, "portalUser5", "org5", "p5@ma.il", null, null, null, null, null, null, null),
                new ClientGroupAdminDTO(portalUserId6, "portalUser6", "org6", "p6@ma.il", null, null, null, null, null, null, null),

                // portal users with invite, longest user first
                new ClientGroupAdminDTO(portalUserId4, "portalUser4", "org4", "p4@ma.il", inviteId10, "invite 10", "i10@ma.il", NOW.minusHours(25), NOW.minusHours(4), NOW.minusHours(1), ClientGroupAdminDTO.InviteStatus.USED),
                new ClientGroupAdminDTO(portalUserId3, "portalUser3", "org3", "p3@ma.il", inviteId9, "invite 9", "i9@ma.il", NOW.minusHours(25), NOW.minusHours(3), NOW.minusHours(1), ClientGroupAdminDTO.InviteStatus.USED),

                // invite without any actual codes
                new ClientGroupAdminDTO(null, null, null, null, inviteId0, "invite 0", "i0@ma.il", null, null, null, ClientGroupAdminDTO.InviteStatus.EXPIRED),

                // Non used invites, oldest first
                new ClientGroupAdminDTO(null, null, null, null, inviteId4, "invite 4", "i4@ma.il", NOW.minusHours(25), null, NOW.minusHours(1), ClientGroupAdminDTO.InviteStatus.EXPIRED),
                new ClientGroupAdminDTO(null, null, null, null, inviteId3, "invite 3", "i3@ma.il", NOW.minusHours(24), null, NOW, ClientGroupAdminDTO.InviteStatus.VALID),
                new ClientGroupAdminDTO(null, null, null, null, inviteId2, "invite 2", "i2@ma.il", NOW.minusHours(13), null, NOW.plusHours(11), ClientGroupAdminDTO.InviteStatus.RESENT),
                new ClientGroupAdminDTO(null, null, null, null, inviteId1, "invite 1", "i1@ma.il", NOW.minusHours(12), null, NOW.plusHours(12), ClientGroupAdminDTO.InviteStatus.VALID),

                // used invites, without a portal user longest used first
                new ClientGroupAdminDTO(null, null, null, null, inviteId8, "invite 8", "i8@ma.il", NOW.minusHours(27), NOW.minusHours(2), NOW.minusHours(3), ClientGroupAdminDTO.InviteStatus.USED),
                new ClientGroupAdminDTO(null, null, null, null, inviteId6, "invite 6", "i6@ma.il", NOW.minusHours(25), NOW.minusHours(2), NOW.minusHours(1), ClientGroupAdminDTO.InviteStatus.USED),
                new ClientGroupAdminDTO(null, null, null, null, inviteId7, "invite 7", "i7@ma.il", NOW.minusHours(26), NOW.minusHours(1), NOW.minusHours(2), ClientGroupAdminDTO.InviteStatus.USED),
                new ClientGroupAdminDTO(null, null, null, null, inviteId5, "invite 5", "i5@ma.il", NOW.minusHours(25), NOW.minusHours(1), NOW.minusHours(1), ClientGroupAdminDTO.InviteStatus.USED)
        );
    }

    @Test
    void inviteClientGroupAdmin() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(devPortalUserService.isAdminForClientGroup(clientGroupToken, mail)).thenReturn(false);
        when(clientGroupAdminInvitationRepository.existsByClientGroupIdAndEmail(clientGroupId, mail)).thenReturn(false);
        doAnswer(returnsFirstArg()).when(clientGroupAdminInvitationRepository).save(any());

        clientGroupAdminService.inviteClientGroupAdmin(clientGroupToken, newInvite);

        verify(clientGroupAdminInvitationRepository).save(clientGroupAdminInvitationArgumentCaptor.capture());
        ClientGroupAdminInvitation invitation = clientGroupAdminInvitationArgumentCaptor.getValue();
        assertThat(invitation.getCodes()).hasSize(1);
        assertThat(invitation.getEmail()).isEqualTo(mail);
        assertThat(invitation.getName()).isEqualTo(name);

        ClientGroupAdminInvitationCode invitationCode = invitation.getCodes().stream().findAny().get();
        assertThat(invitationCode.getCode()).isNotBlank();
        assertThat(invitationCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(invitationCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(invitationCode.getUsedAt()).isNull();
        assertThat(invitationCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser(mail, name, invitationCode.getCode(), "client group", NOW.plusHours(expirationHours), true);
    }

    @Test
    void inviteClientGroupAdmin_invalid_domain() {
        String mail = "mail@invalid.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));

        InvalidDomainException ex = assertThrows(InvalidDomainException.class, () -> clientGroupAdminService.inviteClientGroupAdmin(clientGroupToken, newInvite));
        assertThat(ex).hasMessage("The domain invalid.com is not in the allowed list of domains for client group id " + clientGroupId);
    }

    @Test
    void inviteClientGroupAdmin_email_known_for_invite() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.existsByClientGroupIdAndEmail(clientGroupId, mail)).thenReturn(true);

        ClientGroupAdminEmailInUseException ex = assertThrows(ClientGroupAdminEmailInUseException.class, () -> clientGroupAdminService.inviteClientGroupAdmin(clientGroupToken, newInvite));
        assertThat(ex).hasMessage("could not invite %s for client group %s, reason: already invited".formatted(mail, clientGroupId));
    }

    @Test
    void inviteClientGroupAdmin_email_known_as_admin() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.existsByClientGroupIdAndEmail(clientGroupId, mail)).thenReturn(false);
        when(devPortalUserService.isAdminForClientGroup(clientGroupToken, mail)).thenReturn(true);

        ClientGroupAdminEmailInUseException ex = assertThrows(ClientGroupAdminEmailInUseException.class, () -> clientGroupAdminService.inviteClientGroupAdmin(clientGroupToken, newInvite));
        assertThat(ex).hasMessage("could not invite %s for client group %s, reason: already an admin".formatted(mail, clientGroupId));
    }

    @Test
    void inviteClientGroupAdmin_client_group_not_found() {
        String mail = "mail@provider.com";
        String name = "klaas Vaak";
        NewInviteDTO newInvite = new NewInviteDTO(mail, name);

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.empty());

        ClientGroupNotFoundException ex = assertThrows(ClientGroupNotFoundException.class, () -> clientGroupAdminService.inviteClientGroupAdmin(clientGroupToken, newInvite));
        assertThat(ex).hasMessage("No client group found for client group id %s".formatted(clientGroupId));
    }

    @Test
    void resendClientGroupInvite_no_existing_codes() {
        UUID invitationId = UUID.randomUUID();
        ClientGroupAdminInvitation existingInvitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>());

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientGroupAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId);

        ClientGroupAdminInvitationCode newCode = new ArrayList<>(existingInvitation.getCodes()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client group", NOW.plusHours(expirationHours), true);
    }

    @Test
    void resendClientGroupInvite_expired() {
        UUID invitationId = UUID.randomUUID();
        ClientGroupAdminInvitationCode oldCode = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(25), NOW.minusHours(1), null, null);
        ClientGroupAdminInvitation existingInvitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(oldCode)));

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientGroupAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId);

        ClientGroupAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client group", NOW.plusHours(expirationHours), true);
    }

    @Test
    void resendClientGroupInvite_valid() {
        UUID invitationId = UUID.randomUUID();
        ClientGroupAdminInvitationCode oldCode = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientGroupAdminInvitation existingInvitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(oldCode)));

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientGroupAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId);

        ClientGroupAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client group", NOW.plusHours(expirationHours), true);
    }

    @Test
    void resendClientGroupInvite_resent() {
        UUID invitationId = UUID.randomUUID();
        ClientGroupAdminInvitationCode oldCode1 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientGroupAdminInvitationCode oldCode2 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientGroupAdminInvitation existingInvitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientGroupAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId);

        ClientGroupAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode1) && !code.equals(oldCode2)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client group", NOW.plusHours(expirationHours), true);
    }

    @Test
    void resendClientGroupInvite_used_no_portal_user_id() {
        UUID invitationId = UUID.randomUUID();
        ClientGroupAdminInvitationCode oldCode1 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientGroupAdminInvitationCode oldCode2 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), NOW, null);
        ClientGroupAdminInvitation existingInvitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(clientGroupAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId);

        ClientGroupAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode1) && !code.equals(oldCode2)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client group", NOW.plusHours(expirationHours), true);
    }

    @Test
    void resendClientGroupInvite_used_portal_user_not_linked_anymore() {
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientGroupAdminInvitationCode oldCode1 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientGroupAdminInvitationCode oldCode2 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), NOW, portalUserId);
        ClientGroupAdminInvitation existingInvitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(devPortalUserService.isAdminForClientGroup(clientGroupId, portalUserId)).thenReturn(false);
        when(clientGroupAdminInvitationRepository.save(existingInvitation)).thenReturn(existingInvitation);

        clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId);

        ClientGroupAdminInvitationCode newCode = existingInvitation.getCodes().stream().filter(code -> !code.equals(oldCode1) && !code.equals(oldCode2)).collect(Collectors.toList()).get(0);
        assertThat(newCode.getCode()).isNotBlank();
        assertThat(newCode.getGeneratedAt()).isEqualTo(NOW);
        assertThat(newCode.getExpiresAt()).isEqualTo(NOW.plusHours(expirationHours));
        assertThat(newCode.getUsedAt()).isNull();
        assertThat(newCode.getUsedBy()).isNull();

        verify(emailService).sendInvitationForUser("test@test.com", "test", newCode.getCode(), "client group", NOW.plusHours(expirationHours), true);
    }

    @Test
    void resendClientGroupInvite_used_portal_user_still_linked() {
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientGroupAdminInvitationCode oldCode1 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientGroupAdminInvitationCode oldCode2 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), NOW, portalUserId);
        ClientGroupAdminInvitation existingInvitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(oldCode1, oldCode2)));

        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.of(existingInvitation));
        when(devPortalUserService.isAdminForClientGroup(clientGroupId, portalUserId)).thenReturn(true);

        ClientGroupAdminInviteUsedException ex = assertThrows(ClientGroupAdminInviteUsedException.class, () -> clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId));
        assertThat(ex).hasMessage("the client admin invite %s for client group %s is used and the portal user is still linked".formatted(invitationId, clientGroupId));
    }

    @Test
    void resendClientGroupInvite_invite_does_not_exists() {
        UUID invitationId = UUID.randomUUID();
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.of(clientGroup));
        when(clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)).thenReturn(Optional.empty());
        ClientGroupAdminInvitationNotFoundException ex = assertThrows(ClientGroupAdminInvitationNotFoundException.class, () -> clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId));
        assertThat(ex).hasMessage("Could not find invitation %s for client group %s".formatted(invitationId, clientGroupId));
    }

    @Test
    void resendClientGroupInvite_client_group_does_not_exists() {
        UUID invitationId = UUID.randomUUID();
        when(clientGroupRepository.findById(clientGroupId)).thenReturn(Optional.empty());
        ClientGroupNotFoundException ex = assertThrows(ClientGroupNotFoundException.class, () -> clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId));
        assertThat(ex).hasMessage("No client group found for client group id %s".formatted(clientGroupId));
    }

    @Test
    void connect_valid() {
        reset(clientGroupToken);
        String code = "abcdefghhijkl";
        UUID portalUserId = UUID.randomUUID();
        ClientGroupAdminInvitationCode invitationCode = new ClientGroupAdminInvitationCode(code, NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientGroupAdminInvitation invitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode)));

        when(clientGroupAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));
        when(clientGroupAdminInvitationRepository.save(invitation)).thenReturn(invitation);

        Optional<ConnectionDTO> result = clientGroupAdminService.connect(code, portalUserId);

        assertThat(result).contains(new ConnectionDTO(invitation.getId(), clientGroupId, null));
        assertThat(invitationCode.getUsedAt()).isEqualTo(NOW);
        assertThat(invitationCode.getUsedBy()).isEqualTo(portalUserId);
        verify(clientGroupAdminInvitationRepository).save(invitation);
    }

    @Test
    void connect_resent() {
        reset(clientGroupToken);
        String code = "abcdefghhijkl";
        UUID portalUserId = UUID.randomUUID();
        ClientGroupAdminInvitationCode invitationCode1 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientGroupAdminInvitationCode invitationCode2 = new ClientGroupAdminInvitationCode(code, NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientGroupAdminInvitation invitation = new ClientGroupAdminInvitation(UUID.randomUUID(), clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode1, invitationCode2)));

        when(clientGroupAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));
        when(clientGroupAdminInvitationRepository.save(invitation)).thenReturn(invitation);

        Optional<ConnectionDTO> result = clientGroupAdminService.connect(code, portalUserId);

        assertThat(result).contains(new ConnectionDTO(invitation.getId(), clientGroupId, null));
        assertThat(invitationCode1.getUsedAt()).isNull();
        assertThat(invitationCode1.getUsedBy()).isNull();
        assertThat(invitationCode2.getUsedAt()).isEqualTo(NOW);
        assertThat(invitationCode2.getUsedBy()).isEqualTo(portalUserId);
        verify(clientGroupAdminInvitationRepository).save(invitation);
    }

    @Test
    void connect_old_code() {
        reset(clientGroupToken);
        String code = "abcdefghhijkl";
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientGroupAdminInvitationCode invitationCode1 = new ClientGroupAdminInvitationCode(code, NOW.minusHours(23), NOW.plusHours(1), null, null);
        ClientGroupAdminInvitationCode invitationCode2 = new ClientGroupAdminInvitationCode("getClientGroupDetailsInviteCode", NOW.minusHours(22), NOW.plusHours(2), null, null);
        ClientGroupAdminInvitation invitation = new ClientGroupAdminInvitation(invitationId, clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode1, invitationCode2)));

        when(clientGroupAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));

        ClientGroupAdminInviteInInvalidStateException ex = assertThrows(ClientGroupAdminInviteInInvalidStateException.class, () -> clientGroupAdminService.connect(code, portalUserId));

        assertThat(ex).hasMessage("Could not link user %s with client group %s using invite %s, the supplied code does not belong to the last invite".formatted(portalUserId, clientGroupId, invitationId));
        assertThat(invitationCode1.getUsedAt()).isNull();
        assertThat(invitationCode1.getUsedBy()).isNull();
        assertThat(invitationCode2.getUsedAt()).isNull();
        assertThat(invitationCode2.getUsedBy()).isNull();
        verify(clientGroupAdminInvitationRepository, times(0)).save(invitation);
    }

    @Test
    void connect_expired() {
        reset(clientGroupToken);
        String code = "abcdefghhijkl";
        UUID invitationId = UUID.randomUUID();
        UUID portalUserId = UUID.randomUUID();
        ClientGroupAdminInvitationCode invitationCode = new ClientGroupAdminInvitationCode(code, NOW.minusHours(23), NOW.minusHours(1), null, null);
        ClientGroupAdminInvitation invitation = new ClientGroupAdminInvitation(invitationId, clientGroupId, "test@test.com", "test", new HashSet<>(Set.of(invitationCode)));

        when(clientGroupAdminInvitationRepository.findByCodes_code(code)).thenReturn(Optional.of(invitation));

        ClientGroupAdminInviteInInvalidStateException ex = assertThrows(ClientGroupAdminInviteInInvalidStateException.class, () -> clientGroupAdminService.connect(code, portalUserId));

        assertThat(ex).hasMessage("Could not link user %s with client group %s using invite %s, invite in wrong state %s".formatted(portalUserId, clientGroupId, invitationId, ClientGroupAdminDTO.InviteStatus.EXPIRED));
        assertThat(invitationCode.getUsedAt()).isNull();
        assertThat(invitationCode.getUsedBy()).isNull();
        verify(clientGroupAdminInvitationRepository, times(0)).save(invitation);
    }

    @Value
    private static class ClientGroupAdminInvitationWithLastCodeImpl implements ClientGroupAdminInvitationWithLastCode {
        UUID id;
        UUID clientGroupId;
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
        public String getClientGroupIdText() {
            return clientGroupId.toString();
        }

        @Override
        public String getUsedByText() {
            return usedBy.toString();
        }
    }
}