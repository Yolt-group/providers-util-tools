package com.yolt.clients.client.mtlsdn.dto;

import com.yolt.clients.jira.Status;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class DistinguishedNameDTO {
    UUID id;
    UUID clientId;
    String subjectDN;
    String issuerDN;
    String certificateChain;
    Status status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String jiraTicket;
}
