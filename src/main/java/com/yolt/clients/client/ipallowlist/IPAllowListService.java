package com.yolt.clients.client.ipallowlist;

import com.yolt.clients.client.ipallowlist.dto.AllowedIPDTO;
import com.yolt.clients.client.ipallowlist.dto.AllowedIPIdListDTO;
import com.yolt.clients.client.ipallowlist.dto.NewAllowedIPsDTO;
import com.yolt.clients.client.redirecturls.Action;
import com.yolt.clients.jira.JiraService;
import com.yolt.clients.jira.Status;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IPAllowListService {

    private final IPAllowListRepository ipAllowListRepository;
    private final JiraService jiraService;
    private final Clock clock;
    private final int maxOpenTicketsInLast24Hours;

    public IPAllowListService(IPAllowListRepository ipAllowListRepository,
                              JiraService jiraService,
                              Clock clock,
                              @Value("${yolt.jira.max_open_tickets}") int maxOpenTicketsInLast24Hours) {
        this.ipAllowListRepository = ipAllowListRepository;
        this.jiraService = jiraService;
        this.clock = clock;
        this.maxOpenTicketsInLast24Hours = maxOpenTicketsInLast24Hours;
    }

    public Set<AllowedIPDTO> findAll(UUID clientId) {
        return ipAllowListRepository
                .findAllByClientIdOrderByLastUpdatedDesc(clientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    public boolean hasAddedIPAllowListItems(ClientToken clientToken) {
        return ipAllowListRepository.existsByClientIdAndStatus(clientToken.getClientIdClaim(), Status.ADDED);
    }

    public boolean hasPendingAdditionIPAllowListItems(ClientToken clientToken) {
        return ipAllowListRepository.existsByClientIdAndStatus(clientToken.getClientIdClaim(), Status.PENDING_ADDITION);
    }

    public Set<AllowedIPDTO> create(ClientToken clientToken, NewAllowedIPsDTO newAllowedIPsDTO) {
        final UUID clientId = clientToken.getClientIdClaim();
        rateLimit(clientId);
        final var jiraData = new JiraAllowedIPData(Action.CREATE);

        Set<AllowedIP> result = newAllowedIPsDTO.getCidrs().stream()
                .map(cidr -> findOrCreateAllowedIpItem(clientId, cidr, jiraData))
                .map(allowedIP -> markAsAddedIfRequired(allowedIP, jiraData))
                .collect(Collectors.toSet());

        String jiraTicket = jiraService.createIssue(clientToken, jiraData);

        return result.stream()
                .map(allowedIP -> setJiraTicketIfApplicable(jiraData, jiraTicket, allowedIP))
                .map(ipAllowListRepository::save)
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    public Set<AllowedIPDTO> deleteLimited(ClientToken clientToken, AllowedIPIdListDTO itemsToRemove) {
        var clientId = clientToken.getClientIdClaim();
        rateLimit(clientId);
        var jiraData = new JiraAllowedIPData(Action.DELETE);
        var result = itemsToRemove.getIds().stream()
                .map(id -> getAllowedIP(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(allowedIP -> markAsRemovedIfRequired(jiraData, allowedIP))
                .collect(Collectors.toSet());

        var jiraTicket = jiraService.createIssue(clientToken, jiraData);
        jiraService.updateIssue(jiraData);

        return result.stream()
                .map(allowedIP -> setJiraTicketIfApplicable(jiraData, jiraTicket, allowedIP))
                .map(ipAllowListRepository::save)
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    public void delete(ClientToken clientToken) {
        var clientId = clientToken.getClientIdClaim();
        var jiraData = new JiraAllowedIPData(Action.DELETE);
        var allowedIPs = ipAllowListRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId).stream()
                .map(allowedIP -> markAsRemovedIfRequired(jiraData, allowedIP))
                .collect(Collectors.toSet());

        jiraService.createIssue(clientToken, jiraData);
        jiraService.updateIssue(jiraData);
        ipAllowListRepository.deleteAll(allowedIPs);
    }

    public Set<AllowedIPDTO> markApplied(ClientToken clientToken, AllowedIPIdListDTO itemsToMarkApplied) {
        UUID clientId = clientToken.getClientIdClaim();

        return itemsToMarkApplied.getIds().stream()
                .map(id -> getAllowedIP(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(this::markAsAppliedIfRequired)
                .map(allowedIP -> mapToDTO(ipAllowListRepository.save(allowedIP)))
                .collect(Collectors.toSet());
    }

    private void rateLimit(UUID clientId) {
        LocalDateTime countAfter = LocalDateTime.now(clock).minusDays(1);
        long numberOfJiraTickets = ipAllowListRepository.findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(clientId, countAfter)
                .stream()
                .map(AllowedIP::getJiraTicket)
                .distinct()
                .count();
        if (numberOfJiraTickets >= maxOpenTicketsInLast24Hours) {
            throw new TooManyPendingTasksException(clientId);
        }
    }

    public Set<AllowedIPDTO> markDenied(ClientToken clientToken, AllowedIPIdListDTO itemsToMarkDenied) {
        UUID clientId = clientToken.getClientIdClaim();

        return itemsToMarkDenied.getIds().stream()
                .map(id -> getAllowedIP(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(this::validateInPendingAddition)
                .map(allowedIP -> setStatus(allowedIP, Status.DENIED))
                .map(allowedIP -> mapToDTO(ipAllowListRepository.save(allowedIP)))
                .collect(Collectors.toSet());
    }

    private AllowedIP findOrCreateAllowedIpItem(UUID clientId, String cidr, JiraAllowedIPData jiraData) {
        return ipAllowListRepository.findByClientIdAndCidr(clientId, cidr)
                .orElseGet(() -> {
                    var newAllowedIP = new AllowedIP(UUID.randomUUID(), clientId, cidr, Status.PENDING_ADDITION, LocalDateTime.now(clock), null);
                    jiraData.withItemToBeAdded(newAllowedIP);
                    return newAllowedIP;
                });
    }

    private AllowedIP getAllowedIP(UUID clientId, UUID id) {
        return ipAllowListRepository.findByClientIdAndId(clientId, id)
                .orElseGet(() -> {
                    log.warn("could not fetch the AllowedIP with id {} for client {}", id, clientId); //NOSHERIFF
                    return null;
                });
    }

    private AllowedIP validateInPendingAddition(AllowedIP allowedIP) {
        if (allowedIP.getStatus() != Status.PENDING_ADDITION && allowedIP.getStatus() != Status.DENIED) {
            throw new AllowedIPNotInExpectedStateException(Set.of(Status.PENDING_ADDITION, Status.DENIED), allowedIP.getStatus());
        }
        return allowedIP;
    }

    private AllowedIP markAsAddedIfRequired(AllowedIP allowedIP, JiraAllowedIPData jiraData) {
        switch (allowedIP.getStatus()) {
            case PENDING_REMOVAL -> {
                jiraData.withJiraTicketToBeEdited(allowedIP.getJiraTicket(), allowedIP);
                setStatus(allowedIP, Status.ADDED);
            }
            case REMOVED -> {
                jiraData.withItemToBeAdded(allowedIP);
                setStatus(allowedIP, Status.PENDING_ADDITION);
            }
            default -> log.info("Will not add {} since its already in status {}", allowedIP.getCidr(), allowedIP.getStatus()); //NOSHERIFF
        }
        return allowedIP;
    }

    private AllowedIP markAsRemovedIfRequired(JiraAllowedIPData jiraData, AllowedIP allowedIP) {
        switch (allowedIP.getStatus()) {
            case PENDING_ADDITION -> {
                jiraData.withJiraTicketToBeEdited(allowedIP.getJiraTicket(), allowedIP);
                setStatus(allowedIP, Status.REMOVED);
            }
            case ADDED -> {
                jiraData.withItemToBeRemoved(allowedIP);
                setStatus(allowedIP, Status.PENDING_REMOVAL);
            }
            default -> log.info("Will not delete {} since its already in status {}", allowedIP.getCidr(), allowedIP.getStatus()); //NOSHERIFF
        }
        return allowedIP;
    }

    private AllowedIP markAsAppliedIfRequired(AllowedIP allowedIP) {
        switch (allowedIP.getStatus()) {
            case PENDING_ADDITION -> setStatus(allowedIP, Status.ADDED);
            case PENDING_REMOVAL -> setStatus(allowedIP, Status.REMOVED);
            default -> log.info("Will not mark {} as applied since its already in status {}", allowedIP.getCidr(), allowedIP.getStatus()); //NOSHERIFF
        }
        return allowedIP;
    }

    private AllowedIP setStatus(AllowedIP allowedIP, Status newStatus) {
        allowedIP.setJiraTicket(null);
        allowedIP.setStatus(newStatus);
        allowedIP.setLastUpdated(LocalDateTime.now(clock));
        return allowedIP;
    }

    private AllowedIP setJiraTicketIfApplicable(JiraAllowedIPData jiraAllowedIPData, String jiraTicket, AllowedIP allowedIP) {
        if (jiraAllowedIPData.contains(allowedIP)) {
            allowedIP.setJiraTicket(jiraTicket);
        }
        return allowedIP;
    }

    private AllowedIPDTO mapToDTO(AllowedIP allowedIP) {
        return new AllowedIPDTO(allowedIP.getId(), allowedIP.getCidr(), allowedIP.getLastUpdated(), allowedIP.getStatus(), allowedIP.getJiraTicket());
    }
}
