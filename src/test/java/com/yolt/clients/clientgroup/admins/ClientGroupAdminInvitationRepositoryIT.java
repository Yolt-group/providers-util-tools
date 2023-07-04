package com.yolt.clients.clientgroup.admins;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationCode;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationWithLastCode;
import com.yolt.clients.model.ClientGroup;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ClientGroupAdminInvitationRepositoryIT {
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientGroupAdminInvitationRepository clientGroupAdminInvitationRepository;

    private UUID clientGroupId;
    private ClientGroup clientGroup;

    @BeforeEach
    void setUp() {
        clientGroupId = UUID.randomUUID();
        clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
    }

    @Test
    void findAllByClientGroupIdWithLastInvite() {
        clientGroupRepository.save(clientGroup);
        UUID inviteId1 = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        clientGroupAdminInvitationRepository.save(new ClientGroupAdminInvitation(inviteId1, clientGroupId, "email1@provider.com", "name", Set.of(
                new ClientGroupAdminInvitationCode("code-1-2", NOW.minusHours(2), NOW.minusHours(2).plusDays(1)),
                new ClientGroupAdminInvitationCode("code-1-1", NOW.minusHours(1), NOW.minusHours(1).plusDays(1)),
                new ClientGroupAdminInvitationCode("code-1-3", NOW.minusHours(3), NOW.minusHours(3).plusDays(1)),
                new ClientGroupAdminInvitationCode("code-1-4", NOW.minusHours(4), NOW.minusHours(4).plusDays(1), NOW, userId1)
        )));

        UUID inviteId2 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        clientGroupAdminInvitationRepository.save(new ClientGroupAdminInvitation(inviteId2, clientGroupId, "email2@provider.com", "name", Set.of(
                new ClientGroupAdminInvitationCode("code-2-3", NOW.minusHours(3), NOW.minusHours(3).plusDays(1)),
                new ClientGroupAdminInvitationCode("code-2-1", NOW.minusHours(1), NOW.minusHours(1).plusDays(1), NOW, userId2),
                new ClientGroupAdminInvitationCode("code-2-2", NOW.minusHours(2), NOW.minusHours(2).plusDays(1)),
                new ClientGroupAdminInvitationCode("code-2-4", NOW.minusHours(4), NOW.minusHours(4).plusDays(1))
        )));

        UUID inviteId3 = UUID.randomUUID();
        clientGroupAdminInvitationRepository.save(new ClientGroupAdminInvitation(inviteId3, clientGroupId, "email3@provider.com", "name", Set.of()));

        UUID inviteId4 = UUID.randomUUID();
        clientGroupAdminInvitationRepository.save(new ClientGroupAdminInvitation(inviteId4, clientGroupId, "email4@provider.com", "name", Set.of(
                new ClientGroupAdminInvitationCode("code-4-1", NOW.minusHours(1), NOW.plusDays(23))
        )));

        Set<ClientGroupAdminInvitationWithLastCode> result = clientGroupAdminInvitationRepository.findAllByClientGroupIdWithLastInvite(clientGroupId);

        assertThat(result).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new dbItem(inviteId1.toString(), clientGroupId.toString(), "email1@provider.com", "name", NOW.minusHours(1), NOW.minusHours(1).plusDays(1), null, null, 4),
                new dbItem(inviteId2.toString(), clientGroupId.toString(), "email2@provider.com", "name", NOW.minusHours(1), NOW.minusHours(1).plusDays(1), NOW, userId2.toString(), 4),
                new dbItem(inviteId3.toString(), clientGroupId.toString(), "email3@provider.com", "name", null, null, null, null, 0),
                new dbItem(inviteId4.toString(), clientGroupId.toString(), "email4@provider.com", "name", NOW.minusHours(1), NOW.plusDays(23), null, null, 1)
        );
    }

    @Value
    private static class dbItem implements ClientGroupAdminInvitationWithLastCode {
        String idText;
        String clientGroupIdText;
        String email;
        String name;
        LocalDateTime generatedAt;
        LocalDateTime expiresAt;
        LocalDateTime usedAt;
        String usedByText;
        int numberOfCodes;
    }
}