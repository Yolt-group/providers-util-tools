package com.yolt.clients.jira.dto;

import lombok.Value;

@Value
public class IssueResponseDTO {
    String id;
    String key;
    String self;
}
