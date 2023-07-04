package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.dto.*;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLAlreadyExistsException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLNotFoundException;
import com.yolt.clients.client.redirecturls.jira.RedirectURLChangelogJiraData;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogEntry;
import com.yolt.clients.client.redirecturls.repository.RedirectURLChangelogRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.jira.JiraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectURLChangelogService {

    private final RedirectURLChangelogRepository changelogRepository;
    private final RedirectURLRepository urlRepository;
    private final JiraService jiraService;
    private final Clock clock;

    public List<RedirectURLChangelogDTO> findAll(final UUID clientId) {
        return changelogRepository
                .findFirst20ByClientIdOrderByRequestDateDesc(clientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public RedirectURLChangelogDTO createAddRequest(final ClientToken clientToken, final NewAddRequestDTO payload) {
        final var clientId = clientToken.getClientIdClaim();

        var url = Utils.lowercaseURL(payload.getNewRedirectURL());
        if (urlRepository.existsByClientIdAndRedirectURL(clientId, url)) {
            throw new RedirectURLAlreadyExistsException(clientId, url);
        }

        final var changelogEntry = RedirectURLChangelogEntry.createEntry(
                clientToken.getClientIdClaim(), LocalDateTime.now(clock), url, payload.getComment());

        final var jiraData = new RedirectURLChangelogJiraData();
        jiraData.withItemToBeAdded(changelogEntry);

        return setJiraTicketAndSave(clientToken, changelogEntry, jiraData);
    }

    public RedirectURLChangelogDTO createUpdateRequest(final ClientToken clientToken, final NewUpdateRequestDTO payload) {
        final var clientId = clientToken.getClientIdClaim();

        final RedirectURL redirectURL = findRedirectURL(clientId, payload.getRedirectURLId());

        var url = Utils.lowercaseURL(payload.getNewRedirectURL());
        if (redirectURL.getRedirectURL().equals(url)) {
            throw new RedirectURLAlreadyExistsException(clientId, url);
        }

        final var changelogEntry = RedirectURLChangelogEntry.updateEntry(
                clientId, LocalDateTime.now(clock), redirectURL.getRedirectURLId(), redirectURL.getRedirectURL(),
                url, payload.getComment());

        final var jiraData = new RedirectURLChangelogJiraData();
        jiraData.withItemToBeEdited(changelogEntry);

        return setJiraTicketAndSave(clientToken, changelogEntry, jiraData);
    }

    public RedirectURLChangelogDTO createDeleteRequest(final ClientToken clientToken, final NewDeleteRequestDTO payload) {
        final var clientId = clientToken.getClientIdClaim();

        final RedirectURL redirectURL = findRedirectURL(clientId, payload.getRedirectURLId());

        final var changelogEntry = RedirectURLChangelogEntry.deleteEntry(
                clientId, LocalDateTime.now(clock), redirectURL.getRedirectURLId(), redirectURL.getRedirectURL(), payload.getComment());

        final var jiraData = new RedirectURLChangelogJiraData();
        jiraData.withItemToBeRemoved(changelogEntry);

        return setJiraTicketAndSave(clientToken, changelogEntry, jiraData);
    }

    public List<RedirectURLChangelogDTO> markApplied(final UUID clientId, final RedirectURLChangeRequestListDTO payload) {
        return payload.getIds().stream()
                .map(id -> getRedirectURLChangelog(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(item -> setNewStatusIfRequired(item, ChangelogStatus.PROCESSED))
                .map(item -> mapToDTO(changelogRepository.save(item)))
                .collect(Collectors.toList());
    }

    public List<RedirectURLChangelogDTO> markDenied(final UUID clientId, final RedirectURLChangeRequestListDTO payload) {
        return payload.getIds().stream()
                .map(id -> getRedirectURLChangelog(clientId, id))
                .filter(Predicate.not(Objects::isNull))
                .map(item -> setNewStatusIfRequired(item, ChangelogStatus.DENIED))
                .map(item -> mapToDTO(changelogRepository.save(item)))
                .collect(Collectors.toList());
    }


    private RedirectURLChangelogEntry setNewStatusIfRequired(RedirectURLChangelogEntry changelogEntry, ChangelogStatus status) {
        if (changelogEntry.getStatus() == ChangelogStatus.PENDING) {
            changelogEntry.setStatus(status);
        } else {
            log.info("Will not changed status for {} since its already in status {}, only items in PENDING can be modified", changelogEntry.getId(), changelogEntry.getStatus()); //NOSHERIFF
        }
        return changelogEntry;
    }

    private RedirectURLChangelogEntry getRedirectURLChangelog(UUID clientId, UUID id) {
        return changelogRepository.findByClientIdAndId(clientId, id)
                .orElseGet(() -> {
                    log.warn("could not fetch the RedirectURLChangelogEntry with id {} for client {}", id, clientId); //NOSHERIFF
                    return null;
                });
    }

    private RedirectURL findRedirectURL(UUID clientId, UUID redirectURLId) {
        return urlRepository
                .findByClientIdAndRedirectURLId(clientId, redirectURLId)
                .orElseThrow(() -> new RedirectURLNotFoundException(clientId, redirectURLId));
    }

    private RedirectURLChangelogDTO setJiraTicketAndSave(ClientToken clientToken, RedirectURLChangelogEntry changelogEntry, RedirectURLChangelogJiraData jiraData) {
        final var jiraTicket = jiraService.createIssue(clientToken, jiraData);
        changelogEntry.setJiraTicket(jiraTicket);

        final var saved = changelogRepository.save(changelogEntry);
        return mapToDTO(saved);
    }

    private RedirectURLChangelogDTO mapToDTO(final RedirectURLChangelogEntry redirectURLChangelogEntry) {
        return new RedirectURLChangelogDTO(
                redirectURLChangelogEntry.getId(),
                redirectURLChangelogEntry.getRequestDate(),
                redirectURLChangelogEntry.getAction(),
                redirectURLChangelogEntry.getRedirectURLId(),
                redirectURLChangelogEntry.getRedirectURL(),
                redirectURLChangelogEntry.getNewRedirectURL(),
                redirectURLChangelogEntry.getStatus(),
                redirectURLChangelogEntry.getJiraTicket()
        );
    }

    public void delete(ClientToken clientToken) {
        var clientId = clientToken.getClientIdClaim();
        var jiraData = new RedirectURLChangelogJiraData();
        var changelogItems = changelogRepository.findAllByClientId(clientId);

        changelogItems.stream()
                .filter(item -> item.getStatus() == ChangelogStatus.PENDING)
                .filter(item -> StringUtils.hasText(item.getJiraTicket()))
                .forEach(item -> jiraData.withJiraTicketToBeEdited(item.getJiraTicket(), item));

        jiraService.updateIssue(jiraData);

        changelogRepository.deleteAll(changelogItems);
    }
}
