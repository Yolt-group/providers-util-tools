package com.yolt.clients.client.redirecturls.dto;

import com.yolt.clients.client.redirecturls.Action;
import com.yolt.clients.client.redirecturls.ChangelogStatus;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class RedirectURLChangelogDTO {
    UUID id;
    LocalDateTime requestDate;
    Action action;
    UUID redirectURLId;
    String redirectURL;
    String newRedirectURL;
    ChangelogStatus status;
    String jiraTicket;
}
