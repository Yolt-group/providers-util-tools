package com.yolt.clients.client.outboundallowlist;

import com.yolt.clients.client.ipallowlist.TooManyPendingTasksException;
import com.yolt.clients.client.outboundallowlist.dto.AllowedOutboundHostDTO;
import com.yolt.clients.client.outboundallowlist.dto.AllowedOutboundHostIdListDTO;
import com.yolt.clients.client.outboundallowlist.dto.NewAllowedOutboundHostsDTO;
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
public class OutboundAllowListService {

    private final AllowedOutboundHostRepository allowedOutboundHostRepository;
    private final JiraService jiraService;
    private final Clock clock;
    private final int maxOpenTicketsInLast24Hours;

    public OutboundAllowListService(AllowedOutboundHostRepository allowedOutboundHostRepository,
                                    JiraService jiraService,
                                    Clock clock,
                                    @Value("${yolt.jira.max_open_tickets}") int maxOpenTicketsInLast24Hours) {
        this.allowedOutboundHostRepository = allowedOutboundHostRepository;
        this.jiraService = jiraService;
        this.clock = clock;
        this.maxOpenTicketsInLast24Hours = maxOpenTicketsInLast24Hours;
    }

    public boolean hasAllowedOutboundHost(UUID clientId, String host) {
        return allowedOutboundHostRepository.existsByClientIdAndHostAndStatus(clientId, host, Status.ADDED);
    }

    public Set<AllowedOutboundHostDTO> findAll(UUID clientId) {
        return allowedOutboundHostRepository
                .findAllByClientIdOrderByLastUpdatedDesc(clientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    public Set<AllowedOutboundHostDTO> create(ClientToken clientToken, NewAllowedOutboundHostsDTO newAllowedOutboundHostsDTO) {
        final UUID clientId = clientToken.getClientIdClaim();
        rateLimit(clientToken.getClientIdClaim());
        final var jiraData = new JiraAllowedOutboundHostData();

        Set<AllowedOutboundHost> result = newAllowedOutboundHostsDTO.getHosts().stream()
                .map(host -> findOrCreateAllowedIpItem(clientId, host, jiraData))
                .map(allowedHost -> markAsAddedIfRequired(allowedHost, jiraData))
                .collect(Collectors.toSet());

        String jiraTicket = jiraService.createIssue(clientToken, jiraData);

        return result.stream()
                .map(allowedOutboundHost -> setJiraTicketIfApplicable(jiraData, jiraTicket, allowedOutboundHost))
                .map(allowedOutboundHostRepository::save)
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    public Set<AllowedOutboundHostDTO> deleteLimited(ClientToken clientToken, AllowedOutboundHostIdListDTO itemsToRemove) {
        var clientId = clientToken.getClientIdClaim();
        rateLimit(clientId);
        var jiraData = new JiraAllowedOutboundHostData();
        var result = itemsToRemove.getIds().stream()
                .map(id -> getAllowedIP(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(allowedOutboundHost -> markAsRemovedIfRequired(jiraData, allowedOutboundHost))
                .collect(Collectors.toSet());

        var jiraTicket = jiraService.createIssue(clientToken, jiraData);
        jiraService.updateIssue(jiraData);

        return result.stream()
                .map(allowedOutboundHost -> setJiraTicketIfApplicable(jiraData, jiraTicket, allowedOutboundHost))
                .map(allowedOutboundHostRepository::save)
                .map(this::mapToDTO)
                .collect(Collectors.toSet());
    }

    public void delete(ClientToken clientToken) {
        var clientId = clientToken.getClientIdClaim();
        var jiraData = new JiraAllowedOutboundHostData();
        var allowedHosts = allowedOutboundHostRepository.findAllByClientIdOrderByLastUpdatedDesc(clientId).stream()
                .map(allowedOutboundHost -> markAsRemovedIfRequired(jiraData, allowedOutboundHost))
                .collect(Collectors.toSet());

        jiraService.createIssue(clientToken, jiraData);
        jiraService.updateIssue(jiraData);
        allowedOutboundHostRepository.deleteAll(allowedHosts);
    }

    public Set<AllowedOutboundHostDTO> markApplied(ClientToken clientToken, AllowedOutboundHostIdListDTO itemsToMarkApplied) {
        UUID clientId = clientToken.getClientIdClaim();

        return itemsToMarkApplied.getIds().stream()
                .map(id -> getAllowedIP(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(this::markAsAppliedIfRequired)
                .map(allowedOutboundHost -> mapToDTO(allowedOutboundHostRepository.save(allowedOutboundHost)))
                .collect(Collectors.toSet());
    }

    private void rateLimit(UUID clientId) {
        LocalDateTime countAfter = LocalDateTime.now(clock).minusDays(1);
        long numberOfJiraTickets = allowedOutboundHostRepository.findAllByClientIdAndLastUpdatedAfterAndJiraTicketNotNull(clientId, countAfter)
                .stream()
                .map(AllowedOutboundHost::getJiraTicket)
                .distinct()
                .count();
        if (numberOfJiraTickets >= maxOpenTicketsInLast24Hours) {
            throw new TooManyPendingTasksException(clientId);
        }
    }

    public Set<AllowedOutboundHostDTO> markDenied(ClientToken clientToken, AllowedOutboundHostIdListDTO itemsToMarkDenied) {
        UUID clientId = clientToken.getClientIdClaim();

        return itemsToMarkDenied.getIds().stream()
                .map(id -> getAllowedIP(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(this::validateInPendingAddition)
                .map(allowedOutboundHost -> setStatus(allowedOutboundHost, Status.DENIED))
                .map(allowedOutboundHost -> mapToDTO(allowedOutboundHostRepository.save(allowedOutboundHost)))
                .collect(Collectors.toSet());
    }

    private AllowedOutboundHost findOrCreateAllowedIpItem(UUID clientId, String host, JiraAllowedOutboundHostData jiraData) {
        return allowedOutboundHostRepository.findByClientIdAndHost(clientId, host)
                .orElseGet(() -> {
                    var newAllowedIP = new AllowedOutboundHost(UUID.randomUUID(), clientId, host, Status.PENDING_ADDITION, LocalDateTime.now(clock), null);
                    jiraData.withItemToBeAdded(newAllowedIP);
                    return newAllowedIP;
                });
    }

    private AllowedOutboundHost getAllowedIP(UUID clientId, UUID id) {
        return allowedOutboundHostRepository.findByClientIdAndId(clientId, id)
                .orElseGet(() -> {
                    log.warn("could not fetch the AllowedIP with id {} for client {}", id, clientId); //NOSHERIFF
                    return null;
                });
    }

    private AllowedOutboundHost validateInPendingAddition(AllowedOutboundHost allowedOutboundHost) {
        if (allowedOutboundHost.getStatus() != Status.PENDING_ADDITION && allowedOutboundHost.getStatus() != Status.DENIED) {
            throw new AllowedOutboundHostNotInExpectedStateException(Set.of(Status.PENDING_ADDITION, Status.DENIED), allowedOutboundHost.getStatus());
        }
        return allowedOutboundHost;
    }

    private AllowedOutboundHost markAsAddedIfRequired(AllowedOutboundHost allowedOutboundHost, JiraAllowedOutboundHostData jiraData) {
        switch (allowedOutboundHost.getStatus()) {
            case PENDING_REMOVAL -> {
                jiraData.withJiraTicketToBeEdited(allowedOutboundHost.getJiraTicket(), allowedOutboundHost);
                setStatus(allowedOutboundHost, Status.ADDED);
            }
            case REMOVED -> {
                jiraData.withItemToBeAdded(allowedOutboundHost);
                setStatus(allowedOutboundHost, Status.PENDING_ADDITION);
            }
            default -> log.info("Will not add {} since its already in status {}", allowedOutboundHost.getHost(), allowedOutboundHost.getStatus()); //NOSHERIFF
        }
        return allowedOutboundHost;
    }

    private AllowedOutboundHost markAsRemovedIfRequired(JiraAllowedOutboundHostData jiraData, AllowedOutboundHost allowedOutboundHost) {
        switch (allowedOutboundHost.getStatus()) {
            case PENDING_ADDITION -> {
                jiraData.withJiraTicketToBeEdited(allowedOutboundHost.getJiraTicket(), allowedOutboundHost);
                setStatus(allowedOutboundHost, Status.REMOVED);
            }
            case ADDED -> {
                jiraData.withItemToBeRemoved(allowedOutboundHost);
                setStatus(allowedOutboundHost, Status.PENDING_REMOVAL);
            }
            default -> log.info("Will not delete {} since its already in status {}", allowedOutboundHost.getHost(), allowedOutboundHost.getStatus()); //NOSHERIFF
        }
        return allowedOutboundHost;
    }

    private AllowedOutboundHost markAsAppliedIfRequired(AllowedOutboundHost allowedOutboundHost) {
        switch (allowedOutboundHost.getStatus()) {
            case PENDING_ADDITION -> setStatus(allowedOutboundHost, Status.ADDED);
            case PENDING_REMOVAL -> setStatus(allowedOutboundHost, Status.REMOVED);
            default -> log.info("Will not mark {} as applied since its already in status {}", allowedOutboundHost.getHost(), allowedOutboundHost.getStatus()); //NOSHERIFF
        }
        return allowedOutboundHost;
    }

    private AllowedOutboundHost setStatus(AllowedOutboundHost allowedOutboundHost, Status newStatus) {
        allowedOutboundHost.setJiraTicket(null);
        allowedOutboundHost.setStatus(newStatus);
        allowedOutboundHost.setLastUpdated(LocalDateTime.now(clock));
        return allowedOutboundHost;
    }

    private AllowedOutboundHost setJiraTicketIfApplicable(JiraAllowedOutboundHostData jiraAllowedOutboundHostData, String jiraTicket, AllowedOutboundHost allowedOutboundHost) {
        if (jiraAllowedOutboundHostData.contains(allowedOutboundHost)) {
            allowedOutboundHost.setJiraTicket(jiraTicket);
        }
        return allowedOutboundHost;
    }

    private AllowedOutboundHostDTO mapToDTO(AllowedOutboundHost allowedOutboundHost) {
        return new AllowedOutboundHostDTO(allowedOutboundHost.getId(), allowedOutboundHost.getHost(), allowedOutboundHost.getLastUpdated(), allowedOutboundHost.getStatus(), allowedOutboundHost.getJiraTicket());
    }

    public boolean hasAddedWebhookDomainAllowListItems(ClientToken clientToken) {
        return allowedOutboundHostRepository.existsByClientIdAndStatus(clientToken.getClientIdClaim(), Status.ADDED);
    }

    public boolean hasPendingAdditionWebhookDomainAllowListItems(ClientToken clientToken) {
        return allowedOutboundHostRepository.existsByClientIdAndStatus(clientToken.getClientIdClaim(), Status.PENDING_ADDITION);
    }
}
