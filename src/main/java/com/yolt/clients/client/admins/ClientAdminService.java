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
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClientAdminService {
    private final ClientAdminInvitationRepository clientAdminInvitationRepository;
    private final ClientGroupRepository clientGroupRepository;
    private final ClientsRepository clientsRepository;
    private final DevPortalUserService devPortalUserService;
    private final EmailService emailService;
    private final Clock clock;
    private final int expirationHours;
    private final int maxInvites;
    private final int resendTimeout;

    public ClientAdminService(
            ClientAdminInvitationRepository clientAdminInvitationRepository,
            ClientGroupRepository clientGroupRepository,
            ClientsRepository clientsRepository,
            DevPortalUserService devPortalUserService,
            EmailService emailService,
            Clock clock,
            @Value("${yolt.invites.expiration-hours}") int expirationHours,
            @Value("${yolt.invites.max-client-invites}") int maxInvites,
            @Value("${yolt.invites.resend-timeout}") int resendTimeout
    ) {
        this.clientAdminInvitationRepository = clientAdminInvitationRepository;
        this.clientGroupRepository = clientGroupRepository;
        this.clientsRepository = clientsRepository;
        this.devPortalUserService = devPortalUserService;
        this.emailService = emailService;
        this.clock = clock;
        this.expirationHours = expirationHours;
        this.maxInvites = maxInvites;
        this.resendTimeout = resendTimeout;
    }

    public List<ClientAdminDTO> getClientAdmins(ClientToken clientToken) {
        Set<ClientAdminInvitationWithLastCode> latestInvites = clientAdminInvitationRepository.findAllByClientIdWithLastInvite(clientToken.getClientIdClaim());
        Set<PortalUser> clientAdmins = devPortalUserService.getClientAdmins(clientToken);

        Map<UUID, ClientAdminInvitationWithLastCode> id2Invite = latestInvites.stream()
                .collect(Collectors.toMap(ClientAdminInvitationWithLastCode::getId, Function.identity()));

        List<ClientAdminDTO> result = new ArrayList<>();
        Set<UUID> usedInvites = new HashSet<>();

        clientAdmins.forEach(clientAdmin -> {
            if (clientAdmin.getInviteId() != null) {
                usedInvites.add(clientAdmin.getInviteId());
                result.add(fromPortalUserAndInvitation(clientAdmin, id2Invite.get(clientAdmin.getInviteId())));
            } else {
                result.add(fromPortalUser(clientAdmin));
            }
        });

        latestInvites.stream()
                .filter(invitation -> !usedInvites.contains(invitation.getId()))
                .forEach(invitation -> result.add(fromInvitation(invitation)));

        result.sort(Comparator.comparing(ClientAdminDTO::hasPortalUserId, Comparator.reverseOrder()) // first show all the portal users
                .thenComparing(ClientAdminDTO::portalUserIsKnown, Comparator.reverseOrder()) // have unknown portal users on top
                .thenComparing(ClientAdminDTO::getInviteUsedAt, Comparator.nullsFirst(Comparator.naturalOrder())) // sort on how long that have been admin
                .thenComparing(ClientAdminDTO::getPortalUserName, Comparator.nullsFirst(Comparator.naturalOrder()))// sort on user name
                .thenComparing(ClientAdminDTO::getInviteCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))// when all others are equal (non used invites)
        );

        return result;
    }

    public void inviteClientAdmin(ClientToken clientToken, NewInviteDTO newInviteDTO) {
        UUID clientGroupId = clientToken.getClientGroupIdClaim();
        UUID clientId = clientToken.getClientIdClaim();
        ClientGroup clientGroup = clientGroupRepository.findById(clientGroupId)
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));
        Client client = clientGroup.getClients().stream().filter(potentialClient -> potentialClient.getClientId().equals(clientId)).findAny()
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        verifyDomain(clientGroup, newInviteDTO.getEmail());

        if (clientAdminInvitationRepository.countClientAdminInvitationByClientId(clientId) >= maxInvites) {
            throw new TooManyClientAdminInvitesException(clientId, maxInvites);
        }

        if (clientAdminInvitationRepository.existsByClientIdAndEmail(clientId, newInviteDTO.getEmail())) {
            throw new ClientAdminEmailInUseException(clientId, newInviteDTO.getEmail(), "already invited");
        }

        if (devPortalUserService.isAdminForClient(clientToken, newInviteDTO.getEmail())) {
            throw new ClientAdminEmailInUseException(clientId, newInviteDTO.getEmail(), "already an admin");
        }

        ClientAdminInvitation invitation = new ClientAdminInvitation(
                UUID.randomUUID(),
                clientId,
                newInviteDTO.getEmail(),
                newInviteDTO.getName(),
                new HashSet<>()
        );

        sendNewInvitationCode(client, invitation);
    }

    public void resendClientInvite(ClientToken clientToken, UUID invitationId) {
        UUID clientId = clientToken.getClientIdClaim();
        Client client = clientsRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        ClientAdminInvitation invitation = clientAdminInvitationRepository.findByClientIdAndId(clientId, invitationId)
                .orElseThrow(() -> new ClientAdminInvitationNotFoundException(clientId, invitationId));

        if (getStatus(invitation) == ClientAdminDTO.InviteStatus.USED && hasConnectedPortalUser(clientToken, invitation)) {
            throw new ClientAdminInviteUsedException(clientId, invitationId);
        }

        if (isTimeoutExceeded(invitation)) {
            sendNewInvitationCode(client, invitation);
        }
    }

    private boolean isTimeoutExceeded(final ClientAdminInvitation invitation) {
        return invitation.getCodes().stream()
                .map(ClientAdminInvitationCode::getGeneratedAt)
                .max(LocalDateTime::compareTo)
                .filter(date -> date.isAfter(LocalDateTime.now(clock).minusMinutes(resendTimeout)))
                .isEmpty();
    }

    public Optional<ConnectionDTO> connect(String code, UUID portalUserId) {
        return clientAdminInvitationRepository.findByCodes_code(code).map(invitation -> {
            ClientAdminDTO.InviteStatus status = getStatus(invitation);
            var clientId = invitation.getClientId();
            if (status != ClientAdminDTO.InviteStatus.VALID && status != ClientAdminDTO.InviteStatus.RESENT) {
                throw new ClientAdminInviteInInvalidStateException(invitation, status, portalUserId);
            }

            ClientAdminInvitationCode lastInvite = invitation.getCodes().stream().max(Comparator.comparing(ClientAdminInvitationCode::getGeneratedAt)).orElseThrow();
            if (!lastInvite.getCode().equals(code)) {
                throw new ClientAdminInviteInInvalidStateException(invitation, portalUserId);
            }

            if (devPortalUserService.isAdminForClient(clientId, portalUserId)) {
                throw new PortalUserIsClientAdminException(portalUserId, clientId);
            }

            clientsRepository.findByClientIdAndDeletedIsFalse(clientId)
                    .orElseThrow(() -> new ClientNotFoundException(clientId));

            lastInvite.setUsedAt(LocalDateTime.now(clock));
            lastInvite.setUsedBy(portalUserId);
            invitation = clientAdminInvitationRepository.save(invitation);
            return new ConnectionDTO(invitation);
        });
    }

    public void removeAuthorization(ClientToken clientToken, UUID portalUserId) {
        devPortalUserService.removeAuthorization(clientToken, portalUserId);
    }

    private boolean hasConnectedPortalUser(ClientToken clientToken, ClientAdminInvitation invitation) {
        ClientAdminInvitationCode lastInvite = invitation.getCodes().stream().max(Comparator.comparing(ClientAdminInvitationCode::getGeneratedAt)).orElseThrow();
        if (lastInvite.getUsedBy() == null) {
            return false;
        }
        return devPortalUserService.isAdminForClient(clientToken.getClientIdClaim(), lastInvite.getUsedBy());
    }

    private ClientAdminDTO.InviteStatus getStatus(ClientAdminInvitationWithLastCode invite) {
        if (invite.getNumberOfCodes() == 0) {
            return ClientAdminDTO.InviteStatus.EXPIRED;
        }
        if (invite.getUsedAt() != null) {
            return ClientAdminDTO.InviteStatus.USED;
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            return ClientAdminDTO.InviteStatus.EXPIRED;
        }
        if (invite.getNumberOfCodes() > 1) {
            return ClientAdminDTO.InviteStatus.RESENT;
        }
        return ClientAdminDTO.InviteStatus.VALID;
    }

    private ClientAdminDTO.InviteStatus getStatus(ClientAdminInvitation invite) {
        if (invite.getCodes().isEmpty()) {
            return ClientAdminDTO.InviteStatus.EXPIRED;
        }
        ClientAdminInvitationCode lastInvite = invite.getCodes().stream().max(Comparator.comparing(ClientAdminInvitationCode::getGeneratedAt)).orElseThrow();
        if (lastInvite.getUsedAt() != null) {
            return ClientAdminDTO.InviteStatus.USED;
        }
        if (lastInvite.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            return ClientAdminDTO.InviteStatus.EXPIRED;
        }
        if (invite.getCodes().size() > 1) {
            return ClientAdminDTO.InviteStatus.RESENT;
        }
        return ClientAdminDTO.InviteStatus.VALID;
    }

    private void verifyDomain(ClientGroup clientGroup, String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        boolean isDomainAllowed = clientGroup.getEmailDomains().stream().anyMatch(allowedDomain -> domain.equals(allowedDomain.getDomain()));
        if (!isDomainAllowed) {
            throw new InvalidDomainException(domain, clientGroup.getId());
        }
    }

    private void sendNewInvitationCode(Client client, ClientAdminInvitation invitation) {
        String activationCode = generateActivationCode();
        LocalDateTime generationTimeStamp = LocalDateTime.now(clock);
        LocalDateTime expirationTimeStamp = generationTimeStamp.plusHours(expirationHours);
        invitation.getCodes().add(new ClientAdminInvitationCode(activationCode, generationTimeStamp, expirationTimeStamp));
        invitation = clientAdminInvitationRepository.save(invitation);
        emailService.sendInvitationForUser(invitation.getEmail(), invitation.getName(), activationCode, client.getName(), expirationTimeStamp, false);
    }

    private String generateActivationCode() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return new String(Base64.getEncoder().encode(bytes));
    }

    private ClientAdminDTO fromInvitation(ClientAdminInvitationWithLastCode invitation) {

        return new ClientAdminDTO(
                null,
                null,
                null,
                null,
                invitation.getId(),
                invitation.getName(),
                invitation.getEmail(),
                invitation.getGeneratedAt(),
                invitation.getUsedAt(),
                invitation.getExpiresAt(),
                getStatus(invitation)
        );
    }

    private ClientAdminDTO fromPortalUser(PortalUser clientAdmin) {
        return new ClientAdminDTO(
                clientAdmin.getId(),
                clientAdmin.getName(),
                clientAdmin.getOrganisation(),
                clientAdmin.getEmail(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private ClientAdminDTO fromPortalUserAndInvitation(PortalUser clientAdmin, ClientAdminInvitationWithLastCode invitation) {
        return new ClientAdminDTO(
                clientAdmin.getId(),
                clientAdmin.getName(),
                clientAdmin.getOrganisation(),
                clientAdmin.getEmail(),
                invitation.getId(),
                invitation.getName(),
                invitation.getEmail(),
                invitation.getGeneratedAt(),
                invitation.getUsedAt(),
                invitation.getExpiresAt(),
                getStatus(invitation)
        );
    }

}
