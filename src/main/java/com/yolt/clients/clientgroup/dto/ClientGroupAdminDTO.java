package com.yolt.clients.clientgroup.dto;

import lombok.Value;

import java.util.UUID;

@Value
public class ClientGroupAdminDTO {

    String name;
    String email;
    UUID portalUserId;
}
