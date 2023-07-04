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
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClientGroupAdminService {
    private final ClientGroupAdminInvitationRepository clientGroupAdminInvitationRepository;
    private final ClientGroupRepository clientGroupRepository;
    private final DevPortalUserService devPortalUserService;
    private final EmailService emailService;
    private final Clock clock;
    private final int expirationHours;

    public ClientGroupAdminService(
            ClientGroupAdminInvitationRepository clientGroupAdminInvitationRepository,
            ClientGroupRepository clientGroupRepository,
            DevPortalUserService devPortalUserService,
            EmailService emailService,
            Clock clock,
            @Value("${yolt.invites.expiration-hours}") int expirationHours) {
        this.clientGroupAdminInvitationRepository = clientGroupAdminInvitationRepository;
        this.clientGroupRepository = clientGroupRepository;
        this.devPortalUserService = devPortalUserService;
        this.emailService = emailService;
        this.clock = clock;
        this.expirationHours = expirationHours;
    }

    public List<ClientGroupAdminDTO> getClientGroupAdmins(ClientGroupToken clientGroupToken) {
        Set<ClientGroupAdminInvitationWithLastCode> latestInvites = clientGroupAdminInvitationRepository.findAllByClientGroupIdWithLastInvite(clientGroupToken.getClientGroupIdClaim());
        Set<PortalUser> clientGroupAdmins = devPortalUserService.getClientGroupAdmins(clientGroupToken);

        Map<UUID, ClientGroupAdminInvitationWithLastCode> id2Invite = latestInvites.stream()
                .collect(Collectors.toMap(ClientGroupAdminInvitationWithLastCode::getId, Function.identity()));

        List<ClientGroupAdminDTO> result = new ArrayList<>();
        Set<UUID> usedInvites = new HashSet<>();

        clientGroupAdmins.forEach(clientAdmin -> {
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

        result.sort(Comparator.comparing(ClientGroupAdminDTO::hasPortalUserId, Comparator.reverseOrder()) // first show all the portal users
                .thenComparing(ClientGroupAdminDTO::portalUserIsKnown, Comparator.reverseOrder()) // have unknown portal users on top
                .thenComparing(ClientGroupAdminDTO::getInviteUsedAt, Comparator.nullsFirst(Comparator.naturalOrder())) // sort on how long that have been admin
                .thenComparing(ClientGroupAdminDTO::getPortalUserName, Comparator.nullsFirst(Comparator.naturalOrder()))// sort on user name
                .thenComparing(ClientGroupAdminDTO::getInviteCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))// when all others are equal (non used invites)
        );

        return result;
    }

    public void inviteClientGroupAdmin(ClientGroupToken clientGroupToken, NewInviteDTO newInviteDTO) {
        UUID clientGroupId = clientGroupToken.getClientGroupIdClaim();
        ClientGroup clientGroup = clientGroupRepository.findById(clientGroupId)
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));

        verifyDomain(clientGroup, newInviteDTO.getEmail());

        if (clientGroupAdminInvitationRepository.existsByClientGroupIdAndEmail(clientGroupId, newInviteDTO.getEmail())) {
            throw new ClientGroupAdminEmailInUseException(clientGroupId, newInviteDTO.getEmail(), "already invited");
        }

        if (devPortalUserService.isAdminForClientGroup(clientGroupToken, newInviteDTO.getEmail())) {
            throw new ClientGroupAdminEmailInUseException(clientGroupId, newInviteDTO.getEmail(), "already an admin");
        }

        ClientGroupAdminInvitation invitation = new ClientGroupAdminInvitation(
                UUID.randomUUID(),
                clientGroupId,
                newInviteDTO.getEmail(),
                newInviteDTO.getName(),
                new HashSet<>()
        );

        sendNewInvitationCode(clientGroup, invitation);
    }

    public void resendClientGroupInvite(ClientGroupToken clientGroupToken, UUID invitationId) {
        UUID clientGroupId = clientGroupToken.getClientGroupIdClaim();
        ClientGroup clientGroup = clientGroupRepository.findById(clientGroupId)
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));
        ClientGroupAdminInvitation invitation = clientGroupAdminInvitationRepository.findByClientGroupIdAndId(clientGroupId, invitationId)
                .orElseThrow(() -> new ClientGroupAdminInvitationNotFoundException(clientGroupId, invitationId));

        if (getStatus(invitation) == ClientGroupAdminDTO.InviteStatus.USED && hasConnectedPortalUser(clientGroupToken, invitation)) {
            throw new ClientGroupAdminInviteUsedException(clientGroupId, invitationId);
        }

        sendNewInvitationCode(clientGroup, invitation);
    }

    public Optional<ConnectionDTO> connect(String code, UUID portalUserId) {
        return clientGroupAdminInvitationRepository.findByCodes_code(code).map(invitation -> {
            ClientGroupAdminDTO.InviteStatus status = getStatus(invitation);
            if (status != ClientGroupAdminDTO.InviteStatus.VALID && status != ClientGroupAdminDTO.InviteStatus.RESENT) {
                throw new ClientGroupAdminInviteInInvalidStateException(invitation, status, portalUserId);
            }
            ClientGroupAdminInvitationCode lastInvite = invitation.getCodes().stream().max(Comparator.comparing(ClientGroupAdminInvitationCode::getGeneratedAt)).orElseThrow();
            if (!lastInvite.getCode().equals(code)) {
                throw new ClientGroupAdminInviteInInvalidStateException(invitation, portalUserId);
            }

            if (devPortalUserService.isAdminForClientGroup(invitation.getClientGroupId(), portalUserId)) {
                throw new PortalUserIsClientGroupAdminException(portalUserId, invitation.getClientGroupId());
            }

            lastInvite.setUsedAt(LocalDateTime.now(clock));
            lastInvite.setUsedBy(portalUserId);
            invitation = clientGroupAdminInvitationRepository.save(invitation);
            return new ConnectionDTO(invitation);
        });
    }

    public void removeAuthorization(ClientGroupToken clientGroupToken, UUID portalUserId) {
        devPortalUserService.removeAuthorization(clientGroupToken, portalUserId);
    }

    private boolean hasConnectedPortalUser(ClientGroupToken clientGroupToken, ClientGroupAdminInvitation invitation) {
        ClientGroupAdminInvitationCode lastInvite = invitation.getCodes().stream().max(Comparator.comparing(ClientGroupAdminInvitationCode::getGeneratedAt)).orElseThrow();
        if (lastInvite.getUsedBy() == null) {
            return false;
        }
        return devPortalUserService.isAdminForClientGroup(clientGroupToken.getClientGroupIdClaim(), lastInvite.getUsedBy());
    }

    private ClientGroupAdminDTO.InviteStatus getStatus(ClientGroupAdminInvitationWithLastCode invite) {
        if (invite.getNumberOfCodes() == 0) {
            return ClientGroupAdminDTO.InviteStatus.EXPIRED;
        }
        if (invite.getUsedAt() != null) {
            return ClientGroupAdminDTO.InviteStatus.USED;
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            return ClientGroupAdminDTO.InviteStatus.EXPIRED;
        }
        if (invite.getNumberOfCodes() > 1) {
            return ClientGroupAdminDTO.InviteStatus.RESENT;
        }
        return ClientGroupAdminDTO.InviteStatus.VALID;
    }

    private ClientGroupAdminDTO.InviteStatus getStatus(ClientGroupAdminInvitation invite) {
        if (invite.getCodes().isEmpty()) {
            return ClientGroupAdminDTO.InviteStatus.EXPIRED;
        }
        ClientGroupAdminInvitationCode lastInvite = invite.getCodes().stream().max(Comparator.comparing(ClientGroupAdminInvitationCode::getGeneratedAt)).orElseThrow();
        if (lastInvite.getUsedAt() != null) {
            return ClientGroupAdminDTO.InviteStatus.USED;
        }
        if (lastInvite.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            return ClientGroupAdminDTO.InviteStatus.EXPIRED;
        }
        if (invite.getCodes().size() > 1) {
            return ClientGroupAdminDTO.InviteStatus.RESENT;
        }
        return ClientGroupAdminDTO.InviteStatus.VALID;
    }

    private void verifyDomain(ClientGroup clientGroup, String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        boolean isDomainAllowed = clientGroup.getEmailDomains().stream().anyMatch(allowedDomain -> domain.equals(allowedDomain.getDomain()));
        if (!isDomainAllowed) {
            throw new InvalidDomainException(domain, clientGroup.getId());
        }
    }

    private void sendNewInvitationCode(ClientGroup clientGroup, ClientGroupAdminInvitation invitation) {
        String activationCode = generateActivationCode();
        LocalDateTime generationTimeStamp = LocalDateTime.now(clock);
        LocalDateTime expirationTimeStamp = generationTimeStamp.plusHours(expirationHours);
        invitation.getCodes().add(new ClientGroupAdminInvitationCode(activationCode, generationTimeStamp, expirationTimeStamp));
        invitation = clientGroupAdminInvitationRepository.save(invitation);
        emailService.sendInvitationForUser(invitation.getEmail(), invitation.getName(), activationCode, clientGroup.getName(), expirationTimeStamp, true);
    }

    private String generateActivationCode() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return new String(Base64.getEncoder().encode(bytes));
    }

    private ClientGroupAdminDTO fromInvitation(ClientGroupAdminInvitationWithLastCode invitation) {
        return new ClientGroupAdminDTO(
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

    private ClientGroupAdminDTO fromPortalUser(PortalUser clientAdmin) {
        return new ClientGroupAdminDTO(
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

    private ClientGroupAdminDTO fromPortalUserAndInvitation(PortalUser clientAdmin, ClientGroupAdminInvitationWithLastCode invitation) {
        return new ClientGroupAdminDTO(
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
