package com.yolt.clients.client.redirecturls.repository;

import com.yolt.clients.client.redirecturls.Action;
import com.yolt.clients.client.redirecturls.ChangelogStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "redirect_url_changelog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedirectURLChangelogEntry {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(name = "redirect_url_id")
    private UUID redirectURLId;

    @Column(name = "redirect_url")
    private String redirectURL;

    @Column(name = "new_redirect_url")
    private String newRedirectURL;

    @Column(name = "comment")
    private String comment;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ChangelogStatus status;

    @Column(name = "jira_ticket")
    private String jiraTicket;

    public static RedirectURLChangelogEntry createEntry(UUID clientId, LocalDateTime requestDate, String newRedirectURL, String comment) {
        return new RedirectURLChangelogEntry(
                UUID.randomUUID(), clientId, requestDate, Action.CREATE,
                UUID.randomUUID(), null, newRedirectURL, comment,
                ChangelogStatus.PENDING, null);
    }

    public static RedirectURLChangelogEntry updateEntry(UUID clientId, LocalDateTime requestDate, UUID redirectURLId, String redirectURL, String newRedirectURL, String comment) {
        return new RedirectURLChangelogEntry(
                UUID.randomUUID(), clientId, requestDate, Action.UPDATE,
                redirectURLId, redirectURL, newRedirectURL, comment,
                ChangelogStatus.PENDING, null);
    }

    public static RedirectURLChangelogEntry deleteEntry(UUID clientId, LocalDateTime requestDate, UUID redirectURLId, String redirectURL, String comment) {
        return new RedirectURLChangelogEntry(
                UUID.randomUUID(), clientId, requestDate, Action.DELETE,
                redirectURLId, redirectURL, null, comment,
                ChangelogStatus.PENDING, null);
    }
}
