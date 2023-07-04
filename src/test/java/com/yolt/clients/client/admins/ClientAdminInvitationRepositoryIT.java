package com.yolt.clients.client.admins;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.client.admins.models.ClientAdminInvitationCode;
import com.yolt.clients.client.admins.models.ClientAdminInvitationWithLastCode;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ClientAdminInvitationRepositoryIT {
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientsRepository clientsRepository;
    @Autowired
    private ClientAdminInvitationRepository clientAdminInvitationRepository;

    private UUID clientGroupId;
    private UUID clientId;
    private ClientGroup clientGroup;
    private Client client;

    @BeforeEach
    void setUp() {
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
                Collections.emptySet()
        );
    }

    @Test
    void findAllByClientIdWithLastInvite() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        UUID inviteId1 = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        clientAdminInvitationRepository.save(new ClientAdminInvitation(inviteId1, clientId, "email1@provider.com", "name", Set.of(
                new ClientAdminInvitationCode("code-1-1", NOW.minusHours(1), NOW.minusHours(1).plusDays(1)),
                new ClientAdminInvitationCode("code-1-2", NOW.minusHours(2), NOW.minusHours(2).plusDays(1)),
                new ClientAdminInvitationCode("code-1-3", NOW.minusHours(3), NOW.minusHours(3).plusDays(1)),
                new ClientAdminInvitationCode("code-1-4", NOW.minusHours(4), NOW.minusHours(4).plusDays(1), NOW, userId1)
        )));

        UUID inviteId2 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        clientAdminInvitationRepository.save(new ClientAdminInvitation(inviteId2, clientId, "email2@provider.com", "name", Set.of(
                new ClientAdminInvitationCode("code-2-1", NOW.minusHours(1), NOW.minusHours(1).plusDays(1), NOW, userId2),
                new ClientAdminInvitationCode("code-2-2", NOW.minusHours(2), NOW.minusHours(2).plusDays(1)),
                new ClientAdminInvitationCode("code-2-3", NOW.minusHours(3), NOW.minusHours(3).plusDays(1)),
                new ClientAdminInvitationCode("code-2-4", NOW.minusHours(4), NOW.minusHours(4).plusDays(1))
        )));

        UUID inviteId3 = UUID.randomUUID();
        clientAdminInvitationRepository.save(new ClientAdminInvitation(inviteId3, clientId, "email3@provider.com", "name", Set.of()));

        UUID inviteId4 = UUID.randomUUID();
        clientAdminInvitationRepository.save(new ClientAdminInvitation(inviteId4, clientId, "email4@provider.com", "name", Set.of(
                new ClientAdminInvitationCode("code-4-1", NOW.minusHours(1), NOW.plusDays(23))
        )));

        Set<ClientAdminInvitationWithLastCode> result = clientAdminInvitationRepository.findAllByClientIdWithLastInvite(clientId);

        assertThat(result).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new dbItem(inviteId1.toString(), clientId.toString(), "email1@provider.com", "name", NOW.minusHours(1), NOW.minusHours(1).plusDays(1), null, null, 4),
                new dbItem(inviteId2.toString(), clientId.toString(), "email2@provider.com", "name", NOW.minusHours(1), NOW.minusHours(1).plusDays(1), NOW, userId2.toString(), 4),
                new dbItem(inviteId3.toString(), clientId.toString(), "email3@provider.com", "name", null, null, null, null, 0),
                new dbItem(inviteId4.toString(), clientId.toString(), "email4@provider.com", "name", NOW.minusHours(1), NOW.plusDays(23), null, null, 1)
        );
    }

    @Value
    private static class dbItem implements ClientAdminInvitationWithLastCode {
        String idText;
        String clientIdText;
        String email;
        String name;
        LocalDateTime generatedAt;
        LocalDateTime expiresAt;
        LocalDateTime usedAt;
        String usedByText;
        int numberOfCodes;
    }
}
