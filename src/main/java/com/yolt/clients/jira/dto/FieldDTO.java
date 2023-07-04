package com.yolt.clients.jira.dto;

import lombok.Value;

import java.util.Set;

@Value
public class FieldDTO {
    ProjectDTO project;
    String summary;
    String description;
    IssueTypeDTO issuetype;
    // organizations custom field;
    Set<Long> customfield_10002;
    // request type custom field;
    String customfield_10010;
}
