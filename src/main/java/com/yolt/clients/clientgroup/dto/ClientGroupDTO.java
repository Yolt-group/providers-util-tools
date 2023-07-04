package com.yolt.clients.clientgroup.dto;

import lombok.Value;

import java.util.UUID;

@Value
public class ClientGroupDTO {
    UUID clientGroupId;
    String name;
}
