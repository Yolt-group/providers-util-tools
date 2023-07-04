package com.yolt.clients.client.outboundallowlist.dto;

import com.yolt.clients.jira.Status;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class AllowedOutboundHostDTO {
    UUID id;
    String host;
    LocalDateTime lastUpdated;
    Status status;
    String jiraTicket;
}
