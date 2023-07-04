package com.yolt.clients.client.ipallowlist.dto;

import com.yolt.clients.jira.Status;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class AllowedIPDTO {
    UUID id;
    String cidr;
    LocalDateTime lastUpdated;
    Status status;
    String jiraTicket;
}
